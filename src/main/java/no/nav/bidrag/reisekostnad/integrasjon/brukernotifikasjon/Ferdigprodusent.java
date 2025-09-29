package no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.database.dao.OppgavebestillingDao;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.InternFeil;
import no.nav.bidrag.reisekostnad.konfigurasjon.Egenskaper;
import no.nav.bidrag.reisekostnad.tjeneste.Databasetjeneste;
import no.nav.tms.varsel.builder.InaktiverVarselBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Ferdigprodusent {

  KafkaTemplate kafkaTemplate;
  Databasetjeneste databasetjeneste;
  OppgavebestillingDao oppgavebestillingDao;
  Egenskaper egenskaper;

  public void ferdigstilleSamtykkeoppgave(String eventId) {

    var oppgaveSomSkalFerdigstilles = oppgavebestillingDao.henteOppgavebestilling(eventId);

    if (!egenskaper.getBrukernotifikasjon().getSkruddPaa()) {
      log.warn("Brukernotifikasjoner er skrudd av - ferdigbestilling av oppgave ble derfor ikke sendt.");
      return;
    }

    if (oppgaveSomSkalFerdigstilles.isPresent() && oppgaveSomSkalFerdigstilles.get().getFerdigstilt() == null) {
      var melding = oppretteDone(eventId);
      try {
        kafkaTemplate.send(egenskaper.getBrukernotifikasjon().getEmneBrukernotifikasjon(), eventId, melding);
      } catch (Exception e) {
        throw new InternFeil(Feilkode.BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE, e);
      }
      log.info("Ferdigmelding ble sendt for oppgave med eventId {}.", eventId);
      databasetjeneste.setteOppgaveTilFerdigstilt(eventId);
    } else {
      log.warn("Fant ingen aktiv oppgavebestilling for eventId {}. Bestiller derfor ikke ferdigstilling.", eventId);
    }
  }

  private String oppretteDone(String eventId) {
    log.info("Inaktiverer varsel med eventId {}", eventId);

    return InaktiverVarselBuilder.newInstance()
        .withVarselId(eventId)
        .withProdusent(
            egenskaper.getCluster(),
            egenskaper.getNamespace(),
            egenskaper.getAppnavn())
        .build();
  }
}
