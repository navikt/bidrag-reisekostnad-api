package no.nav.bidrag.reisekostnad.api;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import no.nav.bidrag.reisekostnad.api.dto.ut.BrukerinformasjonDto;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.Kjønn;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Krypteringsverktøy;
import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
public class HenteBrukerinformasjonTest extends KontrollerTest {

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
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(1),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().size()).isEqualTo(1),
        () -> AssertionsForClassTypes.assertThat(
            brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
                .size()).isEqualTo(1));

    var motpart = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getMotpart();
    var barnUnder15År = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
        .stream().findFirst().get();
    var barnMinst15År = brukerinformasjon.getBody().getBarnMinstFemtenÅr().stream().findFirst().get();

    assertAll(
        () -> AssertionsForClassTypes.assertThat(motpart.getFornavn()).isEqualTo(kontrollertestpersonStreng.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(motpart.getFødselsdato()).isEqualTo(kontrollertestpersonStreng.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(barnUnder15År.getFødselsdato()).isEqualTo(kontrollertestpersonBarn10.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(barnUnder15År.getFornavn()).isEqualTo(kontrollertestpersonBarn10.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(barnMinst15År.getFødselsdato()).isEqualTo(kontrollertestpersonBarn16.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(barnMinst15År.getFornavn()).isEqualTo(kontrollertestpersonBarn16.getFornavn()));
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
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().isHarDiskresjon()).isEqualTo(true),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().isKanSøkeOmFordelingAvReisekostnader()).isEqualTo(false),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().isHarSkjulteFamilieenheterMedDiskresjon()).isEqualTo(false),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(0),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getForespørslerSomMotpart().size()).isEqualTo(0),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(0),
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
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().isHarDiskresjon()).isEqualTo(false),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().isHarSkjulteFamilieenheterMedDiskresjon()).isEqualTo(true),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(0),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getForespørslerSomMotpart().size()).isEqualTo(0),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(0),
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
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().isHarDiskresjon()).isEqualTo(false),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().isHarSkjulteFamilieenheterMedDiskresjon()).isEqualTo(true),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(0),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getForespørslerSomMotpart().size()).isEqualTo(0),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(0),
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
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getHeaders().get("Warning").get(0)).isEqualTo(Feilkode.PDL_PERSON_DØD.name()),
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
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().isHarDiskresjon()).isEqualTo(false),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().isHarSkjulteFamilieenheterMedDiskresjon()).isEqualTo(false),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(0),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getForespørslerSomMotpart().size()).isEqualTo(0),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(0),
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
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().isHarDiskresjon()).isEqualTo(false),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().isHarSkjulteFamilieenheterMedDiskresjon()).isEqualTo(false),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(0),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().size()).isEqualTo(1),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getForespørslerSomMotpart().size()).isEqualTo(0),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(0),
        () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK)
    );

    var familieenhet = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst();

    AssertionsForClassTypes.assertThat(familieenhet).isPresent();

    var motpart = familieenhet.get().getMotpart();
    assertAll(
        () -> AssertionsForClassTypes.assertThat(motpart.getFødselsdato()).isEqualTo(LocalDate.now().minusYears(38)),
        () -> AssertionsForClassTypes.assertThat(motpart.getFornavn()).isEqualTo("Streng"),
        () -> AssertionsForClassTypes.assertThat(Krypteringsverktøy.dekryptere(motpart.getIdent())).isEqualTo("11111122222")
    );

    var barnUnder15 = familieenhet.get().getFellesBarnUnder15År();

    AssertionsForClassTypes.assertThat(barnUnder15.size()).isEqualTo(1);

    var barnetSomLever = barnUnder15.stream().findFirst();

    assertAll(
        () -> AssertionsForClassTypes.assertThat(barnetSomLever).isPresent(),
        () -> AssertionsForClassTypes.assertThat(Krypteringsverktøy.dekryptere(barnetSomLever.get().getIdent())).isEqualTo("33333355555"),
        () -> AssertionsForClassTypes.assertThat(barnetSomLever.get().getFornavn()).isEqualTo("Småstein"),
        () -> AssertionsForClassTypes.assertThat(barnetSomLever.get().getFødselsdato()).isEqualTo(LocalDate.now().minusYears(10))
    );
  }
}
