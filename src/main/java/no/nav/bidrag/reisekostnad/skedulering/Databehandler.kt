package no.nav.bidrag.reisekostnad.skedulering

import mu.KotlinLogging
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock
import no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Brukernotifikasjonkonsument
import no.nav.bidrag.reisekostnad.model.alleBarnHarFylt15år
import no.nav.bidrag.reisekostnad.model.hovedpartIdent
import no.nav.bidrag.reisekostnad.model.motpartIdent
import no.nav.bidrag.reisekostnad.tjeneste.Arkiveringstjeneste
import no.nav.bidrag.reisekostnad.tjeneste.Databasetjeneste
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

@Component
class Databehandler(
    private val arkiveringstjeneste: Arkiveringstjeneste,
    private val brukernotifikasjonkonsument: Brukernotifikasjonkonsument,
    private val databasetjeneste: Databasetjeneste
) {
    @Scheduled(cron = "\${kjøreplan.databehandling.arkivering}")
    @SchedulerLock(name = "forespørsel_til_arkiv", lockAtLeastFor = "PT5M", lockAtMostFor = "PT14M")
    fun arkiverForespørslerSomErKlareForInnsending() {
        val idForespørslerForInnsending = databasetjeneste.henteForespørslerSomErKlareForInnsending()
        log.info("Fant totalt ${idForespørslerForInnsending.size} forespørsler som vil bli forsøkt oversendt til dokumentarkivet")
        idForespørslerForInnsending.forEach {
            arkiveringstjeneste.arkivereForespørsel(it)
            log.info("Arkivering av forespørsel med id $it ble utført.")
        }
        log.info("Arkivering av alle de ${idForespørslerForInnsending.size} forespørslene er utført")
    }

    @Scheduled(cron = "\${kjøreplan.databehandling.fylt_15}")
    @SchedulerLock(name = "forespørsel_barn_fylt_nylig_15år", lockAtLeastFor = "PT5M", lockAtMostFor = "PT14M")
    fun behandleForespørslerSomInneholderBarnSomHarNyligFylt15År() {
        val forespørslerOver15År = databasetjeneste.hentForespørselSomInneholderBarnSomHarFylt15år()
        log.info("Fant totalt ${forespørslerOver15År.size} forespørsler som inneholder barn som har nylig fylt 15 år")
        forespørslerOver15År.forEach { originalForespørsel ->
            try {
                val nyForespørsel = if (originalForespørsel.alleBarnHarFylt15år)
                    databasetjeneste.oppdaterForespørselTilÅIkkeKreveSamtykke(originalForespørsel.id)
                else databasetjeneste.overførBarnSomHarFylt15årTilNyForespørsel(originalForespørsel.id)
                arkiveringstjeneste.arkivereForespørsel(nyForespørsel.id)
                log.info("Antall barn i forespørselen som nettopp har fylt 15 år: {}", nyForespørsel.barn.size);
                brukernotifikasjonkonsument.varsleOmAutomatiskInnsending(
                    nyForespørsel.hovedpartIdent,
                    nyForespørsel.motpartIdent,
                    nyForespørsel.barn.stream().findFirst().get().fødselsdato
                )
            } catch (e: Exception) {
                log.error(
                    "Det skjedde en feil ved behandling av forespørsel ${originalForespørsel.id} som inneholder barn som har nylig fylt 15 år. Rullet tilbake alle endringer",
                    e
                )
            }
        }
        log.info("Behandlet alle forespørsler ${forespørslerOver15År.size} som inneholder barn som har nylig fylt 15 år")
    }
}