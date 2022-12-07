package no.nav.bidrag.reisekostnad.api;

import java.time.LocalDate;
import java.util.HashMap;
import lombok.Value;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.bidrag.reisekostnad.BidragReisekostnadApiTestapplikasjon;
import no.nav.bidrag.reisekostnad.Testkonfig;
import no.nav.bidrag.reisekostnad.database.dao.BarnDao;
import no.nav.bidrag.reisekostnad.database.dao.ForelderDao;
import no.nav.bidrag.reisekostnad.database.dao.ForespørselDao;
import no.nav.bidrag.reisekostnad.konfigurasjon.Profil;
import no.nav.security.mock.oauth2.MockOAuth2Server;
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

  protected static final String KONTROLLERKONTEKST = "/api/v1/reisekostnad";
  protected final static String ENDEPUNKT_BRUKERINFORMASJON = KONTROLLERKONTEKST + "/brukerinformasjon";
  protected final static String ENDEPUNKT_NY_FORESPØRSEL = KONTROLLERKONTEKST + "/forespoersel/ny";
  protected final static String ENDEPUNKT_TREKKE_FORESPØRSEL = KONTROLLERKONTEKST + "/forespoersel/trekke";

  protected String urlBrukerinformasjon;
  protected String urlNyForespørsel;
  protected String urlTrekkeForespørsel;

  @BeforeEach
  public void oppsett() {
    urlBrukerinformasjon = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + ENDEPUNKT_BRUKERINFORMASJON;
    urlNyForespørsel = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + ENDEPUNKT_NY_FORESPØRSEL;
    urlTrekkeForespørsel = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + ENDEPUNKT_TREKKE_FORESPØRSEL + "?id=%s";
  }

  protected static Testperson testpersonGråtass = new Testperson("12345678910", "Gråtass", 40);
  protected static Testperson testpersonStreng = new Testperson("11111122222", "Streng", 38);
  protected static Testperson testpersonBarn16 = new Testperson("77777700000", "Grus", 16);
  protected static Testperson testpersonBarn10 = new Testperson("33333355555", "Småstein", 10);
  protected static Testperson testpersonIkkeFunnet = new Testperson("00000001231", "Utenfor", 29);
  protected static Testperson testpersonHarDiskresjon = new Testperson("23451644512", "Diskos", 29);
  protected static Testperson testpersonHarMotpartMedDiskresjon = new Testperson("56472134561", "Tordivel", 44);
  protected static Testperson testpersonHarBarnMedDiskresjon = new Testperson("32456849111", "Kaktus", 48);
  protected static Testperson testpersonErDød = new Testperson("77765415234", "Steindød", 35);
  protected static Testperson testpersonHarDødtBarn = new Testperson("05784456310", "Albueskjell", 53);
  protected static Testperson testpersonDødMotpart = new Testperson("445132456487", "Bunkers", 41);
  protected static Testperson testpersonServerfeil = new Testperson("12000001231", "Feil", 78);

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
