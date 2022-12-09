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
    fun skalArkivereForespørslerSomIkkeKreverSamtykke(){
        val forespørsel = oppprettForespørsel()
        forespørselDao.save(forespørsel)
        databehandler.arkiverForespørslerSomErKlareForInnsending()

        assertSoftly {
            forespørsel.journalført shouldHaveSameDayAs LocalDateTime.now()
            forespørsel.idJournalpost shouldBe "1232132132"
            verifiserDokumentArkivertForForespørsel(forespørsel.id, 1)
        }
    }

    protected fun verifiserDokumentArkivertForForespørsel(forespørselId: Int, antallGanger: Int = 1) {
        WireMock.verify(antallGanger, getBidragDokumentRequestPatternBuilder(forespørselId))
    }
}