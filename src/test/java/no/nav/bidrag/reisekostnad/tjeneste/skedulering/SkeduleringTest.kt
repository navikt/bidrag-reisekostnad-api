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
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.EntityManager
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
    lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    lateinit var forelderDao: ForelderDao

    @Autowired
    lateinit var entityManager: EntityManager

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
    protected var testpersonBarn16 = Barn.builder().personident("77777700000").fødselsdato(LocalDate.now().minusYears(16)).build()
    protected var testpersonBarn10 = Barn.builder().personident("33333355555").fødselsdato(LocalDate.now().minusYears(10)).build()
    protected var testpersonBarn11 = Barn.builder().personident("42124124").fødselsdato(LocalDate.now().minusYears(11)).build()
    protected var testpersonBarn12 = Barn.builder().personident("335533133355555").fødselsdato(LocalDate.now().minusYears(12)).build()
    protected var testpersonBarn15 = Barn.builder().personident("23232323455555").fødselsdato(LocalDate.now().minusYears(15)).build()
    protected var testpersonBarn15_2 = Barn.builder().personident("5515155").fødselsdato(LocalDate.now().minusYears(15)).build()

    fun oppprettForespørsel(kreverSamtykke: Boolean = false): Forespørsel{
        return Forespørsel.builder()
            .opprettet(LocalDateTime.now())
            .hovedpart(testpersonGråtass)
            .motpart(testpersonStreng)
            .barn(mutableSetOf(testpersonBarn10, testpersonBarn16))
            .kreverSamtykke(kreverSamtykke)
            .samtykkefrist(LocalDate.now().plusDays(4))
            .build()
    }
}