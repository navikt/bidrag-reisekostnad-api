package no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon;

import static no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Melding.MELDING_OM_AUTOMATISK_INNSENDING;
import static no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Melding.MELDING_OM_VENTENDE_FORESPØRSEL;
import static no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Melding.MELDING_TIL_FORELDRE_OM_UTLØPT_SAMTYKKEFRIST;
import static no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Melding.MELDING_TIL_HOVEDPART_OM_AVSLÅTT_SAMTYKKE;
import static no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Melding.MELDING_TIL_HOVEDPART_OM_FORESPØRSEL_SOM_VENTER_PÅ_SAMTYKKE;
import static no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Melding.MELDING_TIL_MOTPART_OM_AVSLÅTT_SAMTYKKE;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.SIKKER_LOGG;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Brukernotifikasjonskonfig.NAMESPACE_BIDRAG;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
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
    var melding = new DynamiskMelding(MELDING_TIL_FORELDRE_OM_UTLØPT_SAMTYKKEFRIST, List.of(dato));

    beskjedprodusent.oppretteBeskjedTilBruker(personidentHovedpart, melding, true, oppretteNokkel(personidentHovedpart));
    beskjedprodusent.oppretteBeskjedTilBruker(personidentMotpart, melding, true, oppretteNokkel(personidentMotpart));
  }

  public void varsleOmAutomatiskInnsending(String personidentHovedpart, String personidentMotpart, LocalDate fødselsdatoBarn) {
    log.info("Varsler foreldre om automatisk innsending av forespørsel etter at barn med fødselsdato {} fylte 15 år", fødselsdatoBarn);
    SIKKER_LOGG.info(
        "Varsler foreldre (hovedpart: {} og motpart: {}) om automatisk innsending av forespørsel etter at barn med fødselsdato {} fylte 15 år",
        personidentHovedpart, personidentMotpart, fødselsdatoBarn);
    beskjedprodusent.oppretteBeskjedTilBruker(personidentHovedpart, new DynamiskMelding(MELDING_OM_AUTOMATISK_INNSENDING), true,
        oppretteNokkel(personidentHovedpart));
    beskjedprodusent.oppretteBeskjedTilBruker(personidentMotpart, new DynamiskMelding(MELDING_OM_AUTOMATISK_INNSENDING), true,
        oppretteNokkel(personidentMotpart));
  }

  /**
   * Varsler hovedpart om nyopprettet forespørsel som venter på samtykke. Dette gjøres hovedsaklig av hensyn til kontaktsenteret ettersom
   * de med dette får mulighet til å spore påbegynte forespørsler via Modia.
   * @param personidentHovedpart
   */
  public void varsleOmNyForespørselSomVenterPåSamtykke(String personidentHovedpart){
    log.info("Varsler hovedpart om ny forespørsel som venter på samtykke fra den andre forelderen.");
    SIKKER_LOGG.info("Varsler hovedpart med personident {} om ny forespørsel som venter på samtykke fra den andre forelderen.", personidentHovedpart);
    beskjedprodusent.oppretteBeskjedTilBruker(personidentHovedpart, new DynamiskMelding(MELDING_TIL_HOVEDPART_OM_FORESPØRSEL_SOM_VENTER_PÅ_SAMTYKKE), false, true, oppretteNokkel(personidentHovedpart));
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

  public boolean ferdigstilleSamtykkeoppgave(String eventId, String personidentMotpart) {
    log.info("Ferdigstiller samtykkeoppgave med eventId {}", eventId);
    try {
      ferdigprodusent.ferdigstilleSamtykkeoppgave(oppretteNokkel(eventId, personidentMotpart));
      return true;
    } catch (InternFeil internFeilException) {
      log.error("En feil oppstod ved sending av ferdigmelding for oppgave med eventId {}.", eventId);
      return false;
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
