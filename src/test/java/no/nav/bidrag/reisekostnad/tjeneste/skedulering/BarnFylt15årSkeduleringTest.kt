package no.nav.bidrag.reisekostnad.tjeneste.skedulering

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.date.shouldHaveSameDayAs
import io.kotest.matchers.date.shouldHaveSameYearAs
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.bidrag.reisekostnad.verifiserDokumentArkivertForForespørsel
import no.nav.bidrag.reisekostnad.verifiserDokumentArkivertForForespørselAntallGanger
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("BarnFylt15årSkeduleringTest")
class BarnFylt15årSkeduleringTest: SkeduleringTest() {

    @Test
    fun skalOppretteNyForespørselForBarnSomHarFylt15år(){
        var forespørselUtenBarnOver15år = oppprettForespørsel(true)
        forespørselUtenBarnOver15år.barn = mutableSetOf(testpersonBarn11, testpersonBarn12)

        var originalForespørsel = oppprettForespørsel(true)
        originalForespørsel.barn = mutableSetOf(testpersonBarn10, testpersonBarn15)

        transactionTemplate.executeWithoutResult {
            forespørselDao.save(originalForespørsel)
            forespørselDao.save(forespørselUtenBarnOver15år)
        }

        databehandler.behandleForespørslerSomInneholderBarnSomHarNyligFylt15År()

        val alleForespørsler = forespørselDao.findAll().toList()
        originalForespørsel = alleForespørsler.find { it.id == originalForespørsel.id }!!
        forespørselUtenBarnOver15år = alleForespørsler.find { it.id == forespørselUtenBarnOver15år.id }!!
        val forespørselMedBarnFylt15År = alleForespørsler.find { it.id != originalForespørsel.id && it.id != forespørselUtenBarnOver15år.id }

        assertSoftly {
            alleForespørsler.size shouldBe 3

            forespørselUtenBarnOver15år.barn.size shouldBe 2
            forespørselUtenBarnOver15år.samtykket shouldBe null

            forespørselMedBarnFylt15År shouldNotBe null
            forespørselMedBarnFylt15År!!.barn.size shouldBe 1
            forespørselMedBarnFylt15År.isKreverSamtykke shouldBe false
            forespørselMedBarnFylt15År.journalført shouldHaveSameDayAs LocalDateTime.now()
            forespørselMedBarnFylt15År.journalført shouldHaveSameYearAs LocalDateTime.now()
            forespørselMedBarnFylt15År.idJournalpost shouldBe "1232132132"

            originalForespørsel.barn.size shouldBe 1
            originalForespørsel.barn.first().fødselsdato shouldBe LocalDate.now().minusYears(10)

            verifiserDokumentArkivertForForespørsel(forespørselMedBarnFylt15År.id)
        }
    }

    @Test
    fun skalRulleTilbakeEndringerVedFeil(){
        var originalForespørselMedFeil = oppprettForespørsel(true)
        originalForespørselMedFeil.barn = mutableSetOf(testpersonBarn11, testpersonBarn15_2)
        originalForespørselMedFeil.hovedpart = null

        var originalForespørsel = oppprettForespørsel(true)
        originalForespørsel.barn = mutableSetOf(testpersonBarn10, testpersonBarn15)

        transactionTemplate.executeWithoutResult {
            forespørselDao.save(originalForespørsel)
            forespørselDao.save(originalForespørselMedFeil)
        }

        databehandler.behandleForespørslerSomInneholderBarnSomHarNyligFylt15År()

        val alleForespørsler = transactionTemplate.execute { forespørselDao.findAll().toList() }!!

        originalForespørsel = alleForespørsler.find { it.id == originalForespørsel.id }!!
        originalForespørselMedFeil = alleForespørsler.find { it.id == originalForespørselMedFeil.id }!!
        val forespørselMedBarnFylt15År = alleForespørsler.find { it.id != originalForespørsel.id && it.id != originalForespørselMedFeil.id }

        assertSoftly {
            alleForespørsler.size shouldBe 3
            originalForespørselMedFeil.barn.size shouldBe 2
            originalForespørsel.barn.size shouldBe 1

            forespørselMedBarnFylt15År shouldNotBe null
            forespørselMedBarnFylt15År!!.barn.size shouldBe 1
            forespørselMedBarnFylt15År.isKreverSamtykke shouldBe false
            forespørselMedBarnFylt15År.journalført shouldHaveSameDayAs LocalDateTime.now()
            forespørselMedBarnFylt15År.journalført shouldHaveSameYearAs LocalDateTime.now()
            forespørselMedBarnFylt15År.idJournalpost shouldBe "1232132132"

            originalForespørsel.barn.size shouldBe 1
            originalForespørsel.barn.first().fødselsdato shouldBe LocalDate.now().minusYears(10)

            verifiserDokumentArkivertForForespørsel(forespørselMedBarnFylt15År.id)

            verifiserDokumentArkivertForForespørselAntallGanger(1)
        }
    }
}