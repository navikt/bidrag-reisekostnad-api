package no.nav.bidrag.reisekostnad.skedulering

import io.kotest.assertions.assertSoftly
import no.nav.bidrag.reisekostnad.database.datamodell.Oppgavebestilling
import no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@DisplayName("AnonymisereTest")
class AnonymisereTest : DatabehandlerTest() {

    @Test
    fun skalAnonymisereBarnOgSletteForeldreSomIkkeErTilknyttetAktiveForespørsler() {

        // gitt
        val anonymiseringsklarForespørsel = opppretteForespørsel(true)

        anonymiseringsklarForespørsel.opprettet =
            LocalDateTime.now()
                .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING * 2 + 13)
        anonymiseringsklarForespørsel.journalført =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING * 2)
                .atStartOfDay()
        anonymiseringsklarForespørsel.deaktivert =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING + 1)
                .atStartOfDay()

        val lagretAnonymiseringsklarForespørsel = forespørselDao.save(anonymiseringsklarForespørsel)
        val hovedpart = forelderDao.findById(lagretAnonymiseringsklarForespørsel.hovedpart.id)
        val motpart = forelderDao.findById(lagretAnonymiseringsklarForespørsel.motpart.id)

        assertSoftly {
            assertThat(hovedpart).isPresent
            assertThat(motpart).isPresent
        }

        // hvis
        databehandler.anonymisereBarnOgSletteForeldreSomIkkeErKnyttetTilAktiveForespørsler()

        // så
        val anonymisertForespørsel = forespørselDao.findById(lagretAnonymiseringsklarForespørsel.id)

        val hovedpartEtterSletting = forelderDao.findById(lagretAnonymiseringsklarForespørsel.hovedpart.id)
        val motpartEtterSletting = forelderDao.findById(lagretAnonymiseringsklarForespørsel.motpart.id)

        assertSoftly {
            assertThat(hovedpartEtterSletting).isEmpty
            assertThat(motpartEtterSletting).isEmpty
            assertThat(anonymisertForespørsel).isPresent
            assertThat(anonymisertForespørsel.get().deaktivert).isNotNull
            assertThat(anonymisertForespørsel.get().deaktivert.toLocalDate()).isEqualTo(
                LocalDate.now()
                    .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING + 1)
            )
            assertThat(anonymisertForespørsel.get().journalført).isNotNull
            assertThat(anonymisertForespørsel.get().hovedpart).isNull()
            assertThat(anonymisertForespørsel.get().motpart).isNull()
            assertThat(anonymisertForespørsel.get().barn.stream().findFirst().get().personident).isNull()
        }
    }

    @Test
    fun skalIkkeAnonymisereAktiveForespørsler() {

        // gitt
        val aktivForespørsel = opppretteForespørsel(true)

        aktivForespørsel.opprettet =
            LocalDateTime.now()
                .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING * 2 + 13)
        aktivForespørsel.journalført =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING * 2)
                .atStartOfDay()
        aktivForespørsel.deaktivert =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING - 5)
                .atStartOfDay()

        val lagretAnonymiseringsklarForespørsel = forespørselDao.save(aktivForespørsel)

        // hvis
        databehandler.anonymisereBarnOgSletteForeldreSomIkkeErKnyttetTilAktiveForespørsler()

        // så
        val anonymisertForespørsel = forespørselDao.findById(lagretAnonymiseringsklarForespørsel.id)

        assertSoftly {
            assertThat(anonymisertForespørsel.isPresent)
            assertThat(anonymisertForespørsel.get().deaktivert).isNotNull
            assertThat(anonymisertForespørsel.get().deaktivert.toLocalDate()).isEqualTo(
                LocalDate.now()
                    .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING - 5)
            )
            assertThat(anonymisertForespørsel.get().journalført).isNotNull
            assertThat(anonymisertForespørsel.get().hovedpart).isNotNull
            assertThat(anonymisertForespørsel.get().motpart).isNotNull
            assertThat(anonymisertForespørsel.get().barn.stream().findFirst().get().personident).isNotNull()
        }
    }

    @Test
    fun skalAnonymisereBarnUavhengigAvForeldre() {

        // gitt
        val anonymiseringsklarForespørsel = opppretteForespørsel(true)
        val deaktiveringstidspunkt =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING + 5)
                .atStartOfDay()

        anonymiseringsklarForespørsel.opprettet =
            LocalDateTime.now()
                .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING * 2 + 13)
        anonymiseringsklarForespørsel.journalført =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING * 2)
                .atStartOfDay()
        anonymiseringsklarForespørsel.deaktivert = deaktiveringstidspunkt

        val lagretAnonymiseringsklarForespørsel = forespørselDao.save(anonymiseringsklarForespørsel)
        val lagretIkkeAnonymiseringsklarForespørsel = databasetjeneste.lagreNyForespørsel(
            testpersonGråtass.personident,
            testpersonStreng.personident,
            mutableSetOf(testpersonBarn13.personident),
            true
        )

        // hvis
        databehandler.anonymisereBarnOgSletteForeldreSomIkkeErKnyttetTilAktiveForespørsler()

        // så
        val anonymisertForespørsel = forespørselDao.findById(lagretAnonymiseringsklarForespørsel.id)
        val ikkeAnonymiseringsklarForespørsel = forespørselDao.findById(lagretIkkeAnonymiseringsklarForespørsel.id)

        assertSoftly {
            assertThat(anonymisertForespørsel.isPresent)
            assertThat(anonymisertForespørsel.get().deaktivert).isNotNull
            assertThat(anonymisertForespørsel.get().deaktivert).isEqualTo(deaktiveringstidspunkt)
            assertThat(anonymisertForespørsel.get().anonymisert.toLocalDate()).isEqualTo(LocalDate.now())
            assertThat(anonymisertForespørsel.get().journalført).isNotNull
            assertThat(anonymisertForespørsel.get().hovedpart).isNotNull
            assertThat(anonymisertForespørsel.get().hovedpart.personident).isNotNull()
            assertThat(anonymisertForespørsel.get().motpart).isNotNull
            assertThat(anonymisertForespørsel.get().motpart.personident).isNotNull()
            assertThat(anonymisertForespørsel.get().barn.forEach { b ->
                b.personident == null && LocalDate.now().isEqual(b.anonymisert.toLocalDate())
            })
        }

        assertSoftly {
            assertThat(ikkeAnonymiseringsklarForespørsel.isPresent)
            assertThat(ikkeAnonymiseringsklarForespørsel.get().deaktivert).isNull()
            assertThat(ikkeAnonymiseringsklarForespørsel.get().anonymisert).isNull()
            assertThat(ikkeAnonymiseringsklarForespørsel.get().journalført).isNull()
            assertThat(ikkeAnonymiseringsklarForespørsel.get().hovedpart).isNotNull
            assertThat(ikkeAnonymiseringsklarForespørsel.get().hovedpart.personident).isNotNull()
            assertThat(ikkeAnonymiseringsklarForespørsel.get().motpart).isNotNull
            assertThat(ikkeAnonymiseringsklarForespørsel.get().motpart.personident).isNotNull()
            assertThat(ikkeAnonymiseringsklarForespørsel.get().barn.forEach { b ->
                b.personident != null && b.anonymisert == null
            })
        }
    }

    @Test
    fun skalIkkeAnonymisereForespørslerSomIkkeErDeaktivert() {

        // gitt
        val ikkeAnonymiseringsklarForespørsel = opppretteForespørsel(true)

        ikkeAnonymiseringsklarForespørsel.opprettet =
            LocalDateTime.now()
                .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING * 2 + 13)
        ikkeAnonymiseringsklarForespørsel.journalført =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING * 2)
                .atStartOfDay()

        val lagretAnonymiseringsklarForespørsel = forespørselDao.save(ikkeAnonymiseringsklarForespørsel)

        // hvis
        databehandler.anonymisereBarnOgSletteForeldreSomIkkeErKnyttetTilAktiveForespørsler()

        // så
        val ikkeAnonymisertForespørsel = forespørselDao.findById(lagretAnonymiseringsklarForespørsel.id)

        assertSoftly {
            assertThat(ikkeAnonymisertForespørsel.isPresent)
            assertThat(ikkeAnonymisertForespørsel.get().deaktivert).isNull()
            assertThat(ikkeAnonymisertForespørsel.get().journalført).isNotNull
            assertThat(ikkeAnonymisertForespørsel.get().hovedpart).isNotNull
            assertThat(ikkeAnonymisertForespørsel.get().motpart).isNotNull
            assertThat(ikkeAnonymisertForespørsel.get().barn.stream().findFirst().get().personident).isNotNull()
        }
    }

    @Test
    fun skalIkkeASletteForeldreMedAktiveBrukernotifikasjonsoppgaver() {

        // gitt
        val anonymiseringsklarForespørsel = opppretteForespørsel(true)

        anonymiseringsklarForespørsel.opprettet =
            LocalDateTime.now()
                .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING * 2 + 13)
        anonymiseringsklarForespørsel.journalført =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING * 2)
                .atStartOfDay()
        anonymiseringsklarForespørsel.deaktivert =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING + 1)
                .atStartOfDay()

        val lagretAnonymiseringsklarForespørsel = forespørselDao.save(anonymiseringsklarForespørsel)

        val eventId = UUID.randomUUID().toString()
        val samtykkeoppgave =
            Oppgavebestilling.builder().eventId(eventId).forespørsel(lagretAnonymiseringsklarForespørsel)
                .forelder(lagretAnonymiseringsklarForespørsel.motpart).build()
        oppgavebestillingDao.save(samtykkeoppgave)

        // hvis
        databehandler.anonymisereBarnOgSletteForeldreSomIkkeErKnyttetTilAktiveForespørsler()

        // så
        val anonymisertForespørsel = forespørselDao.findById(lagretAnonymiseringsklarForespørsel.id)

        assertSoftly {
            assertThat(anonymisertForespørsel.isPresent)
            assertThat(anonymisertForespørsel.get().deaktivert).isNotNull
            assertThat(anonymisertForespørsel.get().deaktivert.toLocalDate()).isEqualTo(
                LocalDate.now()
                    .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING + 1)
            )
            assertThat(anonymisertForespørsel.get().journalført).isNotNull
            assertThat(anonymisertForespørsel.get().hovedpart).isNull()
            assertThat(anonymisertForespørsel.get().motpart).isNotNull
            assertThat(anonymisertForespørsel.get().barn.stream().findFirst().get().personident).isNull()
        }
    }
}
