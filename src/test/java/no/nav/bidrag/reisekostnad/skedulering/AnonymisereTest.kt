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
        var anonymiseringsklarForespørsel = opppretteForespørsel(true)

        anonymiseringsklarForespørsel.opprettet =
            LocalDateTime.now()
                .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() * 2 + 13)
        anonymiseringsklarForespørsel.journalført =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() * 2)
                .atStartOfDay()
        anonymiseringsklarForespørsel.deaktivert =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() + 1)
                .atStartOfDay()

        var lagretAnonymiseringsklarForespørsel = forespørselDao.save(anonymiseringsklarForespørsel)
        var hovedpart = forelderDao.findById(lagretAnonymiseringsklarForespørsel.hovedpart.id)
        var motpart = forelderDao.findById(lagretAnonymiseringsklarForespørsel.motpart.id)

        assertSoftly {
            assertThat(hovedpart).isPresent
            assertThat(motpart).isPresent
        }

        // hvis
        databehandler.anonymisereBarnOgSletteForeldreSomIkkeErKnyttetTilAktiveForespørsler();

        // så
        var anonymisertForespørsel = forespørselDao.findById(lagretAnonymiseringsklarForespørsel.id);

        var hovedpartEtterSletting = forelderDao.findById(lagretAnonymiseringsklarForespørsel.hovedpart.id)
        var motpartEtterSletting = forelderDao.findById(lagretAnonymiseringsklarForespørsel.motpart.id)

        assertSoftly {
            assertThat(hovedpartEtterSletting).isEmpty
            assertThat(motpartEtterSletting).isEmpty
            assertThat(anonymisertForespørsel).isPresent
            assertThat(anonymisertForespørsel.get().deaktivert).isNotNull
            assertThat(anonymisertForespørsel.get().deaktivert.toLocalDate()).isEqualTo(
                LocalDate.now()
                    .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() + 1)
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
        var aktivForespørsel = opppretteForespørsel(true)

        aktivForespørsel.opprettet =
            LocalDateTime.now()
                .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() * 2 + 13)
        aktivForespørsel.journalført =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() * 2)
                .atStartOfDay()
        aktivForespørsel.deaktivert =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() - 5)
                .atStartOfDay()

        var lagretAnonymiseringsklarForespørsel = forespørselDao.save(aktivForespørsel)

        // hvis
        databehandler.anonymisereBarnOgSletteForeldreSomIkkeErKnyttetTilAktiveForespørsler();

        // så
        var anonymisertForespørsel = forespørselDao.findById(lagretAnonymiseringsklarForespørsel.id);

        assertSoftly {
            assertThat(anonymisertForespørsel.isPresent)
            assertThat(anonymisertForespørsel.get().deaktivert).isNotNull
            assertThat(anonymisertForespørsel.get().deaktivert.toLocalDate()).isEqualTo(
                LocalDate.now()
                    .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() - 5)
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
        var anonymiseringsklarForespørsel = opppretteForespørsel(true)
        var deaktiveringstidspunkt =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() + 5)
                .atStartOfDay()

        anonymiseringsklarForespørsel.opprettet =
            LocalDateTime.now()
                .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() * 2 + 13)
        anonymiseringsklarForespørsel.journalført =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() * 2)
                .atStartOfDay()
        anonymiseringsklarForespørsel.deaktivert = deaktiveringstidspunkt

        var lagretAnonymiseringsklarForespørsel = forespørselDao.save(anonymiseringsklarForespørsel)
        var lagretIkkeAnonymiseringsklarForespørsel = databasetjeneste.lagreNyForespørsel(
            testpersonGråtass.personident,
            testpersonStreng.personident,
            mutableSetOf(testpersonBarn13.personident),
            true
        )

        // hvis
        databehandler.anonymisereBarnOgSletteForeldreSomIkkeErKnyttetTilAktiveForespørsler();

        // så
        var anonymisertForespørsel = forespørselDao.findById(lagretAnonymiseringsklarForespørsel.id);
        var ikkeAnonymiseringsklarForespørsel = forespørselDao.findById(lagretIkkeAnonymiseringsklarForespørsel.id)

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
        var ikkeAnonymiseringsklarForespørsel = opppretteForespørsel(true)

        ikkeAnonymiseringsklarForespørsel.opprettet =
            LocalDateTime.now()
                .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() * 2 + 13)
        ikkeAnonymiseringsklarForespørsel.journalført =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() * 2)
                .atStartOfDay()

        var lagretAnonymiseringsklarForespørsel = forespørselDao.save(ikkeAnonymiseringsklarForespørsel)

        // hvis
        databehandler.anonymisereBarnOgSletteForeldreSomIkkeErKnyttetTilAktiveForespørsler();

        // så
        var ikkeAnonymisertForespørsel = forespørselDao.findById(lagretAnonymiseringsklarForespørsel.id);

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
        var anonymiseringsklarForespørsel = opppretteForespørsel(true)

        anonymiseringsklarForespørsel.opprettet =
            LocalDateTime.now()
                .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() * 2 + 13)
        anonymiseringsklarForespørsel.journalført =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() * 2)
                .atStartOfDay()
        anonymiseringsklarForespørsel.deaktivert =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() + 1)
                .atStartOfDay()

        var lagretAnonymiseringsklarForespørsel = forespørselDao.save(anonymiseringsklarForespørsel)

        var eventId = UUID.randomUUID().toString();
        var samtykkeoppgave =
            Oppgavebestilling.builder().eventId(eventId).forespørsel(lagretAnonymiseringsklarForespørsel)
                .forelder(lagretAnonymiseringsklarForespørsel.motpart).build();
        oppgavebestillingDao.save(samtykkeoppgave);

        // hvis
        databehandler.anonymisereBarnOgSletteForeldreSomIkkeErKnyttetTilAktiveForespørsler();

        // så
        var anonymisertForespørsel = forespørselDao.findById(lagretAnonymiseringsklarForespørsel.id);

        assertSoftly {
            assertThat(anonymisertForespørsel.isPresent)
            assertThat(anonymisertForespørsel.get().deaktivert).isNotNull
            assertThat(anonymisertForespørsel.get().deaktivert.toLocalDate()).isEqualTo(
                LocalDate.now()
                    .minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() + 1)
            )
            assertThat(anonymisertForespørsel.get().journalført).isNotNull
            assertThat(anonymisertForespørsel.get().hovedpart).isNull()
            assertThat(anonymisertForespørsel.get().motpart).isNotNull
            assertThat(anonymisertForespørsel.get().barn.stream().findFirst().get().personident).isNull()
        }
    }
}
