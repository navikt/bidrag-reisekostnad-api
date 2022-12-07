package no.nav.bidrag.reisekostnad.skedulering;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import no.nav.bidrag.reisekostnad.tjeneste.Databasetjeneste;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Databehandler {

  private final Databasetjeneste databasetjeneste;

  @Autowired
  public Databehandler(Databasetjeneste databasetjeneste) {
    this.databasetjeneste = databasetjeneste;
  }

  @Scheduled(cron = "${kjøreplan.databehandling.arkivering}")
  @SchedulerLock(name = "forespørsel_til_arkiv", lockAtLeastFor = "PT5M", lockAtMostFor = "PT14M")
  public void skalImplementeres() {

    var idForespørslerForInnsending = databasetjeneste.henteForespørslerSomErKlareForInnsending();

    log.info("Skedulert oppgave ble trigget, men implementasjon mangler");
  }
}