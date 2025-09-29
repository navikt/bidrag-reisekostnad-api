package no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.InternFeil;
import no.nav.bidrag.reisekostnad.konfigurasjon.Egenskaper;
import no.nav.bidrag.reisekostnad.tjeneste.Databasetjeneste;
import no.nav.tms.varsel.action.Sensitivitet;
import no.nav.tms.varsel.action.Varseltype;
import no.nav.tms.varsel.builder.OpprettVarselBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@AllArgsConstructor
public class Oppgaveprodusent {

  private KafkaTemplate kafkaTemplate;
  private Databasetjeneste databasetjeneste;
  private URL reisekostnadUrl;
  private Egenskaper egenskaper;

  public void oppretteOppgaveOmSamtykke(int idForespørsel, String personidentMotpart, DynamiskMelding oppgavetekst, String eventId) {

    var melding = oppretteOppgave(oppgavetekst.hentFormatertMelding(), reisekostnadUrl, personidentMotpart, eventId);
    log.info("Melding opprettet: {}", melding);
    var motpartsAktiveSamtykkeoppgaver = databasetjeneste.henteAktiveOppgaverMotpart(idForespørsel, personidentMotpart);

    if (motpartsAktiveSamtykkeoppgaver.isEmpty()) {
      log.info("Oppretter oppgave om samtykke til motpart i forespørsel med id {}", idForespørsel);

      if (egenskaper.getBrukernotifikasjon().getSkruddPaa()) {
        oppretteOppgave(eventId, melding);
        log.info("Samtykkeoppgave opprettet for forespørsel med id {}.", idForespørsel);
        databasetjeneste.lagreNyOppgavebestilling(idForespørsel, eventId);
      } else {
        log.warn("Brukernotifikasjoner er skrudd av - oppgavebestilling ble derfor ikke sendt.");
      }
    }
  }

  private void oppretteOppgave(String eventId, String melding) {
    try {
      kafkaTemplate.send(egenskaper.getBrukernotifikasjon().getEmneBrukernotifikasjon(), eventId, melding);
    } catch (Exception e) {
      log.error("Opprettelse av oppgave {} til forelder feilet!", melding, e);
      throw new InternFeil(Feilkode.BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE, e);
    }
  }

  private String oppretteOppgave(String oppgavetekst, URL reisekostnadUrl, String fodselsnummer, String eventId) {
    log.info("Oppretter oppgave med eventId {} og melding {}", eventId, oppgavetekst);

    return OpprettVarselBuilder.newInstance()
        .withType(Varseltype.Oppgave)
        .withVarselId(eventId)
        .withSensitivitet(
            Sensitivitet.valueOf(
                egenskaper.getBrukernotifikasjon().getSikkerhetsnivaaOppgave()))
        .withIdent(fodselsnummer)
        .withTekst("nb", oppgavetekst, true)
        .withLink(reisekostnadUrl.toString())
        .withAktivFremTil(
            ZonedDateTime.now(ZoneId.of("UTC"))
                .plusDays(
                    egenskaper
                        .getBrukernotifikasjon()
                        .getLevetidOppgaveAntallDager()))
        .withEksternVarsling()
        .withProdusent(
            egenskaper.getCluster(),
            egenskaper.getNamespace(),
            egenskaper.getAppnavn())
        .build();
  }
}
