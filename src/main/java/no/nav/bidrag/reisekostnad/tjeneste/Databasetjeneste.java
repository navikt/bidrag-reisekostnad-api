package no.nav.bidrag.reisekostnad.tjeneste;

import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.SIKKER_LOGG;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.database.dao.BarnDao;
import no.nav.bidrag.reisekostnad.database.dao.ForelderDao;
import no.nav.bidrag.reisekostnad.database.dao.ForespørselDao;
import no.nav.bidrag.reisekostnad.database.dao.OppgavebestillingDao;
import no.nav.bidrag.reisekostnad.database.datamodell.Deaktivator;
import no.nav.bidrag.reisekostnad.database.datamodell.Forelder;
import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel;
import no.nav.bidrag.reisekostnad.database.datamodell.Oppgavebestilling;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.InternFeil;
import no.nav.bidrag.reisekostnad.feilhåndtering.Valideringsfeil;
import no.nav.bidrag.reisekostnad.model.ForespørselUtvidelserKt;
import no.nav.bidrag.reisekostnad.model.KonstanterKt;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Mapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Databasetjeneste {

  private BarnDao barnDao;
  private ForelderDao forelderDao;
  private ForespørselDao forespørselDao;
  private OppgavebestillingDao oppgavebestillingDao;
  private Mapper mapper;

  @Autowired
  public Databasetjeneste(BarnDao barnDao, ForelderDao forelderDao, ForespørselDao forespørselDao, OppgavebestillingDao oppgavebestillingDao,
      Mapper mapper) {
    this.barnDao = barnDao;
    this.forelderDao = forelderDao;
    this.forespørselDao = forespørselDao;
    this.oppgavebestillingDao = oppgavebestillingDao;
    this.mapper = mapper;
  }

  @Transactional(TxType.REQUIRES_NEW)
  public Forespørsel oppdaterForespørselTilÅIkkeKreveSamtykke(int forespørselId) {
    var originalForespørsel = forespørselDao.henteAktivForespørsel(forespørselId).get();
    if (ForespørselUtvidelserKt.getAlleBarnHarFylt15år(originalForespørsel)) {
      originalForespørsel.setKreverSamtykke(false);
      log.info("Forespørsel med id {} ble endret til å ikke kreve samtykke", forespørselId);
    }

    return originalForespørsel;
  }

  @Transactional(TxType.REQUIRES_NEW)
  public Forespørsel overførBarnSomHarFylt15årTilNyForespørsel(int forespørselId) {
    var originalForespørsel = forespørselDao.henteAktivForespørsel(forespørselId).get();

    if (ForespørselUtvidelserKt.getAlleBarnHarFylt15år(originalForespørsel)) {
      log.warn(
          "Forespørsel {} som inneholder barn fylt 15 år ble forsøket splittet til ny forespørsel. Splitting er ikke mulig fordi alle barn i forespørselen har fylt 15 år. Gjør ingen endring",
          forespørselId);
      return originalForespørsel;
    }

    var barnSomHarFylt15år = ForespørselUtvidelserKt.getIdenterBarnSomHarFylt15år(originalForespørsel);

    ForespørselUtvidelserKt.fjernBarnSomHarFylt15år(originalForespørsel);

    var hovedpartIdent = originalForespørsel.getHovedpart().getPersonident();
    var motpartIdent = originalForespørsel.getMotpart().getPersonident();
    var nyForespørselId = lagreNyForespørsel(hovedpartIdent, motpartIdent, barnSomHarFylt15år, false);
    log.info("Barn som har fylt 15 år i forespørsel med id {} ble overført til ny forespørsel {}", forespørselId, nyForespørselId);
    return nyForespørselId;
  }

  @Transactional
  public Forespørsel lagreNyForespørsel(String hovedpart, String motpart, Set<String> identerBarn, boolean kreverSamtykke) {

    for (String ident : identerBarn) {
      var barn = barnDao.henteBarnTilknyttetAktivForespørsel(ident);
      if (barn.isPresent()) {
        log.warn("Validering feilet. Det finnes allerede en aktiv forespørsel for et av de oppgitte barna.");
        SIKKER_LOGG.warn("Validering feilet. Barn med ident {} er allerede tilknyttet en aktiv forespørsel", ident);
        throw new Valideringsfeil(Feilkode.VALIDERING_NY_FOREPØRSEL_BARN_I_AKTIV_FORESPØRSEL);
      }
    }

    var ekisterendeHovedpart = forelderDao.finnMedPersonident(hovedpart);
    var eksisterendeMotpart = forelderDao.finnMedPersonident(motpart);

    var samtykkefrist = kreverSamtykke ? LocalDate.now().plusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING) : null;

    var nyForespørsel = Forespørsel.builder().opprettet(LocalDateTime.now())
        .hovedpart(ekisterendeHovedpart.orElseGet(() -> Forelder.builder().personident(hovedpart).build()))
        .motpart(eksisterendeMotpart.orElseGet(() -> Forelder.builder().personident(motpart).build())).barn(mapper.tilEntitet(identerBarn))
        .kreverSamtykke(kreverSamtykke).samtykkefrist(samtykkefrist).build();

    return forespørselDao.save(nyForespørsel);
  }

  @Transactional
  public void giSamtykke(int idForespørsel, String personident) {
    log.info("Samtykker til fordeling av reisekostnader i forespørsel med id {}", idForespørsel);
    var forespørsel = forespørselDao.henteAktivForespørsel(idForespørsel);
    if (forespørsel.isPresent() && personident.equals(forespørsel.get().getMotpart().getPersonident())) {
      var nå = LocalDateTime.now();
      SIKKER_LOGG.info("Motpart (ident: {}) samtykker til fordeling av reisekostnader relatert til forespørsel id {}", personident, idForespørsel);
      forespørsel.get().setSamtykket(nå);
    } else {
      log.warn("Fant ikke forespørsel med id {}. Får ikke gitt samtykke.", idForespørsel);
      throw new Valideringsfeil(Feilkode.VALIDERING_SAMTYKKE_MOTPART);
    }
  }

  @Transactional
  public Forespørsel deaktivereForespørsel(int idForespørsel, String personident) {
    log.info("Deaktiverer forespørsel med id {}", idForespørsel);
    var forespørsel = forespørselDao.henteAktivForespørsel(idForespørsel);
    var deaktiveresAvSystem = personident == null;
    if (forespørsel.isPresent() && (deaktiveresAvSystem || personErPartIForespørsel(personident, forespørsel.get()))) {
      var nå = LocalDateTime.now();

      var deaktivertAv = personident == null
          ? Deaktivator.SYSTEM
          : erHovedpart(personident, forespørsel.get())
              ? Deaktivator.HOVEDPART : Deaktivator.MOTPART;

      SIKKER_LOGG.info("Forelder med ident: {} deaktiverer forespørsel med id {}", personident, idForespørsel);
      forespørsel.get().setDeaktivert(nå);
      forespørsel.get().setDeaktivertAv(deaktivertAv);
      return forespørsel.get();
    } else if (forespørsel.isEmpty()) {
      throw new Valideringsfeil(Feilkode.VALIDERING_DEAKTIVERE_FEIL_STATUS);
    } else {
      throw new Valideringsfeil(Feilkode.VALIDERING_DEAKTIVERE_PERSON_IKKE_PART_I_FORESPØRSEL);
    }
  }

  public Forespørsel henteAktivForespørsel(int idForespørsel) {
    var forespørsel = forespørselDao.henteAktivForespørsel(idForespørsel);
    if (forespørsel.isPresent() && forespørsel.get().getDeaktivert() == null) {
      return forespørsel.get();
    } else {
      throw new InternFeil(Feilkode.RESSURS_IKKE_FUNNET);
    }
  }

  @Transactional
  public int anonymisereBarnUtenTilknytningTilAktiveForespørsler() {

    var barnSomSkalAnonymiseres = barnDao.henteBarnUtenTilknytningTilAktivForespørsel(
        LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING).atStartOfDay());

    // Dobbelsjekker at uttrekket kun inneholder barn som skal anonymiseres
    barnSomSkalAnonymiseres = barnSomSkalAnonymiseres.stream()
        .filter(Objects::nonNull)
        .filter(b -> b.getPersonident() != null)
        .filter(b -> b.getForespørsel().getDeaktivert() != null)
        .filter(b -> b.getForespørsel().getDeaktivert()
            .isBefore(LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING).atStartOfDay()))
        .collect(Collectors.toSet());

    log.info("Fant {} barn uten tilknytning til aktive forespørsler. Anonymiserer disse", barnSomSkalAnonymiseres.size());
    barnSomSkalAnonymiseres.forEach(b -> b.setPersonident(null));
    return barnSomSkalAnonymiseres.size();
  }

  @Transactional
  public int sletteForeldreUtenTilknytningTilAktiveForespørsler() {
    log.info("Sletter foreldre uten tilknytning til aktive forespørsler");
    var foreldreSomSkalSlettes = forelderDao.henteForeldreUtenTilknytningTilAktiveForespørsler(
        LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING));

    log.info("Fant {} foreldre uten tilknyting til aktive forespørsler. Anonymiserer disse.", foreldreSomSkalSlettes.size());
    forelderDao.deleteAll(foreldreSomSkalSlettes);
    return foreldreSomSkalSlettes.size();
  }

  public Oppgavebestilling lagreNyOppgavebestilling(int idFarskapserklaering, String eventId) {
    var forespørsel = henteForespørselForId(idFarskapserklaering);

    var oppgavebestilling = Oppgavebestilling.builder().forespørsel(forespørsel).forelder(forespørsel.getMotpart()).eventId(eventId)
        .opprettet(LocalDateTime.now()).build();
    return oppgavebestillingDao.save(oppgavebestilling);
  }

  public Forespørsel henteForespørselForId(int idFarskapserklaering) {
    var farskapserklaering = forespørselDao.findById(idFarskapserklaering);
    if (farskapserklaering.isPresent() && farskapserklaering.get().getDeaktivert() == null) {
      return farskapserklaering.get();
    }
    throw new InternFeil(Feilkode.FANT_IKKE_FORESPØRSEL);
  }

  public Set<Integer> henteForespørslerSomErKlareForInnsending() {
    log.info("Ser etter forespørsler som er klare for innsending");
    var aktiveForespørslerMedSamtykke = forespørselDao.henteAktiveOgSamtykkedeForespørslerSomErKlareForInnsending();
    var aktiveForespørslerUtenSamtykke = forespørselDao.henteAktiveForespørslerSomIkkeKreverSamtykkOgErKlareForInnsending();
    log.info("Fant {} aktive forespørsler med samtykke, og {} uten samtykke, som er klare for innsending", aktiveForespørslerMedSamtykke.size(),
        aktiveForespørslerUtenSamtykke.size());

    aktiveForespørslerMedSamtykke.addAll(aktiveForespørslerUtenSamtykke);

    return aktiveForespørslerMedSamtykke;
  }

  public Set<Integer> henteIdTilAktiveForespørsler(LocalDateTime opprettetFør, boolean erJournalført) {
    return erJournalført ? forespørselDao.henteIdTilAktiveJournalførteForespørsler(opprettetFør)
        : forespørselDao.henteIdTilAktiveForespørsler(opprettetFør);
  }

  public List<Forespørsel> hentForespørselSomInneholderBarnSomHarFylt15år() {
    var forespørsler = forespørselDao.henteForespørslerSomKreverSamtykkeOgInneholderBarnFødtSammeDagEllerEtterDato(
        KonstanterKt.getDato15ÅrTilbakeFraIdag());
    log.info("Fant {} aktive forespørsler om inneholder barn som har nylig fylt 15år", forespørsler.size());

    return forespørsler.stream().toList();
  }

  private boolean erHovedpart(String personident, Forespørsel forespørsel) {
    return !StringUtils.isEmpty(personident) && (personident.equals(forespørsel.getHovedpart().getPersonident()));
  }

  private boolean personErPartIForespørsel(String personident, Forespørsel forespørsel) {
    return !StringUtils.isEmpty(personident) && (personident.equals(forespørsel.getHovedpart().getPersonident()) || personident.equals(
        forespørsel.getMotpart().getPersonident()));
  }

  @Transactional
  public void setteOppgaveTilFerdigstilt(String eventId) {
    var aktiveOppgaver = oppgavebestillingDao.henteOppgavebestilling(eventId);

    if (aktiveOppgaver.isPresent()) {
      aktiveOppgaver.get().setFerdigstilt(LocalDateTime.now());
    } else {
      log.warn("Fant ingen oppgavebestilling med eventId {}, ferdigstiltstatus ble ikke satt!", eventId);
    }
  }

  public Set<Oppgavebestilling> henteAktiveOppgaverMotpart(int idForespørsel, String personidentMotpart) {
    return oppgavebestillingDao.henteAktiveOppgaver(idForespørsel, personidentMotpart);
  }

  public Set<Oppgavebestilling> henteOppgaverSomSkalFerdigstilles() {
    return oppgavebestillingDao.henteAktiveOppgaverKnyttetTilDeaktiverteForespørsler();
  }
}
