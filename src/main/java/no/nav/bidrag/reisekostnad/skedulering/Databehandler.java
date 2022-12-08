package no.nav.bidrag.reisekostnad.skedulering;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.bidrag.reisekostnad.tjeneste.Arkiveringstjeneste;
import no.nav.bidrag.reisekostnad.tjeneste.Databasetjeneste;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Databehandler {

  private final Arkiveringstjeneste arkiveringstjeneste;
  private final Databasetjeneste databasetjeneste;

  @Autowired
  public Databehandler(Arkiveringstjeneste arkiveringstjeneste, Databasetjeneste databasetjeneste) {
    this.arkiveringstjeneste = arkiveringstjeneste;
    this.databasetjeneste = databasetjeneste;
  }

  @Scheduled(cron = "${kjøreplan.databehandling.arkivering}")
  @SchedulerLock(name = "forespørsel_til_arkiv", lockAtLeastFor = "PT5M", lockAtMostFor = "PT14M")
  public void skalImplementeres() {

    var idForespørslerForInnsending = databasetjeneste.henteForespørslerSomErKlareForInnsending();

    log.info("Fant totalt {} forespørsler som vil bli forsøkt oversendt til dokumentarkivet", idForespørslerForInnsending.size());

    for (Integer id : idForespørslerForInnsending) {
      //arkiveringstjeneste.arkivereForespørsel(id);
      log.info("Arkivering av forespørsel med id {} ble ikke startet.", id);
    }

    log.info("Arkivering av alle de {} forespørslene ble gjennomført uten feil", idForespørslerForInnsending.size());
  }
}
