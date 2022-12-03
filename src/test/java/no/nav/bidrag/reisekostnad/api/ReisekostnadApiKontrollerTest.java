package no.nav.bidrag.reisekostnad.api;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Set;
import lombok.Value;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.bidrag.reisekostnad.BidragReisekostnadApiTestapplikasjon;
import no.nav.bidrag.reisekostnad.Testkonfig;
import no.nav.bidrag.reisekostnad.api.dto.inn.NyForespørselDto;
import no.nav.bidrag.reisekostnad.api.dto.ut.BrukerinformasjonDto;
import no.nav.bidrag.reisekostnad.database.dao.BarnDao;
import no.nav.bidrag.reisekostnad.database.dao.ForelderDao;
import no.nav.bidrag.reisekostnad.database.dao.ForespørselDao;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.Kjønn;
import no.nav.bidrag.reisekostnad.konfigurasjon.Profil;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Krypteringsverktøy;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

@Disabled("Brutt opp i egene klasser pga Wiremock race condition")
@DisplayName("ReisekostnadApiKontrollerTest")
@ActiveProfiles(Profil.TEST)
@EnableMockOAuth2Server
@AutoConfigureTestDatabase(replace = Replace.ANY)
@AutoConfigureWireMock(stubs = "file:src/test/java/resources/mappings", port = 0)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = {BidragReisekostnadApiTestapplikasjon.class, Testkonfig.class})
public class ReisekostnadApiKontrollerTest {

  private static final String KONTROLLERKONTEKST = "/api/v1/reisekostnad";
  private final static String ENDEPUNKT_BRUKERINFORMASJON = KONTROLLERKONTEKST + "/brukerinformasjon";
  private final static String ENDEPUNKT_NY_FORESPØRSEL = KONTROLLERKONTEKST + "/forespoersel/ny";
  private final static String ENDEPUNKT_TREKKE_FORESPØRSEL = KONTROLLERKONTEKST + "/forespoersel/trekke";
  private @Autowired MockOAuth2Server mockOAuth2Server;
  private @Autowired ServletWebServerApplicationContext webServerAppCtxt;
  @Autowired
  private @Qualifier("api") HttpHeaderTestRestTemplate httpHeaderTestRestTemplateApi;
  private @MockBean OAuth2AccessTokenService oAuth2AccessTokenService;
  private @Autowired ForespørselDao forespørselDao;
  private @Autowired ForelderDao forelderDao;
  private @Autowired BarnDao barnDao;

  private static Kontrollertestperson kontrollertestpersonGråtass = new Kontrollertestperson("12345678910", "Gråtass", 40);
  private static Kontrollertestperson kontrollertestpersonStreng = new Kontrollertestperson("11111122222", "Streng", 38);
  private static Kontrollertestperson kontrollertestpersonBarn16 = new Kontrollertestperson("77777700000", "Grus", 16);
  private static Kontrollertestperson kontrollertestpersonBarn10 = new Kontrollertestperson("33333355555", "Småstein", 10);
  private static Kontrollertestperson kontrollertestpersonIkkeFunnet = new Kontrollertestperson("00000001231", "Utenfor", 29);
  private static Kontrollertestperson kontrollertestpersonHarDiskresjon = new Kontrollertestperson("23451644512", "Diskos", 29);
  private static Kontrollertestperson kontrollertestpersonHarMotpartMedDiskresjon = new Kontrollertestperson("56472134561", "Tordivel", 44);
  private static Kontrollertestperson kontrollertestpersonHarBarnMedDiskresjon = new Kontrollertestperson("32456849111", "Kaktus", 48);
  private static Kontrollertestperson kontrollertestpersonErDød = new Kontrollertestperson("77765415234", "Steindød", 35);
  private static Kontrollertestperson kontrollertestpersonHarDødtBarn = new Kontrollertestperson("05784456310", "Albueskjell", 53);
  private static Kontrollertestperson kontrollertestpersonDødMotpart = new Kontrollertestperson("445132456487", "Bunkers", 41);
  private static Kontrollertestperson kontrollertestpersonServerfeil = new Kontrollertestperson("12000001231", "Feil", 78);

  private static class CustomHeader {

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

  private String generereTesttoken(String personident) {
    var claims = new HashMap<String, Object>();
    claims.put("idp", personident);
    var token = mockOAuth2Server.issueToken("tokenx", personident, "aud-localhost", claims);
    return "Bearer " + token.serialize();
  }

  @Nested
  class HenteBrukerinformasjon {

    private final static String ENDEPUNKT_BRUKERINFORMASJON = KONTROLLERKONTEKST + "/brukerinformasjon";
    private String urlBrukerinformasjon = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + ENDEPUNKT_BRUKERINFORMASJON;

    @Test
    void skalHenteBrukerinformasjonForHovedpartMedFamilierelasjoner() {

      // gitt
      var påloggetPerson = kontrollertestpersonGråtass;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));

      var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      // hvis
      var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      // så
      assertAll(
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
          () -> assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(1),
          () -> assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().size()).isEqualTo(1),
          () -> assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
              .size()).isEqualTo(1));

      var motpart = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getMotpart();
      var barnUnder15År = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
          .stream().findFirst().get();
      var barnMinst15År = brukerinformasjon.getBody().getBarnMinstFemtenÅr().stream().findFirst().get();

      assertAll(
          () -> assertThat(motpart.getFornavn()).isEqualTo(kontrollertestpersonStreng.getFornavn()),
          () -> assertThat(motpart.getFødselsdato()).isEqualTo(kontrollertestpersonStreng.getFødselsdato()),
          () -> assertThat(barnUnder15År.getFødselsdato()).isEqualTo(kontrollertestpersonBarn10.getFødselsdato()),
          () -> assertThat(barnUnder15År.getFornavn()).isEqualTo(kontrollertestpersonBarn10.getFornavn()),
          () -> assertThat(barnMinst15År.getFødselsdato()).isEqualTo(kontrollertestpersonBarn16.getFødselsdato()),
          () -> assertThat(barnMinst15År.getFornavn()).isEqualTo(kontrollertestpersonBarn16.getFornavn()));
    }

    @Test
    void skalHenteBrukerinformasjonForHovedpartMedDiskresjon() {

      // gitt
      var påloggetPerson = kontrollertestpersonHarDiskresjon;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));

      var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      // hvis
      var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      // så
      assertAll(
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(brukerinformasjon.getBody().getKjønn()).isEqualTo(Kjønn.MANN),
          () -> assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
          () -> assertThat(brukerinformasjon.getBody().isHarDiskresjon()).isEqualTo(true),
          () -> assertThat(brukerinformasjon.getBody().isKanSøkeOmFordelingAvReisekostnader()).isEqualTo(false),
          () -> assertThat(brukerinformasjon.getBody().isHarSkjulteFamilieenheterMedDiskresjon()).isEqualTo(false),
          () -> assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getBody().getForespørslerSomMotpart().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK));
    }

    @Test
    void skalGiStatuskode404DersomPersondataMangler() {

      // gitt
      var påloggetPerson = kontrollertestpersonIkkeFunnet;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));

      var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      // hvis
      var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      // så
      assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void skalGiStatuskode500DersomKallMotBidragPersonFeilerMed500() {

      // gitt
      var påloggetPerson = kontrollertestpersonServerfeil;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));
      var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      // hvis
      var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      // så
      assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void skalFiltrereBortFamilieenhetHvorMotpartHarDiskresjon() {

      // gitt
      var påloggetPerson = kontrollertestpersonHarMotpartMedDiskresjon;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));
      var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      // hvis
      var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      // så
      assertAll(
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(brukerinformasjon.getBody().getKjønn()).isEqualTo(Kjønn.UKJENT),
          () -> assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
          () -> assertThat(brukerinformasjon.getBody().isHarDiskresjon()).isEqualTo(false),
          () -> assertThat(brukerinformasjon.getBody().isHarSkjulteFamilieenheterMedDiskresjon()).isEqualTo(true),
          () -> assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getBody().getForespørslerSomMotpart().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK)
      );
    }

    @Test
    void skalFiltrereBortFamilieenhetHvorBarnHarDiskresjon() {

      // gitt
      var påloggetPerson = kontrollertestpersonHarBarnMedDiskresjon;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));
      var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      // hvis
      var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      // så
      assertAll(
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(brukerinformasjon.getBody().getKjønn()).isEqualTo(Kjønn.MANN),
          () -> assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
          () -> assertThat(brukerinformasjon.getBody().isHarDiskresjon()).isEqualTo(false),
          () -> assertThat(brukerinformasjon.getBody().isHarSkjulteFamilieenheterMedDiskresjon()).isEqualTo(true),
          () -> assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getBody().getForespørslerSomMotpart().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK)
      );
    }

    @Test
    void skalGi403ForDødPerson() {

      // gitt
      var påloggetPerson = kontrollertestpersonErDød;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));
      var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      // hvis
      var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      // så vil
      assertAll(
          () -> assertThat(brukerinformasjon.getHeaders().get("Warning").get(0)).isEqualTo(Feilkode.PDL_PERSON_DØD.name()),
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN)
      );
    }

    @Test
    void skalFiltrereBortFamilieenheterDerMotpartErDød() {

      // gitt
      var påloggetPerson = kontrollertestpersonDødMotpart;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));
      var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      // hvis
      var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      // så
      assertAll(
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(brukerinformasjon.getBody().getKjønn()).isEqualTo(Kjønn.KVINNE),
          () -> assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
          () -> assertThat(brukerinformasjon.getBody().isHarDiskresjon()).isEqualTo(false),
          () -> assertThat(brukerinformasjon.getBody().isHarSkjulteFamilieenheterMedDiskresjon()).isEqualTo(false),
          () -> assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getBody().getForespørslerSomMotpart().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK)
      );
    }

    @Test
    void skalFiltrereBortDødeBarn() {

      // gitt
      var påloggetPerson = kontrollertestpersonHarDødtBarn;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));
      var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      // hvis
      var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      // så
      assertAll(
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(brukerinformasjon.getBody().getKjønn()).isEqualTo(Kjønn.KVINNE),
          () -> assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
          () -> assertThat(brukerinformasjon.getBody().isHarDiskresjon()).isEqualTo(false),
          () -> assertThat(brukerinformasjon.getBody().isHarSkjulteFamilieenheterMedDiskresjon()).isEqualTo(false),
          () -> assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().size()).isEqualTo(1),
          () -> assertThat(brukerinformasjon.getBody().getForespørslerSomMotpart().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(0),
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK)
      );

      var familieenhet = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst();

      assertThat(familieenhet).isPresent();

      var motpart = familieenhet.get().getMotpart();
      assertAll(
          () -> assertThat(motpart.getFødselsdato()).isEqualTo(LocalDate.now().minusYears(38)),
          () -> assertThat(motpart.getFornavn()).isEqualTo("Streng"),
          () -> assertThat(Krypteringsverktøy.dekryptere(motpart.getIdent())).isEqualTo("11111122222")
      );

      var barnUnder15 = familieenhet.get().getFellesBarnUnder15År();

      assertThat(barnUnder15.size()).isEqualTo(1);

      var barnetSomLever = barnUnder15.stream().findFirst();

      assertAll(
          () -> assertThat(barnetSomLever).isPresent(),
          () -> assertThat(Krypteringsverktøy.dekryptere(barnetSomLever.get().getIdent())).isEqualTo("33333355555"),
          () -> assertThat(barnetSomLever.get().getFornavn()).isEqualTo("Småstein"),
          () -> assertThat(barnetSomLever.get().getFødselsdato()).isEqualTo(LocalDate.now().minusYears(10))
      );
    }
  }

  @Nested
  class OppretteForespørselOmFordelingAvReisekostnader {

    private String urlNyForespørsel = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + ENDEPUNKT_NY_FORESPØRSEL;
    private String urlBrukerinformasjon = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + ENDEPUNKT_BRUKERINFORMASJON;

    @BeforeEach
    public void sletteTestdata() {
      barnDao.deleteAll();
      forespørselDao.deleteAll();
      forelderDao.deleteAll();
      ;
    }

    @Test
    void skalOppretteForespørselOmFordelingAvReisekostnaderForEttAvToFellesBarn() {

      // gitt
      var påloggetPerson = kontrollertestpersonGråtass;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));

      var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      var nyForespørsel = new NyForespørselDto(Set.of(Krypteringsverktøy.kryptere(kontrollertestpersonBarn10.getIdent())));

      // hvis
      var responsOpprett = httpHeaderTestRestTemplateApi.exchange(urlNyForespørsel, HttpMethod.POST, initHttpEntity(nyForespørsel),
          Void.class);

      // så
      var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      assertAll(
          () -> assertThat(responsOpprett.getStatusCode()).isEqualTo(HttpStatus.CREATED),
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
          () -> assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(1),
          () -> assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().size()).isEqualTo(1),
          () -> assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
              .size()).isEqualTo(1));

      var motpart = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getMotpart();
      var barnUnder15År = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
          .stream().findFirst().get();
      var barnMinst15År = brukerinformasjon.getBody().getBarnMinstFemtenÅr().stream().findFirst().get();

      assertAll(
          () -> assertThat(motpart.getFornavn()).isEqualTo(kontrollertestpersonStreng.getFornavn()),
          () -> assertThat(motpart.getFødselsdato()).isEqualTo(kontrollertestpersonStreng.getFødselsdato()),
          () -> assertThat(barnUnder15År.getFødselsdato()).isEqualTo(kontrollertestpersonBarn10.getFødselsdato()),
          () -> assertThat(barnUnder15År.getFornavn()).isEqualTo(kontrollertestpersonBarn10.getFornavn()),
          () -> assertThat(barnMinst15År.getFødselsdato()).isEqualTo(kontrollertestpersonBarn16.getFødselsdato()),
          () -> assertThat(barnMinst15År.getFornavn()).isEqualTo(kontrollertestpersonBarn16.getFornavn()));

      /* ----------- Verifisere lagret forespørsel  ----------- */

      assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(1);

      var lagretForespørsel = brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().findFirst();

      assertAll(
          () -> assertThat(lagretForespørsel).isPresent(),
          () -> assertThat(lagretForespørsel.get().getOpprettet()).isNotNull(),
          () -> assertThat(lagretForespørsel.get().getJournalført()).isNull(),
          () -> assertThat(lagretForespørsel.get().getSamtykket()).isNull(),
          () -> assertThat(lagretForespørsel.get().isKreverSamtykke()).isTrue(),
          () -> assertThat(lagretForespørsel.get().getHovedpart().getIdent()).isEqualTo(Krypteringsverktøy.kryptere(kontrollertestpersonGråtass.getIdent())),
          () -> assertThat(lagretForespørsel.get().getHovedpart().getFornavn()).isEqualTo(kontrollertestpersonGråtass.getFornavn()),
          () -> assertThat(lagretForespørsel.get().getHovedpart().getFødselsdato()).isEqualTo(kontrollertestpersonGråtass.getFødselsdato()),
          () -> assertThat(lagretForespørsel.get().getMotpart().getIdent()).isEqualTo(Krypteringsverktøy.kryptere(kontrollertestpersonStreng.getIdent())),
          () -> assertThat(lagretForespørsel.get().getMotpart().getFornavn()).isEqualTo(kontrollertestpersonStreng.getFornavn()),
          () -> assertThat(lagretForespørsel.get().getMotpart().getFødselsdato()).isEqualTo(kontrollertestpersonStreng.getFødselsdato()),
          () -> assertThat(lagretForespørsel.get().getBarn().size()).isEqualTo(1)
      );

      var barnILagretForespørsel = lagretForespørsel.get().getBarn().stream().findFirst();

      assertAll(
          () -> assertThat(barnILagretForespørsel).isPresent(),
          () -> assertThat(barnILagretForespørsel.get().getFornavn()).isEqualTo(kontrollertestpersonBarn10.getFornavn()),
          () -> assertThat(Krypteringsverktøy.dekryptere(barnILagretForespørsel.get().getIdent())).isEqualTo(kontrollertestpersonBarn10.getIdent()),
          () -> assertThat(barnILagretForespørsel.get().getFødselsdato()).isEqualTo(kontrollertestpersonBarn10.getFødselsdato())
      );
    }

    @Test
    void skalOppretteForespørselOmFordelingAvReisekostnaderForToAvToFellesBarn() {

      // gitt
      var påloggetPerson = kontrollertestpersonGråtass;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));

      var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      var nyForespørsel = new NyForespørselDto(
          Set.of(Krypteringsverktøy.kryptere(kontrollertestpersonBarn16.getIdent()), Krypteringsverktøy.kryptere(kontrollertestpersonBarn10.getIdent())));

      // hvis
      var responsOpprett = httpHeaderTestRestTemplateApi.exchange(urlNyForespørsel, HttpMethod.POST, initHttpEntity(nyForespørsel),
          Void.class);

      // så
      var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      assertAll(
          () -> assertThat(responsOpprett.getStatusCode()).isEqualTo(HttpStatus.CREATED),
          () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
          () -> assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(1),
          () -> assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().size()).isEqualTo(1),
          () -> assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
              .size()).isEqualTo(1));

      var motpart = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getMotpart();
      var barnUnder15År = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
          .stream().findFirst().get();
      var barnMinst15År = brukerinformasjon.getBody().getBarnMinstFemtenÅr().stream().findFirst().get();

      assertAll(
          () -> assertThat(motpart.getFornavn()).isEqualTo(kontrollertestpersonStreng.getFornavn()),
          () -> assertThat(motpart.getFødselsdato()).isEqualTo(kontrollertestpersonStreng.getFødselsdato()),
          () -> assertThat(barnUnder15År.getFødselsdato()).isEqualTo(kontrollertestpersonBarn10.getFødselsdato()),
          () -> assertThat(barnUnder15År.getFornavn()).isEqualTo(kontrollertestpersonBarn10.getFornavn()),
          () -> assertThat(barnMinst15År.getFødselsdato()).isEqualTo(kontrollertestpersonBarn16.getFødselsdato()),
          () -> assertThat(barnMinst15År.getFornavn()).isEqualTo(kontrollertestpersonBarn16.getFornavn()));

      /* ----------- Verifisere lagrede forespørsler  ----------- */
      assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(2);

      var lagretForespørselBarnUnder15 = brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().filter(f -> f.isKreverSamtykke() == true)
          .findFirst();

      assertAll(
          () -> assertThat(lagretForespørselBarnUnder15).isPresent(),
          () -> assertThat(lagretForespørselBarnUnder15.get().getOpprettet()).isNotNull(),
          () -> assertThat(lagretForespørselBarnUnder15.get().getJournalført()).isNull(),
          () -> assertThat(lagretForespørselBarnUnder15.get().getSamtykket()).isNull(),
          () -> assertThat(lagretForespørselBarnUnder15.get().isKreverSamtykke()).isTrue(),
          () -> assertThat(lagretForespørselBarnUnder15.get().getHovedpart().getIdent()).isEqualTo(
              Krypteringsverktøy.kryptere(kontrollertestpersonGråtass.getIdent())),
          () -> assertThat(lagretForespørselBarnUnder15.get().getHovedpart().getFornavn()).isEqualTo(kontrollertestpersonGråtass.getFornavn()),
          () -> assertThat(lagretForespørselBarnUnder15.get().getHovedpart().getFødselsdato()).isEqualTo(kontrollertestpersonGråtass.getFødselsdato()),
          () -> assertThat(lagretForespørselBarnUnder15.get().getMotpart().getIdent()).isEqualTo(
              Krypteringsverktøy.kryptere(kontrollertestpersonStreng.getIdent())),
          () -> assertThat(lagretForespørselBarnUnder15.get().getMotpart().getFornavn()).isEqualTo(kontrollertestpersonStreng.getFornavn()),
          () -> assertThat(lagretForespørselBarnUnder15.get().getMotpart().getFødselsdato()).isEqualTo(kontrollertestpersonStreng.getFødselsdato()),
          () -> assertThat(lagretForespørselBarnUnder15.get().getBarn().size()).isEqualTo(1)
      );

      var barnILagretForespørsel = lagretForespørselBarnUnder15.get().getBarn().stream().findFirst();

      assertAll(
          () -> assertThat(barnILagretForespørsel).isPresent(),
          () -> assertThat(barnILagretForespørsel.get().getFornavn()).isEqualTo(kontrollertestpersonBarn10.getFornavn()),
          () -> assertThat(Krypteringsverktøy.dekryptere(barnILagretForespørsel.get().getIdent())).isEqualTo(kontrollertestpersonBarn10.getIdent()),
          () -> assertThat(barnILagretForespørsel.get().getFødselsdato()).isEqualTo(kontrollertestpersonBarn10.getFødselsdato())
      );

      var lagretForespørselBarnOver15 = brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().filter(f -> f.isKreverSamtykke() == false)
          .findFirst();
      assertAll(
          () -> assertThat(lagretForespørselBarnOver15).isPresent(),
          () -> assertThat(lagretForespørselBarnOver15.get().getOpprettet()).isNotNull(),
          () -> assertThat(lagretForespørselBarnOver15.get().getJournalført()).isNull(),
          () -> assertThat(lagretForespørselBarnOver15.get().getSamtykket()).isNull(),
          () -> assertThat(lagretForespørselBarnOver15.get().isKreverSamtykke()).isFalse(),
          () -> assertThat(lagretForespørselBarnOver15.get().getHovedpart().getIdent()).isEqualTo(
              Krypteringsverktøy.kryptere(kontrollertestpersonGråtass.getIdent())),
          () -> assertThat(lagretForespørselBarnOver15.get().getHovedpart().getFornavn()).isEqualTo(kontrollertestpersonGråtass.getFornavn()),
          () -> assertThat(lagretForespørselBarnOver15.get().getHovedpart().getFødselsdato()).isEqualTo(kontrollertestpersonGråtass.getFødselsdato()),
          () -> assertThat(lagretForespørselBarnOver15.get().getMotpart().getIdent()).isEqualTo(
              Krypteringsverktøy.kryptere(kontrollertestpersonStreng.getIdent())),
          () -> assertThat(lagretForespørselBarnOver15.get().getMotpart().getFornavn()).isEqualTo(kontrollertestpersonStreng.getFornavn()),
          () -> assertThat(lagretForespørselBarnOver15.get().getMotpart().getFødselsdato()).isEqualTo(kontrollertestpersonStreng.getFødselsdato()),
          () -> assertThat(lagretForespørselBarnOver15.get().getBarn().size()).isEqualTo(1)
      );

      var barnOver15ILagretForespørsel = lagretForespørselBarnOver15.get().getBarn().stream().findFirst();

      assertAll(
          () -> assertThat(barnOver15ILagretForespørsel).isPresent(),
          () -> assertThat(barnOver15ILagretForespørsel.get().getFornavn()).isEqualTo(kontrollertestpersonBarn16.getFornavn()),
          () -> assertThat(Krypteringsverktøy.dekryptere(barnOver15ILagretForespørsel.get().getIdent())).isEqualTo(kontrollertestpersonBarn16.getIdent()),
          () -> assertThat(barnOver15ILagretForespørsel.get().getFødselsdato()).isEqualTo(kontrollertestpersonBarn16.getFødselsdato())
      );
    }
  }

  @Nested
  class GiSamtykke {

  }

  @Nested
  class TrekkeForespørsel {

    private String urlBrukerinformasjon = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + ENDEPUNKT_BRUKERINFORMASJON;
    private String urlNyForespørsel = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + ENDEPUNKT_NY_FORESPØRSEL;

    private String urlTrekkeForespørsel = "http://localhost:" + webServerAppCtxt.getWebServer().getPort() + ENDEPUNKT_TREKKE_FORESPØRSEL + "?id=%s";

    @BeforeEach
    public void sletteTestdata() {
      barnDao.deleteAll();
      forespørselDao.deleteAll();
      forelderDao.deleteAll();
    }

    @Test
    void skalKunneTrekkeForespørsel() {

      // gitt
      var påloggetPerson = kontrollertestpersonGråtass;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));

      var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      var nyForespørsel = new NyForespørselDto(Set.of(Krypteringsverktøy.kryptere(kontrollertestpersonBarn10.getIdent())));

      var responsOpprett = httpHeaderTestRestTemplateApi.exchange(urlNyForespørsel, HttpMethod.POST, initHttpEntity(nyForespørsel),
          Void.class);

      assertThat(responsOpprett.getStatusCode()).isEqualTo(HttpStatus.CREATED);

      var brukerinformasjonMedAktivForespørsel = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      assertAll(
          () -> assertThat(brukerinformasjonMedAktivForespørsel.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(brukerinformasjonMedAktivForespørsel.getBody().getForespørslerSomHovedpart().size()).isEqualTo(1),
          () -> assertThat(brukerinformasjonMedAktivForespørsel.getBody().getForespørslerSomHovedpart().stream().findFirst().get().getId())
      );

      var idLagretForespørsel = brukerinformasjonMedAktivForespørsel.getBody().getForespørslerSomHovedpart().stream().findFirst().get().getId();

      // hvis
      var url = String.format(urlTrekkeForespørsel, idLagretForespørsel);
      var trukketrespons = httpHeaderTestRestTemplateApi.exchange(url, HttpMethod.PUT, initHttpEntity(null),
          Void.class);

      // så
      assertThat(trukketrespons.getStatusCode().is2xxSuccessful());

      var brukerinformasjonMedTrukketForespørsel = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
          BrukerinformasjonDto.class);

      assertAll(
          () -> assertThat(brukerinformasjonMedTrukketForespørsel.getStatusCode()).isEqualTo(HttpStatus.OK),
          () -> assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
          () -> assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(1),
          () -> assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getMotparterMedFellesBarnUnderFemtenÅr().size()).isEqualTo(1),
          () -> assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get()
              .getFellesBarnUnder15År()
              .size()).isEqualTo(1));

      var motpart = brukerinformasjonMedTrukketForespørsel.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getMotpart();
      var barnUnder15År = brukerinformasjonMedTrukketForespørsel.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get()
          .getFellesBarnUnder15År()
          .stream().findFirst().get();
      var barnMinst15År = brukerinformasjonMedTrukketForespørsel.getBody().getBarnMinstFemtenÅr().stream().findFirst().get();

      assertAll(
          () -> assertThat(motpart.getFornavn()).isEqualTo(kontrollertestpersonStreng.getFornavn()),
          () -> assertThat(motpart.getFødselsdato()).isEqualTo(kontrollertestpersonStreng.getFødselsdato()),
          () -> assertThat(barnUnder15År.getFødselsdato()).isEqualTo(kontrollertestpersonBarn10.getFødselsdato()),
          () -> assertThat(barnUnder15År.getFornavn()).isEqualTo(kontrollertestpersonBarn10.getFornavn()),
          () -> assertThat(barnMinst15År.getFødselsdato()).isEqualTo(kontrollertestpersonBarn16.getFødselsdato()),
          () -> assertThat(barnMinst15År.getFornavn()).isEqualTo(kontrollertestpersonBarn16.getFornavn()));

      /* ----------- Verifisere lagrede forespørsler  ----------- */
      assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getForespørslerSomHovedpart().size()).isEqualTo(0);
      var trukketForespørsel = forespørselDao.findById(idLagretForespørsel);
      assertAll(
          () -> assertThat(trukketForespørsel.isPresent()),
          () -> assertThat(trukketForespørsel.get().getDeaktivert()).isNotNull()
      );
    }

    @Test
    void skalKunneOppretteNyForespørselForBarnMedTrukketForespørsel() {

      // gitt
      skalKunneTrekkeForespørsel();

      var påloggetPerson = kontrollertestpersonGråtass;
      httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));

      var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
      when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

      var f = forespørselDao.findById(1);

      var nyForespørsel = new NyForespørselDto(Set.of(Krypteringsverktøy.kryptere(kontrollertestpersonBarn10.getIdent())));

      // hvis
      var responsOpprett = httpHeaderTestRestTemplateApi.exchange(urlNyForespørsel, HttpMethod.POST, initHttpEntity(nyForespørsel),
          Void.class);

      // så
      assertThat(responsOpprett.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    }
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
