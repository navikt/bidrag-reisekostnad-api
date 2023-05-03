package no.nav.bidrag.reisekostnad.skedulering

import io.kotest.assertions.assertSoftly
import io.mockk.every
import io.mockk.verify
import no.nav.bidrag.reisekostnad.database.datamodell.Oppgavebestilling
import no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING
import no.nav.bidrag.reisekostnad.model.hovedpartIdent
import no.nav.bidrag.reisekostnad.model.motpartIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@DisplayName("DeaktivereJournalførteOgUtgåtteForespørslerTest")
class DeaktivereJournalførteOgUtgåtteForespørslerTest : DatabehandlerTest() {

    @BeforeEach
    fun setup() {

        every { brukernotifikasjonkonsument.varsleForeldreOmManglendeSamtykke(any(), any(), any()) } returns Unit
        every { brukernotifikasjonkonsument.ferdigstilleSamtykkeoppgave(any(), any()) } returns true
    }

    @Test
    fun skalDeaktivereJournalførtForespørsel() {

        // gitt
        val journalførtForespørsel = opppretteForespørsel(true)

        journalførtForespørsel.opprettet = LocalDateTime.now()
            .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING + 13)
        journalførtForespørsel.journalført =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING)
                .atStartOfDay()

        val lagretForespørsel = forespørselDao.save(journalførtForespørsel)

        every { brukernotifikasjonkonsument.varsleForeldreOmManglendeSamtykke(any(), any(), any()) } returns Unit
        every { brukernotifikasjonkonsument.ferdigstilleSamtykkeoppgave(any(), any()) } returns true

        // hvis
        databehandler.deaktivereJournalførteOgUtgåtteForespørsler()

        // så
        val deaktivertForespørsel = forespørselDao.findById(lagretForespørsel.id)

        assertSoftly {
            assertThat(deaktivertForespørsel.isPresent)
            assertThat(deaktivertForespørsel.get().deaktivert).isNotNull
            assertThat(deaktivertForespørsel.get().deaktivert.toLocalDate()).isEqualTo(LocalDate.now())
            assertThat(deaktivertForespørsel.get().journalført).isNotNull
        }
    }

    @Test
    fun skalDeaktivereForespørselMedUtløptSamtykkefrist() {

        // gitt
        val forespørselMedUtløptSamtykkefrist = opppretteForespørsel(true)
        forespørselMedUtløptSamtykkefrist.opprettet =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING + 1)
                .atStartOfDay()

        val lagretForespørsel = forespørselDao.save(forespørselMedUtløptSamtykkefrist)

        val eventId = UUID.randomUUID().toString()
        val samtykkeoppgave = Oppgavebestilling.builder().eventId(eventId).forespørsel(lagretForespørsel)
            .forelder(lagretForespørsel.motpart).build()
        oppgavebestillingDao.save(samtykkeoppgave)

        // hvis
        databehandler.deaktivereJournalførteOgUtgåtteForespørsler()

        // så
        val deaktivertForespørsel = forespørselDao.findById(lagretForespørsel.id)

        assertSoftly {
            assertThat(deaktivertForespørsel.isPresent)
            assertThat(deaktivertForespørsel.get().deaktivert).isNotNull
            assertThat(deaktivertForespørsel.get().deaktivert.toLocalDate()).isEqualTo(LocalDate.now())
            assertThat(deaktivertForespørsel.get().samtykket).isNull()
            assertThat(deaktivertForespørsel.get().journalført).isNull()
            verify(exactly = 1) { brukernotifikasjonkonsument.ferdigstilleSamtykkeoppgave(any(), any()) }
            verify(exactly = 1) {
                brukernotifikasjonkonsument.varsleForeldreOmManglendeSamtykke(
                    forespørselMedUtløptSamtykkefrist.hovedpartIdent,
                    forespørselMedUtløptSamtykkefrist.motpartIdent,
                    forespørselMedUtløptSamtykkefrist.opprettet.toLocalDate()
                )
            }
        }
    }

    @Test
    fun skalSletteSamtykkeoppgaveSelvOmVarslingAvForeldreFeiler() {

        // gitt
        val forespørselMedFeilOgUtløptSamtykkefrist = opppretteForespørsel(true)
        forespørselMedFeilOgUtløptSamtykkefrist.barn = mutableSetOf(testpersonBarn11, testpersonBarn15_2)
        forespørselMedFeilOgUtløptSamtykkefrist.hovedpart.personident = "123"
        forespørselMedFeilOgUtløptSamtykkefrist.opprettet =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING + 1)
                .atStartOfDay()

        val lagretForespørselMedFeilOgUtløptSamtykkefrist = forespørselDao.save(forespørselMedFeilOgUtløptSamtykkefrist)

        val eventId = UUID.randomUUID().toString()
        val samtykkeoppgave =
            Oppgavebestilling.builder().eventId(eventId).forespørsel(lagretForespørselMedFeilOgUtløptSamtykkefrist)
                .forelder(lagretForespørselMedFeilOgUtløptSamtykkefrist.motpart).build()
        oppgavebestillingDao.save(samtykkeoppgave)

        every { brukernotifikasjonkonsument.varsleForeldreOmManglendeSamtykke(any(), any(), any()) } returns Unit
        every { brukernotifikasjonkonsument.ferdigstilleSamtykkeoppgave(any(), any()) } returns false

        // hvis
        databehandler.deaktivereJournalførteOgUtgåtteForespørsler()

        // så
        val deaktivertForespørselMedFeilOgUtløptSamtykkefrist =
            forespørselDao.findById(lagretForespørselMedFeilOgUtløptSamtykkefrist.id)

        assertSoftly {
            assertThat(deaktivertForespørselMedFeilOgUtløptSamtykkefrist.isPresent)
            assertThat(deaktivertForespørselMedFeilOgUtløptSamtykkefrist.get().deaktivert).isNotNull
            assertThat(deaktivertForespørselMedFeilOgUtløptSamtykkefrist.get().deaktivert.toLocalDate()).isEqualTo(
                LocalDate.now()
            )
            assertThat(deaktivertForespørselMedFeilOgUtløptSamtykkefrist.get().samtykket).isNull()
            assertThat(deaktivertForespørselMedFeilOgUtløptSamtykkefrist.get().journalført).isNull()
            verify(exactly = 1) { brukernotifikasjonkonsument.ferdigstilleSamtykkeoppgave(any(), any()) }
        }
    }


}
