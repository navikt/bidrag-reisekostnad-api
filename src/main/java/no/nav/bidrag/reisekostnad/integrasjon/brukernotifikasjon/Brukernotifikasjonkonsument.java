package no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon;

import static no.nav.bidrag.reisekostnad.konfigurasjon.Brukernotifikasjonskonfig.NAMESPACE_BIDRAG;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.database.datamodell.Barn;
import no.nav.bidrag.reisekostnad.database.datamodell.Forelder;
import no.nav.bidrag.reisekostnad.feilhåndtering.InternFeil;
import no.nav.bidrag.reisekostnad.konfigurasjon.Egenskaper;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;

@Slf4j
public class Brukernotifikasjonkonsument {

  private static final String MELDING_OM_SIGNERT_FARSKAPSERKLAERING = "Du har en signert farskapserklæring er tilgjengelig for nedlasting i en begrenset tidsperiode fra farskapsportalen:";
  private static final String MELDING_OM_VENTENDE_FARSKAPSERKLAERING = "Du har mottatt en farskapserklæring som venter på din signatur.";
  private static final String MELDING_TIL_HOVEDPART_OM_NEI_TIL_SAMTYKKE = "Fars signering ble avbrutt, aktuell farskapserklæring måtte derfor slettes. Mor kan opprette ny hvis ønskelig. Trykk her for å opprette ny farskapserklæring.";
  private static final String MELDING_TIL_MOTPART_OM_AVSLÅTT_SAMTYKKE = "Fars signering ble avbrutt, aktuell farskapserklæring måtte derfor slettes. Mor kan opprette ny hvis ønskelig.";
  private static final String MELDING_OM_MANGLENDE_SIGNERING = "Aksjon kreves: Farskapserklæring opprettet den %s for barn med %s er ikke ferdigstilt. Våre systemer mangler informasjon om at far har signert. Far må logge inn på Farskapsportal og forsøke å signere eller oppdatere status på ny. Ta kontakt med NAV ved problemer.";
  private static final String MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE = "Far har ikke signert farskapserklæringen innen fristen. Farskapserklæringen er derfor slettet. Mor kan opprette ny hvis ønskelig. Trykk her for å opprette ny farskapserklæring.";

  private final Beskjedprodusent beskjedprodusent;
  private final Ferdigprodusent ferdigprodusent;
  private final Oppgaveprodusent oppgaveprodusent;
  private final Egenskaper egenskaper;

  public Brukernotifikasjonkonsument(Beskjedprodusent beskjedprodusent, Ferdigprodusent ferdigprodusent, Oppgaveprodusent oppgaveprodusent,
      Egenskaper egenskaper) {
    this.beskjedprodusent = beskjedprodusent;
    this.ferdigprodusent = ferdigprodusent;
    this.oppgaveprodusent = oppgaveprodusent;
    this.egenskaper = egenskaper;
  }

  public void varsleForeldreOmManglendeSamtykke(Forelder hovedpart, Forelder far, Set<Barn> barn, LocalDate opprettetDato) {
    log.info("Informerer foreldre (hovedpart: {}, motpart: {}) om mangldende samtykke.", hovedpart.getId(), far.getId());
    beskjedprodusent.oppretteBeskjedTilBruker(hovedpart,
        String.format(MELDING_OM_MANGLENDE_SIGNERING, opprettetDato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            ""), true,
        oppretteNokkel(hovedpart.getPersonident()));
    beskjedprodusent.oppretteBeskjedTilBruker(far,
        String.format(MELDING_OM_MANGLENDE_SIGNERING, opprettetDato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), ""), true,
        oppretteNokkel(far.getPersonident()));
  }

  public void varsleMorOmUtgaattOppgaveForSignering(Forelder mor) {
    log.info("Sender varsel til mor om utgått signeringsoppgave");
    var noekkel = oppretteNokkel(mor.getPersonident());
    beskjedprodusent.oppretteBeskjedTilBruker(mor, MELDING_OM_IKKE_UTFOERT_SIGNERINGSOPPGAVE, true, noekkel);
    log.info("Ekstern melding med eventId: {}, ble sendt til mor", noekkel.getEventId());
  }

  public void varsleOmNeiTilSamtykke(Forelder hovedpart, Forelder motpart) {
    log.info("Varsler brukere om avbrutt signering");
    beskjedprodusent.oppretteBeskjedTilBruker(hovedpart, MELDING_TIL_HOVEDPART_OM_NEI_TIL_SAMTYKKE, true, oppretteNokkel(hovedpart.getPersonident()));
    beskjedprodusent.oppretteBeskjedTilBruker(motpart, MELDING_TIL_MOTPART_OM_AVSLÅTT_SAMTYKKE, true, oppretteNokkel(motpart.getPersonident()));
  }

  public void oppretteOppgaveTilMotpartOmSamtykke(int udForespørsel, Forelder motpart) {
    try {
      oppgaveprodusent
          .oppretteOppgaveOmSamtykke(udForespørsel, motpart,
              MELDING_OM_VENTENDE_FARSKAPSERKLAERING, true);
    } catch (InternFeil internFeil) {
      log.error("En feil inntraff ved opprettelse av oppgave til far for farskapserklæring med id {}", udForespørsel);
    }
  }

  public void sletteSamtykkeoppgave(String eventId, Forelder motpart) {
    log.info("Sletter samtykkeoppgave med eventId {}", eventId);
    try {
      ferdigprodusent.ferdigstilleFarsSigneringsoppgave(motpart, oppretteNokkel(eventId, motpart.getPersonident()));
    } catch (InternFeil internFeilException) {
      log.error("En feil oppstod ved sending av ferdigmelding for oppgave med eventId {}.", eventId);
    }
  }

  private NokkelInput oppretteNokkel(String foedselsnummer) {
    var unikEventid = UUID.randomUUID().toString();
    return oppretteNokkel(unikEventid, foedselsnummer);
  }

  private NokkelInput oppretteNokkel(String eventId, String foedselsnummer) {
    return new NokkelInputBuilder()
        .withEventId(eventId)
        .withFodselsnummer(foedselsnummer)
        .withGrupperingsId(egenskaper.getBrukernotifikasjon().getGrupperingsidReisekostnad())
        .withNamespace(NAMESPACE_BIDRAG)
        .withAppnavn(egenskaper.getAppnavnReisekostnad())
        .build();
  }

}
