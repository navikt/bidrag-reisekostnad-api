package no.nav.bidrag.reisekostnad.api;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Set;
import no.nav.bidrag.reisekostnad.api.dto.inn.NyForespørselDto;
import no.nav.bidrag.reisekostnad.api.dto.ut.BrukerinformasjonDto;
import no.nav.bidrag.reisekostnad.database.datamodell.Deaktivator;
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
public class TrekkeForespørselTest extends KontrollerTest {

  @BeforeEach
  public void sletteTestdata() {
    barnDao.deleteAll();
    forespørselDao.deleteAll();
    forelderDao.deleteAll();
  }

  @Test
  void hovedpartSkalKunneTrekkeForespørsel() {

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
        () -> assertThat(brukerinformasjonMedAktivForespørsel.getBody().getForespørslerSomHovedpart().size()).isEqualTo(1),
        () -> assertThat(
            brukerinformasjonMedAktivForespørsel.getBody().getForespørslerSomHovedpart().stream().findFirst().get().getId())
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
        () -> assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getFornavn())
            .isEqualTo(påloggetPerson.getFornavn()),
        () -> assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(1),
        () -> assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getMotparterMedFellesBarnUnderFemtenÅr().size())
            .isEqualTo(1),
        () -> assertThat(
            brukerinformasjonMedTrukketForespørsel.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get()
                .getFellesBarnUnder15År()
                .size()).isEqualTo(1));

    var motpart = brukerinformasjonMedTrukketForespørsel.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getMotpart();
    var barnUnder15År = brukerinformasjonMedTrukketForespørsel.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get()
        .getFellesBarnUnder15År()
        .stream().findFirst().get();
    var barnMinst15År = brukerinformasjonMedTrukketForespørsel.getBody().getBarnMinstFemtenÅr().stream().findFirst().get();

    assertAll(
        () -> assertThat(motpart.getFornavn()).isEqualTo(testpersonStreng.getFornavn()),
        () -> assertThat(motpart.getFødselsdato()).isEqualTo(testpersonStreng.getFødselsdato()),
        () -> assertThat(barnUnder15År.getFødselsdato()).isEqualTo(testpersonBarn10.getFødselsdato()),
        () -> assertThat(barnUnder15År.getFornavn()).isEqualTo(testpersonBarn10.getFornavn()),
        () -> assertThat(barnMinst15År.getFødselsdato()).isEqualTo(testpersonBarn16.getFødselsdato()),
        () -> assertThat(barnMinst15År.getFornavn()).isEqualTo(testpersonBarn16.getFornavn()));

    /* ----------- Verifisere lagrede forespørsler  ----------- */
    assertAll(
        () -> assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getForespørslerSomHovedpart().stream().findFirst()).isPresent(),
        () -> assertThat(
            brukerinformasjonMedTrukketForespørsel.getBody().getForespørslerSomHovedpart().stream().findFirst().get().getDeaktivert()).isNotNull()
    );
    var trukketForespørsel = forespørselDao.findById(idLagretForespørsel);
    assertAll(
        () -> assertThat(trukketForespørsel.isPresent()),
        () -> assertThat(trukketForespørsel.get().getDeaktivert()).isNotNull(),
        () -> assertThat(trukketForespørsel.get().getDeaktivertAv()).isEqualTo(Deaktivator.HOVEDPART)
    );
  }

  @Test
  void motpartSkalKunneTrekkeForespørsel() {

    // gitt
    var motpart = testpersonStreng;
    var hovedpart = testpersonGråtass;
    var barnUnder15 = testpersonBarn10;

    var lagretForespørselSomVenterPåSamtykke = lagreForespørselForEttBarn(hovedpart.getIdent(), motpart.getIdent(), barnUnder15.getIdent(), true);

    httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(motpart.getIdent()));
    var b = new OAuth2AccessTokenResponse(generereTesttoken(motpart.getIdent()), 1000, 1000, null);

    when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(b);

    var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
        BrukerinformasjonDto.class);

    assertAll(
        () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(brukerinformasjon.getBody().getForespørslerSomMotpart().size()).isEqualTo(1),
        () -> assertThat(brukerinformasjon.getBody().getForespørslerSomMotpart().stream().filter(f -> !f.isKreverSamtykke()).findFirst()).isEmpty(),
        () -> assertThat(brukerinformasjon.getBody().getForespørslerSomMotpart().stream().filter(f -> f.isKreverSamtykke()).findFirst()).isPresent(),
        () -> assertThat(
            brukerinformasjon.getBody().getForespørslerSomMotpart().stream().filter(f -> f.isKreverSamtykke()).findFirst().get().getId()).isEqualTo(
            lagretForespørselSomVenterPåSamtykke.getId())
    );

    // hvis
    var url = String.format(urlTrekkeForespørsel, lagretForespørselSomVenterPåSamtykke.getId());
    var trukketrespons = httpHeaderTestRestTemplateApi.exchange(url, HttpMethod.PUT, initHttpEntity(null), Void.class);

    // så
    assertThat(trukketrespons.getStatusCode().is2xxSuccessful());

    var brukerinformasjonMedTrukketForespørsel = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
        BrukerinformasjonDto.class);

    assertAll(
        () -> assertThat(brukerinformasjonMedTrukketForespørsel.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getFornavn()).isEqualTo(motpart.getFornavn()),
        () -> assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(1),
        () -> assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getMotparterMedFellesBarnUnderFemtenÅr().size()).isEqualTo(1),
        () -> assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get()
            .getFellesBarnUnder15År()
            .size()).isEqualTo(1),
        () -> assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getForespørslerSomHovedpart().size()).isEqualTo(0),
        () -> assertThat(brukerinformasjonMedTrukketForespørsel.getBody().getForespørslerSomMotpart().size()).isEqualTo(1));

    var trukketForespørsel = brukerinformasjonMedTrukketForespørsel.getBody().getForespørslerSomMotpart().stream().findFirst().get();
    var lagretForespørsel = forespørselDao.findById(lagretForespørselSomVenterPåSamtykke.getId());

    assertAll(
        () -> assertThat(trukketForespørsel.getDeaktivert()).isNotNull(),
        () -> assertThat(trukketForespørsel.getDeaktivertAv()).isEqualTo(Deaktivator.MOTPART),
        () -> assertThat(lagretForespørsel.isPresent()),
        () -> assertThat(lagretForespørsel.get().getDeaktivertAv()).isEqualTo(Deaktivator.MOTPART));
  }

  @Test
  void skalKunneOppretteNyForespørselForBarnMedTrukketForespørsel() {

    // gitt
    hovedpartSkalKunneTrekkeForespørsel();

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
