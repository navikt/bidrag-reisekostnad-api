package no.nav.bidrag.reisekostnad.api;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.stubbing.StubMapping;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Value;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.bidrag.transport.dokument.OpprettJournalpostRequest;
import no.nav.bidrag.reisekostnad.BidragReisekostnadApiTestapplikasjon;
import no.nav.bidrag.reisekostnad.StubsKt;
import no.nav.bidrag.reisekostnad.Testkonfig;
import no.nav.bidrag.reisekostnad.database.dao.BarnDao;
import no.nav.bidrag.reisekostnad.database.dao.ForelderDao;
import no.nav.bidrag.reisekostnad.database.dao.ForespørselDao;
import no.nav.bidrag.reisekostnad.database.datamodell.Barn;
import no.nav.bidrag.reisekostnad.database.datamodell.Forelder;
import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel;
import no.nav.bidrag.reisekostnad.konfigurasjon.Profil;
import no.nav.bidrag.reisekostnad.tjeneste.Databasetjeneste;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Mapper;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles(Profil.TEST)
@EnableMockOAuth2Server
@AutoConfigureTestDatabase(replace = Replace.ANY)
@AutoConfigureWireMock(stubs = "file:src/test/java/resources/mappings", port = 0)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {BidragReisekostnadApiTestapplikasjon.class, Testkonfig.class})
@DisplayName("ReisekostnadApiKontrollerTest")
public class KontrollerTest {

  protected @Autowired MockOAuth2Server mockOAuth2Server;
  protected @Autowired ServletWebServerApplicationContext webServerAppCtxt;

  @Autowired
  protected @Qualifier("api") HttpHeaderTestRestTemplate httpHeaderTestRestTemplateApi;
  protected @MockBean OAuth2AccessTokenService oAuth2AccessTokenService;
  protected @Autowired ForespørselDao forespørselDao;
  protected @Autowired ForelderDao forelderDao;
  protected @Autowired BarnDao barnDao;
  protected @Autowired Mapper mapper;
  protected @Autowired Databasetjeneste databasetjeneste;

  protected static final String KONTROLLERKONTEKST = "/api/v1/reisekostnad";
  protected final static String ENDEPUNKT_BRUKERINFORMASJON = KONTROLLERKONTEKST + "/brukerinformasjon";
  protected final static String ENDEPUNKT_NY_FORESPØRSEL = KONTROLLERKONTEKST + "/forespoersel/ny";
  protected final static String ENDEPUNKT_SAMTYKKE_FORESPØRSEL = KONTROLLERKONTEKST + "/forespoersel/samtykke";
  protected final static String ENDEPUNKT_TREKKE_FORESPØRSEL = KONTROLLERKONTEKST + "/forespoersel/trekke";

  protected String urlBrukerinformasjon;
  protected String urlNyForespørsel;
  protected String urlSamtykkeForespørsel;
  protected String urlTrekkeForespørsel;

  @BeforeEach
  public void oppsett() {
    sletteTestdata();
    urlBrukerinformasjon = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + ENDEPUNKT_BRUKERINFORMASJON;
    urlNyForespørsel = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + ENDEPUNKT_NY_FORESPØRSEL;
    urlSamtykkeForespørsel = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + ENDEPUNKT_SAMTYKKE_FORESPØRSEL;
    urlTrekkeForespørsel = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + ENDEPUNKT_TREKKE_FORESPØRSEL + "?id=%s";
  }

  private void sletteTestdata() {
    barnDao.deleteAll();
    forelderDao.deleteAll();
    forespørselDao.deleteAll();
  }

  protected static String opprettetJournalpostId = "1232132132";

  protected static class CustomHeader {

    String headerName;
    String headerValue;

    CustomHeader(String headerName, String headerValue) {
      this.headerName = headerName;
      this.headerValue = headerValue;
    }
  }

  static <T> HttpEntity<T> initHttpEntity(T body, CustomHeader... customHeaders) {

    var headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);

    if (customHeaders != null) {
      for (var header : customHeaders) {
        headers.add(header.headerName, header.headerValue);
      }
    }
    return new HttpEntity<>(body, headers);
  }

  protected String generereTesttoken(String personident) {
    var claims = new HashMap<String, Object>();
    claims.put("idp", personident);
    var token = mockOAuth2Server.issueToken("tokenx", personident, "aud-localhost", claims);
    return "Bearer " + token.serialize();
  }

  protected Forespørsel lagreForespørselForEttBarn(String personidentHovedpart, String personidentMotpart, String personidentBarn,
      boolean barnErUnder15) {

    var hovedpart = Forelder.builder().personident(personidentHovedpart).build();
    var motpart = Forelder.builder().personident(personidentMotpart).build();
    var barn = Barn.builder().personident(personidentBarn).build();
    var forespørsel = Forespørsel.builder()
        .opprettet(LocalDateTime.now())
        .hovedpart(hovedpart)
        .motpart(motpart)
        .barn(Set.of(barn))
        .kreverSamtykke(barnErUnder15)
        .samtykkefrist(LocalDate.now().plusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING))
        .build();

    return forespørselDao.save(forespørsel);
  }

  protected void initTokenForPåloggetPerson(String personident) {
    httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(personident));

    var a = new OAuth2AccessTokenResponse(generereTesttoken(personident), 1000, 1000, null);
    when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);
  }

  protected List<OpprettJournalpostRequest> hentOpprettDokumentRequestBodyForForespørsel(Integer forespørselId) {
    var objectMapper = new ObjectMapper().findAndRegisterModules();
    var results = WireMock.findAll(StubsKt.getBidragDokumentRequestPatternBuilder(forespørselId));
    if (results.isEmpty()) {
      return null;
    }

    return results.stream().map((p) -> {
      try {
        return objectMapper.readValue(p.getBodyAsString(), OpprettJournalpostRequest.class);
      } catch (JsonProcessingException e) {
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.toList());

  }

  protected StubMapping stubArkiverDokumentFeiler() {
    return WireMock.stubFor(
        WireMock.post(WireMock.urlEqualTo("/bidrag-dokument/journalpost/JOARK")).willReturn(
            aResponse()
                .withHeader(HttpHeaders.CONNECTION, "close")
                .withStatus(HttpStatus.BAD_REQUEST.value())
        )
    );
  }

}

@Value
class Testperson {

  String ident;
  String fornavn;
  LocalDate fødselsdato;

  public Testperson(String ident, String fornavn, int alder) {
    this.ident = ident;
    this.fornavn = fornavn;
    this.fødselsdato = LocalDate.now().minusYears(alder);
  }

}
