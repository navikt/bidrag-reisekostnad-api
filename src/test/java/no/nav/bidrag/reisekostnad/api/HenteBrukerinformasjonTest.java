package no.nav.bidrag.reisekostnad.api;

import static no.nav.bidrag.reisekostnad.Testperson.testpersonBarn10;
import static no.nav.bidrag.reisekostnad.Testperson.testpersonBarn16;
import static no.nav.bidrag.reisekostnad.Testperson.testpersonDødMotpart;
import static no.nav.bidrag.reisekostnad.Testperson.testpersonErDød;
import static no.nav.bidrag.reisekostnad.Testperson.testpersonGråtass;
import static no.nav.bidrag.reisekostnad.Testperson.testpersonHarBarnMedDiskresjon;
import static no.nav.bidrag.reisekostnad.Testperson.testpersonHarDiskresjon;
import static no.nav.bidrag.reisekostnad.Testperson.testpersonHarDødtBarn;
import static no.nav.bidrag.reisekostnad.Testperson.testpersonHarMotpartMedDiskresjon;
import static no.nav.bidrag.reisekostnad.Testperson.testpersonIkkeFunnet;
import static no.nav.bidrag.reisekostnad.Testperson.testpersonServerfeil;
import static no.nav.bidrag.reisekostnad.Testperson.testpersonStreng;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import no.nav.bidrag.reisekostnad.api.dto.inn.NyForespørselDto;
import no.nav.bidrag.reisekostnad.api.dto.ut.BrukerinformasjonDto;
import no.nav.bidrag.reisekostnad.database.datamodell.Deaktivator;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.Kjønn;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Krypteringsverktøy;
import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
public class HenteBrukerinformasjonTest extends KontrollerTest {

  private void sletteData() {
    barnDao.deleteAll();
    forespørselDao.deleteAll();
    forelderDao.deleteAll();
  }

  @Test
  void skalHenteBrukerinformasjonForHovedpartMedFamilierelasjoner() {

    // gitt
    var påloggetPerson = testpersonGråtass;
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
        () -> assertThat(
            brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
                .size()).isEqualTo(1));

    var motpart = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getMotpart();
    var barnUnder15År = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
        .stream().findFirst().get();
    var barnMinst15År = brukerinformasjon.getBody().getBarnMinstFemtenÅr().stream().findFirst().get();

    assertAll(
        () -> assertThat(motpart.getFornavn()).isEqualTo(testpersonStreng.getFornavn()),
        () -> assertThat(motpart.getFødselsdato()).isEqualTo(testpersonStreng.getFødselsdato()),
        () -> assertThat(barnUnder15År.getFødselsdato()).isEqualTo(testpersonBarn10.getFødselsdato()),
        () -> assertThat(barnUnder15År.getFornavn()).isEqualTo(testpersonBarn10.getFornavn()),
        () -> assertThat(barnMinst15År.getFødselsdato()).isEqualTo(testpersonBarn16.getFødselsdato()),
        () -> assertThat(barnMinst15År.getFornavn()).isEqualTo(testpersonBarn16.getFornavn()));
  }

  @Test
  void skalHenteBrukerinformasjonForHovedpartMedDiskresjon() {

    // gitt
    var påloggetPerson = testpersonHarDiskresjon;
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
  void skalViseForespørslerSomHarBlittDeaktivertInnenGyldighetsperioden() {

    // gitt
    var påloggetPerson = testpersonGråtass;
    httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));

    var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
    when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

    var nyForespørsel = new NyForespørselDto(
        Set.of(Krypteringsverktøy.kryptere(testpersonBarn16.getIdent()), Krypteringsverktøy.kryptere(testpersonBarn10.getIdent())));

    httpHeaderTestRestTemplateApi.exchange(urlNyForespørsel, HttpMethod.POST, initHttpEntity(nyForespørsel), Void.class);

    var brukerinformasjonMedAktivForespørsel = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
        BrukerinformasjonDto.class);

    var idLagretForespørsel = brukerinformasjonMedAktivForespørsel.getBody().getForespørslerSomHovedpart().stream().findFirst().get().getId();

    var url = String.format(urlTrekkeForespørsel, idLagretForespørsel);
    httpHeaderTestRestTemplateApi.exchange(url, HttpMethod.PUT, initHttpEntity(null), Void.class);

    // hvis
    var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
        BrukerinformasjonDto.class);

    // så
    // Forespørselen har nå blitt deaktivert
    assertAll(
        () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(
            brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().filter(f -> f.getDeaktivert() != null).findFirst()).isPresent()
    );

    var deaktivertForespørsel = forespørselDao.findById(idLagretForespørsel);

    assertAll(
        () -> assertThat(deaktivertForespørsel).isPresent(),
        () -> assertThat(deaktivertForespørsel.get().getDeaktivert().toLocalDate()).isEqualTo(LocalDate.now())
    );

    deaktivertForespørsel.get()
        .setDeaktivert(LocalDate.now().minusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING).atStartOfDay());

    // Endrer tidspunkt for deaktivering til maks antall dager bakover i tid innen for gyldighetsintervallet
    forespørselDao.save(deaktivertForespørsel.get());

    var brukerinformasjonForespørselDeaktivertI30Dager = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET,
        initHttpEntity(null),
        BrukerinformasjonDto.class);

    assertAll(
        () -> assertThat(brukerinformasjonForespørselDeaktivertI30Dager.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(
            brukerinformasjonForespørselDeaktivertI30Dager.getBody().getForespørslerSomHovedpart().stream().filter(f -> f.getDeaktivert() != null)
                .findFirst()).isPresent()
    );

    deaktivertForespørsel.get().setDeaktivert(LocalDate.now().minusDays(1).atStartOfDay());

    // Endrer tidpunkt for deaktivering til utenfor gyldighetsintervallet
    forespørselDao.save(deaktivertForespørsel.get());

    var brukerinformasjonForespørselDeaktivertI31Dager = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET,
        initHttpEntity(null),
        BrukerinformasjonDto.class);

    // Den deaktiverte førespørselen viser ikke lengre i brukeroversikten
    assertAll(
        () -> assertThat(brukerinformasjonForespørselDeaktivertI31Dager.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(
            brukerinformasjonForespørselDeaktivertI31Dager.getBody().getForespørslerSomHovedpart().stream().filter(f -> f.getDeaktivert() != null)
                .findFirst()).isPresent()
    );

    sletteData();
  }

  @Test
  void skalIkkeViseJournalførteForespørslerSomHarBlittDeaktivert() {

    // gitt
    var påloggetPerson = testpersonGråtass;
    httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));

    var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
    when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

    var nyForespørsel = new NyForespørselDto(
        Set.of(Krypteringsverktøy.kryptere(testpersonBarn16.getIdent()), Krypteringsverktøy.kryptere(testpersonBarn10.getIdent())));

    httpHeaderTestRestTemplateApi.exchange(urlNyForespørsel, HttpMethod.POST, initHttpEntity(nyForespørsel), Void.class);
    var lagredeForespørsler = forespørselDao.henteForespørslerForHovedpart(påloggetPerson.getIdent());

    assertThat(lagredeForespørsler.size()).isEqualTo(2);
    var journalførtOgDeaktivertForespørsel = lagredeForespørsler.stream().findFirst().get();
    journalførtOgDeaktivertForespørsel.setJournalført(LocalDateTime.now());
    journalførtOgDeaktivertForespørsel.setDeaktivert(LocalDateTime.now());
    journalførtOgDeaktivertForespørsel.setDeaktivertAv(Deaktivator.SYSTEM);

    var journalførtForespørsel = lagredeForespørsler.stream().filter(f -> f.getId() != journalførtOgDeaktivertForespørsel.getId()).findFirst().get();
    journalførtForespørsel.setJournalført(LocalDateTime.now());

    Set.of(journalførtOgDeaktivertForespørsel, journalførtForespørsel).forEach(f -> forespørselDao.save(f));

    // hvis
    var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
        BrukerinformasjonDto.class);

    // så
    assertAll(
        () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(
            brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().filter(f -> f.getId() == journalførtOgDeaktivertForespørsel.getId()).findFirst()).isEmpty(),
        () -> assertThat(
            brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().filter(f -> f.getId() == journalførtForespørsel.getId()).findFirst()).isPresent()
    );

    sletteData();
  }

  @Test
  void skalAngiFristForSamtykkeForForespørslerSomKreverSamtykke() {

    // gitt
    var påloggetPerson = testpersonGråtass;
    httpHeaderTestRestTemplateApi.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken(påloggetPerson.getIdent()));

    var a = new OAuth2AccessTokenResponse(generereTesttoken(påloggetPerson.getIdent()), 1000, 1000, null);
    when(oAuth2AccessTokenService.getAccessToken(any(ClientProperties.class))).thenReturn(a);

    var nyForespørsel = new NyForespørselDto(
        Set.of(Krypteringsverktøy.kryptere(testpersonBarn16.getIdent()), Krypteringsverktøy.kryptere(testpersonBarn10.getIdent())));

    httpHeaderTestRestTemplateApi.exchange(urlNyForespørsel, HttpMethod.POST, initHttpEntity(nyForespørsel), Void.class);

    // hvis
    var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
        BrukerinformasjonDto.class);

    // så
    assertAll(
        () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK),
        () -> assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(2),
        () -> assertThat(
            brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().filter(f -> f.isKreverSamtykke()).findFirst()).isPresent(),
        () -> assertThat(
            brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().filter(f -> !f.isKreverSamtykke()).findFirst()).isPresent()
    );

    var forespørselBarnUnder15 = brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().filter(f -> f.isKreverSamtykke()).findFirst();
    var forespørselBarnOver15 = brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().filter(f -> !f.isKreverSamtykke()).findFirst();

    assertAll(
        () -> assertThat(forespørselBarnUnder15.get().getSamtykkefrist()).isEqualTo(
            LocalDate.now().plusDays(FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING)),
        () -> assertThat(forespørselBarnOver15.get().getSamtykkefrist()).isNull()
    );
  }

  @Test
  void skalGiStatuskode404DersomPersondataMangler() {

    // gitt
    var påloggetPerson = testpersonIkkeFunnet;
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
    var påloggetPerson = testpersonServerfeil;
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
    var påloggetPerson = testpersonHarMotpartMedDiskresjon;
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
    var påloggetPerson = testpersonHarBarnMedDiskresjon;
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
    var påloggetPerson = testpersonErDød;
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
    var påloggetPerson = testpersonDødMotpart;
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
    var påloggetPerson = testpersonHarDødtBarn;
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
