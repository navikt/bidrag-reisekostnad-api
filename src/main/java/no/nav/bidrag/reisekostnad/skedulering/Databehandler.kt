package no.nav.bidrag.reisekostnad.skedulering

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.reisekostnad.tjeneste.Arkiveringstjeneste
import no.nav.bidrag.reisekostnad.tjeneste.Databasetjeneste
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class Databehandler(private val arkiveringstjeneste: Arkiveringstjeneste, private val databasetjeneste: Databasetjeneste) {
    @Scheduled(cron = "\${kjøreplan.databehandling.arkivering}")
    @SchedulerLock(name = "forespørsel_til_arkiv", lockAtLeastFor = "PT5M", lockAtMostFor = "PT14M")
    fun arkiverForespørslerSomErKlareForInnsending() {
        val idForespørslerForInnsending = databasetjeneste.henteForespørslerSomErKlareForInnsending()
        log.info("Fant totalt ${idForespørslerForInnsending.size} forespørsler som vil bli forsøkt oversendt til dokumentarkivet")
        idForespørslerForInnsending.forEach {
            arkiveringstjeneste.arkivereForespørsel(it);
            log.info("Arkivering av forespørsel med id $it ble utført.")
        }
        log.info("Arkivering av alle de ${idForespørslerForInnsending.size} forespørslene er utført")
    }
}