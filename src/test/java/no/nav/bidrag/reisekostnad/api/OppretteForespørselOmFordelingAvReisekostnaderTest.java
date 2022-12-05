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
public class OppretteForespørselOmFordelingAvReisekostnaderTest extends KontrollerTest{

  @BeforeEach
  public void sletteTestdata() {
    barnDao.deleteAll();
    forespørselDao.deleteAll();
    forelderDao.deleteAll();
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
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(1),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().size()).isEqualTo(1),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
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

    /* ----------- Verifisere lagret forespørsel  ----------- */

    AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(1);

    var lagretForespørsel = brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().findFirst();

    assertAll(
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel).isPresent(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getOpprettet()).isNotNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getJournalført()).isNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getSamtykket()).isNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().isKreverSamtykke()).isTrue(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getHovedpart().getIdent()).isEqualTo(Krypteringsverktøy.kryptere(kontrollertestpersonGråtass.getIdent())),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getHovedpart().getFornavn()).isEqualTo(kontrollertestpersonGråtass.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getHovedpart().getFødselsdato()).isEqualTo(kontrollertestpersonGråtass.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getMotpart().getIdent()).isEqualTo(Krypteringsverktøy.kryptere(kontrollertestpersonStreng.getIdent())),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getMotpart().getFornavn()).isEqualTo(kontrollertestpersonStreng.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getMotpart().getFødselsdato()).isEqualTo(kontrollertestpersonStreng.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getBarn().size()).isEqualTo(1)
    );

    var barnILagretForespørsel = lagretForespørsel.get().getBarn().stream().findFirst();

    assertAll(
        () -> AssertionsForClassTypes.assertThat(barnILagretForespørsel).isPresent(),
        () -> AssertionsForClassTypes.assertThat(barnILagretForespørsel.get().getFornavn()).isEqualTo(kontrollertestpersonBarn10.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(Krypteringsverktøy.dekryptere(barnILagretForespørsel.get().getIdent())).isEqualTo(kontrollertestpersonBarn10.getIdent()),
        () -> AssertionsForClassTypes.assertThat(barnILagretForespørsel.get().getFødselsdato()).isEqualTo(kontrollertestpersonBarn10.getFødselsdato())
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
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getFornavn()).isEqualTo(påloggetPerson.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getBarnMinstFemtenÅr().size()).isEqualTo(1),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().size()).isEqualTo(1),
        () -> AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
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

    /* ----------- Verifisere lagrede forespørsler  ----------- */
    AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(2);

    var lagretForespørselBarnUnder15 = brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().filter(f -> f.isKreverSamtykke() == true)
        .findFirst();

    assertAll(
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15).isPresent(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getOpprettet()).isNotNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getJournalført()).isNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getSamtykket()).isNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().isKreverSamtykke()).isTrue(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getHovedpart().getIdent()).isEqualTo(
            Krypteringsverktøy.kryptere(kontrollertestpersonGråtass.getIdent())),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getHovedpart().getFornavn()).isEqualTo(kontrollertestpersonGråtass.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getHovedpart().getFødselsdato()).isEqualTo(kontrollertestpersonGråtass.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getMotpart().getIdent()).isEqualTo(
            Krypteringsverktøy.kryptere(kontrollertestpersonStreng.getIdent())),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getMotpart().getFornavn()).isEqualTo(kontrollertestpersonStreng.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getMotpart().getFødselsdato()).isEqualTo(kontrollertestpersonStreng.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getBarn().size()).isEqualTo(1)
    );

    var barnILagretForespørsel = lagretForespørselBarnUnder15.get().getBarn().stream().findFirst();

    assertAll(
        () -> AssertionsForClassTypes.assertThat(barnILagretForespørsel).isPresent(),
        () -> AssertionsForClassTypes.assertThat(barnILagretForespørsel.get().getFornavn()).isEqualTo(kontrollertestpersonBarn10.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(Krypteringsverktøy.dekryptere(barnILagretForespørsel.get().getIdent())).isEqualTo(kontrollertestpersonBarn10.getIdent()),
        () -> AssertionsForClassTypes.assertThat(barnILagretForespørsel.get().getFødselsdato()).isEqualTo(kontrollertestpersonBarn10.getFødselsdato())
    );

    var lagretForespørselBarnOver15 = brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().filter(f -> f.isKreverSamtykke() == false)
        .findFirst();
    assertAll(
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15).isPresent(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getOpprettet()).isNotNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getJournalført()).isNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getSamtykket()).isNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().isKreverSamtykke()).isFalse(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getHovedpart().getIdent()).isEqualTo(
            Krypteringsverktøy.kryptere(kontrollertestpersonGråtass.getIdent())),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getHovedpart().getFornavn()).isEqualTo(kontrollertestpersonGråtass.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getHovedpart().getFødselsdato()).isEqualTo(kontrollertestpersonGråtass.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getMotpart().getIdent()).isEqualTo(
            Krypteringsverktøy.kryptere(kontrollertestpersonStreng.getIdent())),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getMotpart().getFornavn()).isEqualTo(kontrollertestpersonStreng.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getMotpart().getFødselsdato()).isEqualTo(kontrollertestpersonStreng.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getBarn().size()).isEqualTo(1)
    );

    var barnOver15ILagretForespørsel = lagretForespørselBarnOver15.get().getBarn().stream().findFirst();

    assertAll(
        () -> AssertionsForClassTypes.assertThat(barnOver15ILagretForespørsel).isPresent(),
        () -> AssertionsForClassTypes.assertThat(barnOver15ILagretForespørsel.get().getFornavn()).isEqualTo(kontrollertestpersonBarn16.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(Krypteringsverktøy.dekryptere(barnOver15ILagretForespørsel.get().getIdent())).isEqualTo(kontrollertestpersonBarn16.getIdent()),
        () -> AssertionsForClassTypes.assertThat(barnOver15ILagretForespørsel.get().getFødselsdato()).isEqualTo(kontrollertestpersonBarn16.getFødselsdato())
    );
  }
}
