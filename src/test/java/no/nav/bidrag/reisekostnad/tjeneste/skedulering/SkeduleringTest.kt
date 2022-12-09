package no.nav.bidrag.reisekostnad.tjeneste.skedulering

import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.date.shouldHaveSameDayAs
import io.kotest.matchers.shouldBe
import no.nav.bidrag.reisekostnad.BidragReisekostnadApiTestapplikasjon
import no.nav.bidrag.reisekostnad.database.dao.BarnDao
import no.nav.bidrag.reisekostnad.database.dao.ForelderDao
import no.nav.bidrag.reisekostnad.database.dao.ForespørselDao
import no.nav.bidrag.reisekostnad.database.datamodell.Barn
import no.nav.bidrag.reisekostnad.database.datamodell.Forelder
import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel
import no.nav.bidrag.reisekostnad.getBidragDokumentRequestPatternBuilder
import no.nav.bidrag.reisekostnad.konfigurasjon.Profil
import no.nav.bidrag.reisekostnad.skedulering.Databehandler
import no.nav.bidrag.reisekostnad.verifiserDokumentArkivertForForespørsel
import no.nav.bidrag.reisekostnad.verifiserDokumentIkkeArkivertForForespørsel
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime
import javax.transaction.Transactional

@ActiveProfiles(value = [Profil.TEST, Profil.HENDELSE])
@EnableMockOAuth2Server
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@AutoConfigureWireMock(stubs = ["file:src/test/java/resources/mappings"], port = 0)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [BidragReisekostnadApiTestapplikasjon::class]
)
@DisplayName("ReisekostnadApiKontrollerTest")
class SkeduleringTest {

    @Autowired
    lateinit var forespørselDao: ForespørselDao

    @Autowired
    lateinit var forelderDao: ForelderDao

    @Autowired
    lateinit var barnDao: BarnDao

    @Autowired
    lateinit var databehandler: Databehandler

    @BeforeEach
    fun sletteTestdata() {
        WireMock.resetAllRequests()
        barnDao.deleteAll()
        forespørselDao.deleteAll()
        forelderDao.deleteAll()
    }

    protected var testpersonGråtass = Forelder.builder().personident("12345678910").build()
    protected var testpersonStreng = Forelder.builder().personident("11111122222").build()
    protected var testpersonBarn16 = Barn.builder().personident("77777700000").build()
    protected var testpersonBarn10 = Barn.builder().personident("33333355555").build()

    fun oppprettForespørsel(): Forespørsel{
        return Forespørsel.builder()
            .opprettet(LocalDateTime.now())
            .hovedpart(testpersonGråtass)
            .motpart(testpersonStreng)
            .barn(mutableSetOf(testpersonBarn10, testpersonBarn16))
            .kreverSamtykke(false)
            .samtykkefrist(LocalDate.now().plusDays(4))
            .build()
    }

    @Test
    @Transactional
    fun skalArkivereFlereForespørsler(){
        val forespørsel = oppprettForespørsel()
        val forespørsel2 = oppprettForespørsel()
        val forespørsel3 = oppprettForespørsel()
        forespørselDao.save(forespørsel)
        forespørselDao.save(forespørsel2)
        forespørselDao.save(forespørsel3)
        databehandler.arkiverForespørslerSomErKlareForInnsending()

        assertSoftly {
            forespørsel.journalført shouldHaveSameDayAs LocalDateTime.now()
            forespørsel2.journalført shouldHaveSameDayAs LocalDateTime.now()
            forespørsel3.journalført shouldHaveSameDayAs LocalDateTime.now()
            forespørsel.idJournalpost shouldBe "1232132132"
            verifiserDokumentArkivertForForespørsel(forespørsel.id)
            verifiserDokumentArkivertForForespørsel(forespørsel2.id)
            verifiserDokumentArkivertForForespørsel(forespørsel3.id)
        }
    }
    @Test
    @Transactional
    fun skalArkivereForespørslerSomIkkeKreverSamtykke(){
        val forespørsel = oppprettForespørsel()
        forespørsel.isKreverSamtykke = false
        forespørsel.samtykket = null
        forespørselDao.save(forespørsel)
        databehandler.arkiverForespørslerSomErKlareForInnsending()

        assertSoftly {
            forespørsel.journalført shouldHaveSameDayAs LocalDateTime.now()
            forespørsel.idJournalpost shouldBe "1232132132"
            verifiserDokumentArkivertForForespørsel(forespørsel.id)
        }
    }

    @Test
    @Transactional
    fun skalArkivereForespørslerSomErSamtykket(){
        val forespørsel = oppprettForespørsel()
        forespørsel.isKreverSamtykke = true
        forespørsel.samtykket = LocalDateTime.now()
        forespørselDao.save(forespørsel)
        databehandler.arkiverForespørslerSomErKlareForInnsending()

        assertSoftly {
            forespørsel.journalført shouldHaveSameDayAs LocalDateTime.now()
            forespørsel.idJournalpost shouldBe "1232132132"
            verifiserDokumentArkivertForForespørsel(forespørsel.id)
        }
    }

    @Test
    @Transactional
    fun skalIkkeArkivereForespørslerSomIkkeErSamtykket(){
        val forespørsel = oppprettForespørsel()
        forespørsel.isKreverSamtykke = true

        val forespørsel2 = oppprettForespørsel()
        forespørsel2.isKreverSamtykke = true

        val forespørselDeaktivert = oppprettForespørsel()
        forespørselDeaktivert.deaktivert = LocalDateTime.now()
        forespørselDao.save(forespørsel)
        forespørselDao.save(forespørselDeaktivert)
        forespørselDao.save(forespørsel2)
        databehandler.arkiverForespørslerSomErKlareForInnsending()

        assertSoftly {
            forespørsel.journalført shouldBe null
            forespørsel.idJournalpost shouldBe null
            forespørsel2.journalført shouldBe null
            forespørsel2.idJournalpost shouldBe null
            forespørselDeaktivert.journalført shouldBe null
            forespørselDeaktivert.idJournalpost shouldBe null
            verifiserDokumentIkkeArkivertForForespørsel(forespørsel.id)
            verifiserDokumentIkkeArkivertForForespørsel(forespørselDeaktivert.id)
            verifiserDokumentIkkeArkivertForForespørsel(forespørsel2.id)
        }
    }
}