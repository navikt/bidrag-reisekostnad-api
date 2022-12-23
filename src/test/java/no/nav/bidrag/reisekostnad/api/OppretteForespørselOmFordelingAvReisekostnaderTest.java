package no.nav.bidrag.reisekostnad.api;

import static no.nav.bidrag.reisekostnad.Testperson.testpersonBarn10;
import static no.nav.bidrag.reisekostnad.Testperson.testpersonBarn16;
import static no.nav.bidrag.reisekostnad.Testperson.testpersonGråtass;
import static no.nav.bidrag.reisekostnad.Testperson.testpersonStreng;
import static no.nav.bidrag.reisekostnad.integrasjon.bidrag.doument.BidragDokumentkonsument.BEHANDLINGSTEMA_REISEKOSTNADER;
import static no.nav.bidrag.reisekostnad.integrasjon.bidrag.doument.BidragDokumentkonsument.DOKUMENTTITTEL;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.github.tomakehurst.wiremock.client.WireMock;
import java.time.LocalDate;
import java.util.Set;
import no.nav.bidrag.dokument.dto.JournalpostType;
import no.nav.bidrag.reisekostnad.StubsKt;
import no.nav.bidrag.reisekostnad.api.dto.inn.NyForespørselDto;
import no.nav.bidrag.reisekostnad.api.dto.ut.BrukerinformasjonDto;
import no.nav.bidrag.reisekostnad.api.dto.ut.ForespørselDto;
import no.nav.bidrag.reisekostnad.api.dto.ut.PersonDto;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Krypteringsverktøy;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext
public class OppretteForespørselOmFordelingAvReisekostnaderTest extends KontrollerTest {

  @Test
  void skalOppretteForespørselOmFordelingAvReisekostnaderForEttAvToFellesBarn() {

    // gitt
    var påloggetPerson = testpersonGråtass;
    initTokenForPåloggetPerson(påloggetPerson.getIdent());

    var nyForespørsel = new NyForespørselDto(Set.of(Krypteringsverktøy.kryptere(testpersonBarn10.getIdent())));

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
        () -> AssertionsForClassTypes.assertThat(
            brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
                .size()).isEqualTo(1));

    var motpart = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getMotpart();
    var barnUnder15År = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
        .stream().findFirst().get();
    var barnMinst15År = brukerinformasjon.getBody().getBarnMinstFemtenÅr().stream().findFirst().get();

    assertAll(
        () -> AssertionsForClassTypes.assertThat(motpart.getFornavn()).isEqualTo(testpersonStreng.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(motpart.getFødselsdato()).isEqualTo(testpersonStreng.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(barnUnder15År.getFødselsdato()).isEqualTo(testpersonBarn10.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(barnUnder15År.getFornavn()).isEqualTo(testpersonBarn10.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(barnMinst15År.getFødselsdato()).isEqualTo(testpersonBarn16.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(barnMinst15År.getFornavn()).isEqualTo(testpersonBarn16.getFornavn()));

    /* ----------- Verifisere lagret forespørsel  ----------- */

    AssertionsForClassTypes.assertThat(brukerinformasjon.getBody().getForespørslerSomHovedpart().size()).isEqualTo(1);

    var lagretForespørsel = brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().findFirst();

    assertAll(
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel).isPresent(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getOpprettet()).isNotNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getJournalført()).isNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getSamtykket()).isNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().isKreverSamtykke()).isTrue(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getHovedpart().getIdent()).isEqualTo(Krypteringsverktøy.kryptere(
            testpersonGråtass.getIdent())),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getHovedpart().getFornavn()).isEqualTo(testpersonGråtass.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getHovedpart().getFødselsdato())
            .isEqualTo(testpersonGråtass.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getMotpart().getIdent()).isEqualTo(Krypteringsverktøy.kryptere(
            testpersonStreng.getIdent())),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getMotpart().getFornavn()).isEqualTo(testpersonStreng.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getMotpart().getFødselsdato()).isEqualTo(testpersonStreng.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørsel.get().getBarn().size()).isEqualTo(1)
    );

    var barnILagretForespørsel = lagretForespørsel.get().getBarn().stream().findFirst();

    assertAll(
        () -> AssertionsForClassTypes.assertThat(barnILagretForespørsel).isPresent(),
        () -> AssertionsForClassTypes.assertThat(barnILagretForespørsel.get().getFornavn()).isEqualTo(testpersonBarn10.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(Krypteringsverktøy.dekryptere(barnILagretForespørsel.get().getIdent()))
            .isEqualTo(testpersonBarn10.getIdent()),
        () -> AssertionsForClassTypes.assertThat(barnILagretForespørsel.get().getFødselsdato()).isEqualTo(testpersonBarn10.getFødselsdato())
    );


    var forespørselDO = forespørselDao.henteAktivForespørsel(lagretForespørsel.get().getId());
    var barn10År = forespørselDO.get().getBarn().stream().filter(b->b.getPersonident().equals(testpersonBarn10.getIdent())).findFirst();
    assertThat(barn10År.isPresent()).isTrue();
    assertThat(barn10År.get().getFødselsdato()).isEqualTo(LocalDate.now().minusYears(10));
  }

  @Test
  void skalOppretteForespørselOmFordelingAvReisekostnaderForToAvToFellesBarn() {

    // gitt
    var påloggetPerson = testpersonGråtass;
    initTokenForPåloggetPerson(påloggetPerson.getIdent());

    var nyForespørsel = new NyForespørselDto(
        Set.of(Krypteringsverktøy.kryptere(testpersonBarn16.getIdent()), Krypteringsverktøy.kryptere(testpersonBarn10.getIdent())));

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
        () -> AssertionsForClassTypes.assertThat(
            brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
                .size()).isEqualTo(1));

    var motpart = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getMotpart();
    var barnUnder15År = brukerinformasjon.getBody().getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
        .stream().findFirst().get();
    var barnMinst15År = brukerinformasjon.getBody().getBarnMinstFemtenÅr().stream().findFirst().get();

    assertAll(
        () -> AssertionsForClassTypes.assertThat(motpart.getFornavn()).isEqualTo(testpersonStreng.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(motpart.getFødselsdato()).isEqualTo(testpersonStreng.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(barnUnder15År.getFødselsdato()).isEqualTo(testpersonBarn10.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(barnUnder15År.getFornavn()).isEqualTo(testpersonBarn10.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(barnMinst15År.getFødselsdato()).isEqualTo(testpersonBarn16.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(barnMinst15År.getFornavn()).isEqualTo(testpersonBarn16.getFornavn()));

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
            Krypteringsverktøy.kryptere(testpersonGråtass.getIdent())),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getHovedpart().getFornavn())
            .isEqualTo(testpersonGråtass.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getHovedpart().getFødselsdato())
            .isEqualTo(testpersonGråtass.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getMotpart().getIdent()).isEqualTo(
            Krypteringsverktøy.kryptere(testpersonStreng.getIdent())),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getMotpart().getFornavn())
            .isEqualTo(testpersonStreng.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getMotpart().getFødselsdato())
            .isEqualTo(testpersonStreng.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnUnder15.get().getBarn().size()).isEqualTo(1)
    );

    var barnILagretForespørsel = lagretForespørselBarnUnder15.get().getBarn().stream().findFirst();

    assertAll(
        () -> AssertionsForClassTypes.assertThat(barnILagretForespørsel).isPresent(),
        () -> AssertionsForClassTypes.assertThat(barnILagretForespørsel.get().getFornavn()).isEqualTo(testpersonBarn10.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(Krypteringsverktøy.dekryptere(barnILagretForespørsel.get().getIdent()))
            .isEqualTo(testpersonBarn10.getIdent()),
        () -> AssertionsForClassTypes.assertThat(barnILagretForespørsel.get().getFødselsdato()).isEqualTo(testpersonBarn10.getFødselsdato())
    );

    var lagretForespørselBarnOver15 = brukerinformasjon.getBody().getForespørslerSomHovedpart().stream().filter(f -> f.isKreverSamtykke() == false)
        .findFirst();
    assertAll(
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15).isPresent(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getOpprettet()).isNotNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getJournalført()).isNotNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getSamtykket()).isNull(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().isKreverSamtykke()).isFalse(),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getHovedpart().getIdent()).isEqualTo(
            Krypteringsverktøy.kryptere(testpersonGråtass.getIdent())),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getHovedpart().getFornavn())
            .isEqualTo(testpersonGråtass.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getHovedpart().getFødselsdato())
            .isEqualTo(testpersonGråtass.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getMotpart().getIdent()).isEqualTo(
            Krypteringsverktøy.kryptere(testpersonStreng.getIdent())),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getMotpart().getFornavn())
            .isEqualTo(testpersonStreng.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getMotpart().getFødselsdato())
            .isEqualTo(testpersonStreng.getFødselsdato()),
        () -> AssertionsForClassTypes.assertThat(lagretForespørselBarnOver15.get().getBarn().size()).isEqualTo(1)
    );

    var barnOver15ILagretForespørsel = lagretForespørselBarnOver15.get().getBarn().stream().findFirst();

    assertAll(
        () -> AssertionsForClassTypes.assertThat(barnOver15ILagretForespørsel).isPresent(),
        () -> AssertionsForClassTypes.assertThat(barnOver15ILagretForespørsel.get().getFornavn()).isEqualTo(testpersonBarn16.getFornavn()),
        () -> AssertionsForClassTypes.assertThat(Krypteringsverktøy.dekryptere(barnOver15ILagretForespørsel.get().getIdent())).isEqualTo(
            testpersonBarn16.getIdent()),
        () -> AssertionsForClassTypes.assertThat(barnOver15ILagretForespørsel.get().getFødselsdato()).isEqualTo(testpersonBarn16.getFødselsdato())
    );

    var forespørselDO = forespørselDao.henteAktivForespørsel(lagretForespørselBarnOver15.get().getId());
    var barn16År = forespørselDO.get().getBarn().stream().filter(b->b.getPersonident().equals(testpersonBarn16.getIdent())).findFirst();
    assertThat(barn16År.isPresent()).isTrue();
    assertThat(barn16År.get().getFødselsdato()).isEqualTo(LocalDate.now().minusYears(16));
  }

  @Test
  void skalArkivereDokumentHvisOpprettetSamtykkeMinst15år() {

    // gitt
    var hovedPerson = testpersonGråtass;
    initTokenForPåloggetPerson(hovedPerson.getIdent());

    var nyForespørsel = new NyForespørselDto(
        Set.of(Krypteringsverktøy.kryptere(testpersonBarn16.getIdent()), Krypteringsverktøy.kryptere(testpersonBarn10.getIdent())));

    var responsOpprett = httpHeaderTestRestTemplateApi.exchange(urlNyForespørsel, HttpMethod.POST, initHttpEntity(nyForespørsel), Void.class);

    var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
        BrukerinformasjonDto.class);

    assertAll(
        () -> assertThat(responsOpprett.getStatusCode()).isEqualTo(HttpStatus.CREATED),
        () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK)
    );

    var barnMinst15År = hentBarnMinst15år(brukerinformasjon.getBody());
    var allForespørsler = brukerinformasjon.getBody().getForespørslerSomHovedpart();

    assertThat(allForespørsler.size()).isEqualTo(2);

    var forespørselMedBarnMinst15år = allForespørsler.stream()
        .filter(p -> p.getBarn().stream().anyMatch((b) -> b.getIdent().equals(barnMinst15År.getIdent()))).findFirst().get();

    StubsKt.verifiserDokumentArkivertForForespørsel(forespørselMedBarnMinst15år.getId());

    var arkiveringRequest = hentOpprettDokumentRequestBodyForForespørsel(forespørselMedBarnMinst15år.getId());
    var forespørselEntity = forespørselDao.findById(forespørselMedBarnMinst15år.getId());

    assertAll(
        () -> assertThat(forespørselMedBarnMinst15år.getJournalført()).isNotNull(),
        () -> assertThat(forespørselEntity.get().getIdJournalpost()).isEqualTo(opprettetJournalpostId),
        () -> assertThat(arkiveringRequest.size()).isEqualTo(1),
        () -> assertThat(arkiveringRequest.get(0).getDokumenter().size()).isEqualTo(1),
        () -> assertThat(arkiveringRequest.get(0).getDokumenter().get(0).getFysiskDokument()).isNotEmpty(),
        () -> assertThat(arkiveringRequest.get(0).getDokumenter().get(0).getTittel()).isEqualTo(DOKUMENTTITTEL),
        () -> assertThat(arkiveringRequest.get(0).getGjelderIdent()).isEqualTo(hovedPerson.getIdent()),
        () -> assertThat(arkiveringRequest.get(0).getAvsenderMottaker().getIdent()).isEqualTo(hovedPerson.getIdent()),
        () -> assertThat(arkiveringRequest.get(0).getJournalposttype()).isEqualTo(JournalpostType.INNGÅENDE),
        () -> assertThat(arkiveringRequest.get(0).getBehandlingstema()).isEqualTo(BEHANDLINGSTEMA_REISEKOSTNADER),
        () -> assertThat(arkiveringRequest.get(0).getTittel()).isNull(),
        () -> assertThat(arkiveringRequest.get(0).getReferanseId()).isEqualTo(String.format("REISEKOSTNAD_%s", forespørselMedBarnMinst15år.getId()))
    );
  }

  @Test
  void skalArkivereDokumentVedSamtykkeForBarnUnder15År() {

    // gitt
    var hovedPerson = testpersonGråtass;
    initTokenForPåloggetPerson(hovedPerson.getIdent());

    var nyForespørsel = new NyForespørselDto(Set.of(Krypteringsverktøy.kryptere(testpersonBarn10.getIdent())));

    var responsOpprett = httpHeaderTestRestTemplateApi.exchange(urlNyForespørsel, HttpMethod.POST, initHttpEntity(nyForespørsel), Void.class);

    var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
        BrukerinformasjonDto.class);

    assertAll(
        () -> assertThat(responsOpprett.getStatusCode()).isEqualTo(HttpStatus.CREATED),
        () -> assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK)
    );

    var motpart = hentMotpart(brukerinformasjon.getBody());
    var barnUnder15År = hentBarnUnder15År(brukerinformasjon.getBody());
    var allForespørsler = brukerinformasjon.getBody().getForespørslerSomHovedpart();
    assertThat(allForespørsler.size()).isEqualTo(1);

    var forespørselUnder15årFørSamtykke = hentForespørselForBarn(barnUnder15År.getIdent());
    var forespørselIdUnder15år = forespørselUnder15årFørSamtykke.getId();

    // Motpart samtykker forespørsel for barn under 15
    // Pålogget som motpart
    initTokenForPåloggetPerson(Krypteringsverktøy.dekryptere(motpart.getIdent()));

    var samtykkeRespons = httpHeaderTestRestTemplateApi.exchange(urlSamtykkeForespørsel + "?id=" + forespørselIdUnder15år, HttpMethod.PUT,
        initHttpEntity(null), Void.class);
    assertThat(samtykkeRespons.getStatusCode()).isEqualTo(HttpStatus.OK);

    // Pålogget som hovedperson
    initTokenForPåloggetPerson(hovedPerson.getIdent());

    var forespørselMedBarnUnder15årEtterSamtykke = hentForespørselForBarn(barnUnder15År.getIdent());

    StubsKt.verifiserDokumentArkivertForForespørsel(forespørselIdUnder15år);

    var arkiveringRequest = hentOpprettDokumentRequestBodyForForespørsel(forespørselIdUnder15år);

    var forespørselEntity = forespørselDao.findById(forespørselIdUnder15år);
    assertAll(
        () -> assertThat(forespørselUnder15årFørSamtykke.getJournalført()).isNull(),
        () -> assertThat(forespørselEntity.get().getIdJournalpost()).isEqualTo(opprettetJournalpostId),
        () -> assertThat(forespørselMedBarnUnder15årEtterSamtykke.getJournalført()).isNotNull(),
        () -> assertThat(arkiveringRequest.size()).isEqualTo(1),
        () -> assertThat(arkiveringRequest.get(0).getDokumenter().size()).isEqualTo(1),
        () -> assertThat(arkiveringRequest.get(0).getDokumenter().get(0).getFysiskDokument()).isNotEmpty(),
        () -> assertThat(arkiveringRequest.get(0).getDokumenter().get(0).getTittel()).isEqualTo(DOKUMENTTITTEL),
        () -> assertThat(arkiveringRequest.get(0).getGjelderIdent()).isEqualTo(hovedPerson.getIdent()),
        () -> assertThat(arkiveringRequest.get(0).getAvsenderMottaker().getIdent()).isEqualTo(hovedPerson.getIdent()),
        () -> assertThat(arkiveringRequest.get(0).getJournalposttype()).isEqualTo(JournalpostType.INNGÅENDE),
        () -> assertThat(arkiveringRequest.get(0).getBehandlingstema()).isEqualTo(BEHANDLINGSTEMA_REISEKOSTNADER),
        () -> assertThat(arkiveringRequest.get(0).getTittel()).isNull(),
        () -> assertThat(arkiveringRequest.get(0).getReferanseId()).isEqualTo(String.format("REISEKOSTNAD_%s", forespørselIdUnder15år))
    );

  }

  @Test
  void skalIkkeFeileHvisArkiveringFeiler() {

    // gitt
    var hovedPerson = testpersonGråtass;
    initTokenForPåloggetPerson(hovedPerson.getIdent());

    var nyForespørsel = new NyForespørselDto(
        Set.of(Krypteringsverktøy.kryptere(testpersonBarn16.getIdent()), Krypteringsverktøy.kryptere(testpersonBarn10.getIdent())));

    var stub = stubArkiverDokumentFeiler();
    try {
      var responsOpprett = httpHeaderTestRestTemplateApi.exchange(urlNyForespørsel, HttpMethod.POST, initHttpEntity(nyForespørsel), Void.class);
      assertThat(responsOpprett.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    } finally {
      WireMock.removeStub(stub);
    }

    var brukerinformasjon = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
        BrukerinformasjonDto.class);
    assertThat(brukerinformasjon.getStatusCode()).isEqualTo(HttpStatus.OK);

    var barnMinst15År = hentBarnMinst15år(brukerinformasjon.getBody());
    var allForespørsler = brukerinformasjon.getBody().getForespørslerSomHovedpart();

    assertThat(allForespørsler.size()).isEqualTo(2);

    var forespørselMedBarnMinst15år = allForespørsler.stream()
        .filter(p -> p.getBarn().stream().anyMatch((b) -> b.getIdent().equals(barnMinst15År.getIdent()))).findFirst().get();

    StubsKt.verifiserDokumentArkivertForForespørsel(forespørselMedBarnMinst15år.getId());
    var forespørselEntity = forespørselDao.findById(forespørselMedBarnMinst15år.getId());

    assertThat(forespørselMedBarnMinst15år.getJournalført()).isNull();
    assertThat(forespørselEntity.get().getIdJournalpost()).isNull();
  }


  private PersonDto hentMotpart(BrukerinformasjonDto brukerinformasjon) {
    return brukerinformasjon.getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getMotpart();
  }

  private PersonDto hentBarnMinst15år(BrukerinformasjonDto brukerinformasjon) {
    return brukerinformasjon.getBarnMinstFemtenÅr().stream().findFirst().get();
  }

  private ForespørselDto hentForespørselForBarn(String barnIdent) {
    var brukerinformasjonEtterSamtykke = httpHeaderTestRestTemplateApi.exchange(urlBrukerinformasjon, HttpMethod.GET, initHttpEntity(null),
        BrukerinformasjonDto.class);
    var allForespørslerEtterSamtykke = brukerinformasjonEtterSamtykke.getBody().getForespørslerSomHovedpart();
    return allForespørslerEtterSamtykke.stream().filter(p -> p.getBarn().stream().anyMatch((b) -> b.getIdent().equals(barnIdent))).findFirst().get();
  }

  private PersonDto hentBarnUnder15År(BrukerinformasjonDto brukerinformasjon) {
    return brukerinformasjon.getMotparterMedFellesBarnUnderFemtenÅr().stream().findFirst().get().getFellesBarnUnder15År()
        .stream().findFirst().get();
  }
}
