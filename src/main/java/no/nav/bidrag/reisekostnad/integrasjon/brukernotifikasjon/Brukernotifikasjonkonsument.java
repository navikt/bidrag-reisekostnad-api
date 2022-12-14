package no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon;

import static no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Melding.MELDING_OM_IKKE_UTFOERT_SAMTYKKEOPPGAVE;
import static no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Melding.MELDING_OM_MANGLENDE_SAMTYKKE;
import static no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Melding.MELDING_OM_VENTENDE_FORESPØRSEL;
import static no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Melding.MELDING_TIL_HOVEDPART_OM_AVSLÅTT_SAMTYKKE;
import static no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Melding.MELDING_TIL_MOTPART_OM_AVSLÅTT_SAMTYKKE;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.SIKKER_LOGG;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Brukernotifikasjonskonfig.NAMESPACE_BIDRAG;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.database.datamodell.Forelder;
import no.nav.bidrag.reisekostnad.feilhåndtering.InternFeil;
import no.nav.bidrag.reisekostnad.konfigurasjon.Egenskaper;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;

@Slf4j
public class Brukernotifikasjonkonsument {

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

  public void varsleForeldreOmManglendeSamtykke(String personidentHovedpart, String personidentMotpart, LocalDate opprettetDato) {
    log.info("Informerer foreldre om mangldende samtykke for forespørsel opprettet den {}.",
        opprettetDato.format(DateTimeFormatter.ofPattern("ddMMyyy")));
    SIKKER_LOGG.info("Informerer foreldre (hovedpart: {}, motpart: {}) om mangldende samtykke for forespørsel opprettet den {}", personidentHovedpart,
        personidentMotpart, opprettetDato);

    var dato = opprettetDato.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    var melding = new DynamiskMelding(MELDING_OM_MANGLENDE_SAMTYKKE, List.of(dato));

    beskjedprodusent.oppretteBeskjedTilBruker(personidentHovedpart, melding, true, oppretteNokkel(personidentHovedpart));
    beskjedprodusent.oppretteBeskjedTilBruker(personidentMotpart, melding, true, oppretteNokkel(personidentMotpart));
  }

  public void varsleMorOmUtgaattOppgaveForSignering(String personidentHovedperson) {
    log.info("Sender varsel til mor om utgått signeringsoppgave");
    var noekkel = oppretteNokkel(personidentHovedperson);
    beskjedprodusent.oppretteBeskjedTilBruker(personidentHovedperson, new DynamiskMelding(MELDING_OM_IKKE_UTFOERT_SAMTYKKEOPPGAVE), true, noekkel);
    log.info("Ekstern melding med eventId: {}, ble sendt til mor", noekkel.getEventId());
  }

  public void varsleOmNeiTilSamtykke(String personidentHovedpart, String personidentMotpart) {
    log.info("Varsler brukere om avbrutt signering");
    beskjedprodusent.oppretteBeskjedTilBruker(personidentHovedpart, new DynamiskMelding(MELDING_TIL_HOVEDPART_OM_AVSLÅTT_SAMTYKKE), true,
        oppretteNokkel(personidentHovedpart));
    beskjedprodusent.oppretteBeskjedTilBruker(personidentMotpart, new DynamiskMelding(MELDING_TIL_MOTPART_OM_AVSLÅTT_SAMTYKKE), false,
        oppretteNokkel(personidentMotpart));
  }

  public void oppretteOppgaveTilMotpartOmSamtykke(int idForespørsel, String personidentMotpart) {
    try {
      oppgaveprodusent.oppretteOppgaveOmSamtykke(idForespørsel, personidentMotpart, new DynamiskMelding(MELDING_OM_VENTENDE_FORESPØRSEL), true);
    } catch (InternFeil internFeil) {
      log.error("En feil inntraff ved opprettelse av samtykkeoppgave til motpart i forespørsels med id {}", idForespørsel);
    }
  }

  public void sletteSamtykkeoppgave(String eventId, Forelder motpart) {
    log.info("Sletter samtykkeoppgave med eventId {}", eventId);
    try {
      ferdigprodusent.ferdigstilleFarsSigneringsoppgave(oppretteNokkel(eventId, motpart.getPersonident()));
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
