package no.nav.bidrag.reisekostnad.tjeneste;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.reisekostnad.api.dto.ut.BrukerinformasjonDto;
import no.nav.bidrag.reisekostnad.database.datamodell.Deaktivator;
import no.nav.bidrag.reisekostnad.database.datamodell.Oppgavebestilling;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.Persondatafeil;
import no.nav.bidrag.reisekostnad.feilhåndtering.Valideringsfeil;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.BidragPersonkonsument;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.Diskresjonskode;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.HentFamilieRespons;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.MotpartBarnRelasjon;
import no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Brukernotifikasjonkonsument;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Krypteringsverktøy;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Mapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReisekostnadApiTjeneste {

  private Arkiveringstjeneste arkiveringstjeneste;
  private final BidragPersonkonsument bidragPersonkonsument;
  private final Brukernotifikasjonkonsument brukernotifikasjonkonsument;
  private final Databasetjeneste databasetjeneste;
  private final MeterRegistry meterRegistry;
  private final Mapper mapper;

  @Autowired
  public ReisekostnadApiTjeneste(
      Arkiveringstjeneste arkiveringstjeneste,
      BidragPersonkonsument bidragPersonkonsument,
      Brukernotifikasjonkonsument brukernotifikasjonkonsument,
      Databasetjeneste databasetjeneste,
      MeterRegistry meterRegistry, Mapper mapper) {
    this.arkiveringstjeneste = arkiveringstjeneste;
    this.bidragPersonkonsument = bidragPersonkonsument;
    this.brukernotifikasjonkonsument = brukernotifikasjonkonsument;
    this.databasetjeneste = databasetjeneste;
    this.meterRegistry = meterRegistry;
    this.mapper = mapper;
  }

  public HttpResponse<BrukerinformasjonDto> henteBrukerinformasjon(String fnrPaaloggetBruker) {
    var familierespons = bidragPersonkonsument.hentFamilie(fnrPaaloggetBruker);
    try {
      validerePåloggetPerson(familierespons);
    } catch (Valideringsfeil valideringsfeil) {
      log.warn("Pålogget person har diskresjon. Kan ikke bruke løsningen");
    }
    return HttpResponse.Companion.from(HttpStatus.OK, mapper.tilDto(familierespons.get()));
  }

  public HttpResponse<Void> oppretteForespørselOmFordelingAvReisekostnader(String personidentHovedpart, Set<String> krypterteIdenterBarn) {
    var familierespons = bidragPersonkonsument.hentFamilie(personidentHovedpart);
    validerePåloggetPerson(familierespons);
    var personidenterBarn = krypterteIdenterBarn.stream().map(k -> dekryptere(k)).collect(Collectors.toSet());

    // Kaster Valideringsfeil dersom hovedpart ikke er registrert med familierelasjoner eller mangler relasjon til minst ett av de oppgitte barna.
    validereRelasjonTilBarn(personidenterBarn, familierespons);

    familierespons.get().getPersonensMotpartBarnRelasjon().stream().filter(Objects::nonNull)
        .filter(mbr -> mbr.getMotpart() != null && !StringUtils.isEmpty(mbr.getMotpart().getIdent()))
        .forEach(m -> lagreForespørsel(personidentHovedpart, m.getMotpart().getIdent(), henteUtBarnaSomTilhørerMotpart(m, personidenterBarn)));

    return HttpResponse.Companion.from(HttpStatus.CREATED);
  }

  public HttpResponse<Void> oppdatereForespørselMedSamtykke(int idForespørsel, String personidentMotpart) {
    // Kaster Valideringsfeil dersom forespørsel ikke finnes eller oppdatering av samtykke  feiler
    databasetjeneste.giSamtykke(idForespørsel, personidentMotpart);
    sletteSamtykkeoppgave(idForespørsel, personidentMotpart);
    arkiveringstjeneste.arkivereForespørsel(idForespørsel);
    return HttpResponse.Companion.from(HttpStatus.OK, null);
  }

  public HttpResponse<Void> trekkeForespørsel(int idForespørsel, String personident) {

    // Kaster Valideringsfeil dersom forespørsel ikke finnes eller deaktivering feiler
    var deaktivertForespørsel = databasetjeneste.deaktivereForespørsel(idForespørsel, personident);

    if (Deaktivator.MOTPART.equals(deaktivertForespørsel.getDeaktivertAv())) {
      brukernotifikasjonkonsument.varsleOmNeiTilSamtykke(deaktivertForespørsel.getHovedpart().getPersonident(),
          deaktivertForespørsel.getMotpart().getPersonident());
    } else if (Deaktivator.HOVEDPART.equals(deaktivertForespørsel.getDeaktivertAv())) {
      brukernotifikasjonkonsument.varsleOmTrukketForespørsel(deaktivertForespørsel.getHovedpart().getPersonident(),
          deaktivertForespørsel.getMotpart().getPersonident());
    }

    // Motparts samtykkeoppgave skal slettes uavhengig av om det er hovedpart eller motpart som trekker forespørselen
    sletteSamtykkeoppgave(deaktivertForespørsel.getId(), deaktivertForespørsel.getMotpart().getPersonident());

    countReisekostnadTrukket();
    return HttpResponse.Companion.from(HttpStatus.OK, null);
  }

  private void sletteSamtykkeoppgave(int idForespørsel, String personidentMotpart) {
    var aktiveOppgaver = databasetjeneste.henteAktiveOppgaverMotpart(idForespørsel, personidentMotpart);
    log.info("Fant {} aktive brukernotifikasjonsoppgaver knyttet til motpart i forespørsel med id {}", aktiveOppgaver.size(), idForespørsel);
    for (Oppgavebestilling oppgave : aktiveOppgaver) {
      brukernotifikasjonkonsument.ferdigstilleSamtykkeoppgave(oppgave.getEventId(), personidentMotpart);
      log.info("Slettet oppgave med eventId {} knyttet til forespørsel {}", oppgave.getEventId(), idForespørsel);
    }
  }

  private Set<String> henteUtBarnaSomTilhørerMotpart(MotpartBarnRelasjon mBRelasjon, Set<String> valgteBarn) {
    return mBRelasjon.getFellesBarn().stream().filter(Objects::nonNull).filter(barn -> valgteBarn.contains(barn.getIdent())).map(b -> b.getIdent())
        .collect(Collectors.toSet());
  }

  private void validerePåloggetPerson(Optional<HentFamilieRespons> familieRespons) {
    if (!familieRespons.isPresent() || familieRespons.get().getPerson() == null) {
      throw new Persondatafeil(Feilkode.PDL_PERSON_IKKE_FUNNET, HttpStatus.NOT_FOUND);
    } else if (familieRespons.get().getPerson().getDoedsdato() != null && LocalDate.now().isAfter(familieRespons.get().getPerson().getDoedsdato())) {
      throw new Persondatafeil(Feilkode.PDL_PERSON_DØD, HttpStatus.FORBIDDEN);
    }

    håndtereDiskresjonForPåloggetPerson(familieRespons.get().getPerson().getDiskresjonskode());
  }

  private void håndtereDiskresjonForPåloggetPerson(String diskresjonskode) {

    if (StringUtils.isEmpty(diskresjonskode)) {
      return;
    }

    for (Diskresjonskode kode : Diskresjonskode.values()) {
      if (kode.toString().equals(diskresjonskode)) {
        throw new Valideringsfeil(Feilkode.VALIDERING_PÅLOGGET_PERSON_DISKRESJON);
      }
    }
  }

  private void lagreForespørsel(String personidentHovedpart, String personidentMotpart, Set<String> barn) {

    var barnOver15År = filtrereUtBarnOver15År(barn);
    var barnUnder15År = barn.stream().filter(b -> !erPersonOver15År(b)).collect(Collectors.toSet());

    if (barnOver15År.size() > 0) {
      lagreNyForespørsel(personidentHovedpart, personidentMotpart, barnOver15År, false);
    }

    if (barnUnder15År.size() > 0) {
      var idForespørsel = lagreNyForespørsel(personidentHovedpart, personidentMotpart, barnUnder15År, true);
      if (idForespørsel > 0) {
        brukernotifikasjonkonsument.oppretteOppgaveTilMotpartOmSamtykke(idForespørsel, personidentMotpart);
        brukernotifikasjonkonsument.varsleOmNyForespørselSomVenterPåSamtykke(personidentHovedpart);
      }
    }
  }

  private int lagreNyForespørsel(String personidentHovedpart, String personidentMotpart, Set<String> barn, Boolean kreverSamtykke) {
    var forespørsel = databasetjeneste.lagreNyForespørsel(personidentHovedpart, personidentMotpart, barn, kreverSamtykke);
    if (!kreverSamtykke) {
      arkiveringstjeneste.arkivereForespørsel(forespørsel.getId());
    }
    return forespørsel.getId();
  }

  private void validereRelasjonTilBarn(Set<String> personidenterBarn, Optional<HentFamilieRespons> familieRespons) {
    if (familieRespons.isEmpty() || familieRespons.get().getPersonensMotpartBarnRelasjon().size() < 1) {
      throw new Valideringsfeil(Feilkode.VALIDERING_NY_FOREPØRSEL_INGEN_FAMILIERELASJONER);
    }

    var allePersonensBarnMedDeltForeldreansvar = familieRespons.get().getPersonensMotpartBarnRelasjon().stream()
        .flatMap(m -> m.getFellesBarn().stream())
        .map(f -> f.getIdent())
        .collect(Collectors.toSet());

    var alleBarnaIForespørselenHarForelderrelasjonTilPåloggetPerson = erSubsett(personidenterBarn, allePersonensBarnMedDeltForeldreansvar);

    if (!alleBarnaIForespørselenHarForelderrelasjonTilPåloggetPerson) {
      throw new Valideringsfeil(Feilkode.VALIDERING_NY_FOREPØRSEL_MANGLER_RELASJON_BARN);
    }
  }

  private boolean erSubsett(Set<String> subsett, Set<String> supersett) {

    if (subsett == null || subsett.size() < 1 || supersett == null || supersett.size() < 1) {
      return false;
    }

    for (String element : subsett) {
      if (!supersett.contains(element)) {
        return false;
      }
    }
    return true;
  }

  private boolean erPersonOver15År(String personident) {
    var respons = bidragPersonkonsument.hentPersoninfo(personident);
    var fødselsdato = respons.getFoedselsdato();
    return fødselsdato != null && fødselsdato.isBefore(LocalDate.now().minusYears(15));
  }

  private Set<String> filtrereUtBarnOver15År(Set<String> alleBarn) {
    return alleBarn.stream().filter(this::erPersonOver15År).collect(Collectors.toSet());
  }

  private String dekryptere(String kryptertPersonident) {
    return Krypteringsverktøy.dekryptere(kryptertPersonident);
  }

  private void countReisekostnadTrukket(){
    Counter.builder("reisekostnad_trukket")
        .description("Teller antall reisekostnader som er trukket")
        .register(meterRegistry).increment();
  }
}
