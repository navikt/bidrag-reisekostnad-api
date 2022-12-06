package no.nav.bidrag.reisekostnad.api;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;
import no.nav.bidrag.reisekostnad.api.dto.inn.NyForespørselDto;
import no.nav.bidrag.reisekostnad.api.dto.ut.BrukerinformasjonDto;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Krypteringsverktøy;
import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
public class TrekkeForespørselTest extends KontrollerTest{

  @BeforeEach
  public void sletteTestdata() {
    barnDao.deleteAll();
    forespørselDao.deleteAll();
    forelderDao.deleteAll();
  }

  @Test
  void skalKunneTrekkeForespørsel() {

    // gitt
    var påloggetPerson = testpersonGråtass;
    httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));

    var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
    when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

    var nyForespørsel = new NyForespørselDto(Set.of(Krypteringsverktøy.kryptere(testpersonBarn10.getIdent())));

    var responsOpprett = httpHeaderTestRestTemplateApi.exchange(urlNyForespørsel, HttpMethod.POST, initHttpEntity(nyForespørsel),
        Void.class);

    assertThat(responsOpprett.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    var brukerinformasjonMedAktivForespørsel = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
        BrukerinformasjonDto.class);

    assertAll(
        () -> assertThat(brukerinformasjonMedAktivForespørsel.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjonMedAktivForespørsel.getBody().getForespørslerSomHovedpart().size()).isEqualTo(1),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjonMedAktivForespørsel.getBody().getForespørslerSomHovedpart().stream().findFirst().get().getId())
    );

    var idLagretForespørsel = brukerinformasjonMedAktivForespørsel.getBody().getForespørslerSomHovedpart().stream().findFirst().get().getId();

    // hvis
    var url = String.format(urlTrekkeForespørsel, idLagretForespørsel);
    var trukketrespons = httpHeaderTestRestTemplateApi.exchange(url, HttpMethod.PUT, initHttpEntity(null),
        Void.class);

    // så
    AssertionsForClassTypes.assertThat(trukketrespons.getStatusCode().is2xxSuccessful());

    var brukerinformasjonMedTrukketForespørsel = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
        BrukerinformasjonDto.class);

    assertAll(
        () -> assertThat(brukerinformasjonMedTrukketForespørsel.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(1),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getMotparterMedFellesBarnUnderFemtenÅr().size()).isEqualTo(1),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get()
            .getFellesBarnUnder15År()
            .size()).isEqualTo(1));

    var motpart = brukerinformasjonMedTrukketForespørsel.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getMotpart();
    var barnUnder15År = brukerinformasjonMedTrukketForespørsel.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get()
        .getFellesBarnUnder15År()
        .stream().findFirst().get();
    var barnMinst15År = brukerinformasjonMedTrukketForespørsel.getBody().getBarnMinstFemtenÅr().stream().findFirst().get();

    assertAll(
        () -> AssertionsForClassTypes.assertThat(motpart.getFornavn()).isEqualTo(testpersonStreng.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(motpart.getFødselsdato()).isEqualTo(testpersonStreng.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(barnUnder15År.getFødselsdato()).isEqualTo(testpersonBarn10.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(barnUnder15År.getFornavn()).isEqualTo(testpersonBarn10.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(barnMinst15År.getFødselsdato()).isEqualTo(testpersonBarn16.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(barnMinst15År.getFornavn()).isEqualTo(testpersonBarn16.getFornavn()));

    /* ----------- Verifisere lagrede forespørsler  ----------- */
    AssertionsForClassTypes.assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getForespørslerSomHovedpart().size()).isEqualTo(0);
    var trukketForespørsel = forespørselDao.findById(idLagretForespørsel);
    assertAll(
        () -> AssertionsForClassTypes.assertThat(trukketForespørsel.isPresent()),
        () -> AssertionsForClassTypes.assertThat(trukketForespørsel.get().getDeaktivert()).isNotNull()
    );
  }

  @Test
  void skalKunneOppretteNyForespørselForBarnMedTrukketForespørsel() {

    // gitt
    skalKunneTrekkeForespørsel();

    var påloggetPerson = testpersonGråtass;
    httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));

    var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
    when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

    var f = forespørselDao.findById(1);

    var nyForespørsel = new NyForespørselDto(Set.of(Krypteringsverktøy.kryptere(testpersonBarn10.getIdent())));

    // hvis
    var responsOpprett = httpHeaderTestRestTemplateApi.exchange(urlNyForespørsel, HttpMethod.POST, initHttpEntity(nyForespørsel),
        Void.class);

    // så
    assertThat(responsOpprett.getStatusCode()).isEqualTo(HttpStatus.CREATED);

  }
}
