package no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.database.dao.OppgavebestillingDao;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.InternFeil;
import no.nav.bidrag.reisekostnad.konfigurasjon.Egenskaper;
import no.nav.bidrag.reisekostnad.tjeneste.Databasetjeneste;
import no.nav.brukernotifikasjon.schemas.builders.DoneInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.DoneInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Ferdigprodusent {

  KafkaTemplate kafkaTemplate;
  Databasetjeneste databasetjeneste;
  OppgavebestillingDao oppgavebestillingDao;
  Egenskaper egenskaper;

  public void ferdigstilleFarsSigneringsoppgave(NokkelInput nokkel) {

    var oppgaveSomSkalFerdigstilles = oppgavebestillingDao.henteOppgavebestilling(nokkel.getEventId());

    if (!egenskaper.getBrukernotifikasjon().getSkruddPaa()) {
      log.warn("Brukernotifikasjoner er skrudd av - ferdigbestilling av oppgave ble derfor ikke sendt.");
      return;
    }

    if (oppgaveSomSkalFerdigstilles.isPresent() && oppgaveSomSkalFerdigstilles.get().getFerdigstilt() == null) {
      var melding = oppretteDone();
      try {
        kafkaTemplate.send(egenskaper.getBrukernotifikasjon().getEmneFerdig(), nokkel, melding);
      } catch (Exception e) {
        throw new InternFeil(Feilkode.BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE, e);
      }
      log.info("Ferdigmelding ble sendt for oppgave med eventId {}.");
      databasetjeneste.setteOppgaveTilFerdigstilt(nokkel.getEventId());
    } else {
      log.warn("Fant ingen aktiv oppgavebestilling for eventId {}. Bestiller derfor ikke ferdigstilling.", nokkel.getEventId());
    }
  }

  private DoneInput oppretteDone() {
    return new DoneInputBuilder()
        .withTidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime())
        .build();
  }
}
