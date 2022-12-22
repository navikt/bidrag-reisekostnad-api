package no.nav.bidrag.reisekostnad.skedulering

import io.kotest.assertions.assertSoftly
import no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("AnonymisereTest")
class AnonymisereTest : DatabehandlerTest() {

    @Test
    fun skalAnonymisereBarnOgSletteForeldreSomIkkeErTilknyttetAktiveForespørsler() {

        // gitt
        var anonymiseringsklarForespørsel = opppretteForespørsel(true)

        anonymiseringsklarForespørsel.opprettet =
            LocalDateTime.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() * 2 + 13)
        anonymiseringsklarForespørsel.journalført =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() * 2).atStartOfDay()
        anonymiseringsklarForespørsel.deaktivert =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong()+1).atStartOfDay()

        var lagretAnonymiseringsklarForespørsel = forespørselDao.save(anonymiseringsklarForespørsel)

        // hvis
        databehandler.anonymisereBarnOgSletteForeldreSomIkkeErKnyttetTilAktiveForespørsler();

        // så
        var anonymisertForespørsel = forespørselDao.findById(lagretAnonymiseringsklarForespørsel.id);

        assertSoftly {
            assertThat(anonymisertForespørsel.isPresent)
            assertThat(anonymisertForespørsel.get().deaktivert).isNotNull
            assertThat(anonymisertForespørsel.get().deaktivert.toLocalDate()).isEqualTo(
                LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() + 1)
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
        var anonymiseringsklarForespørsel = opppretteForespørsel(true)

        anonymiseringsklarForespørsel.opprettet =
            LocalDateTime.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() * 2 + 13)
        anonymiseringsklarForespørsel.journalført =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() * 2).atStartOfDay()
        anonymiseringsklarForespørsel.deaktivert =
            LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong()-5).atStartOfDay()

        var lagretAnonymiseringsklarForespørsel = forespørselDao.save(anonymiseringsklarForespørsel)

        // hvis
        databehandler.anonymisereBarnOgSletteForeldreSomIkkeErKnyttetTilAktiveForespørsler();

        // så
        var anonymisertForespørsel = forespørselDao.findById(lagretAnonymiseringsklarForespørsel.id);

        assertSoftly {
            assertThat(anonymisertForespørsel.isPresent)
            assertThat(anonymisertForespørsel.get().deaktivert).isNotNull
            assertThat(anonymisertForespørsel.get().deaktivert.toLocalDate()).isEqualTo(
                LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING.toLong() -5)
            )
            assertThat(anonymisertForespørsel.get().journalført).isNotNull
            assertThat(anonymisertForespørsel.get().hovedpart).isNotNull()
            assertThat(anonymisertForespørsel.get().hovedpart).isNotNull()
            assertThat(anonymisertForespørsel.get().barn.stream().findFirst().get().personident).isNotNull()
        }
    }
}