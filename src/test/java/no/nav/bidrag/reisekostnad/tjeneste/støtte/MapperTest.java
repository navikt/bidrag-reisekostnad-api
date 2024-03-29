package no.nav.bidrag.reisekostnad.tjeneste.støtte;

import no.nav.bidrag.reisekostnad.BidragReisekostnadApiTestapplikasjon;
import no.nav.bidrag.reisekostnad.Testperson;
import no.nav.bidrag.reisekostnad.database.dao.BarnDao;
import no.nav.bidrag.reisekostnad.database.dao.ForelderDao;
import no.nav.bidrag.reisekostnad.database.dao.ForespørselDao;
import no.nav.bidrag.reisekostnad.database.datamodell.Barn;
import no.nav.bidrag.reisekostnad.database.datamodell.Forelder;
import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.BidragPersonkonsument;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.Familiemedlem;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.HentFamilieRespons;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.HentPersoninfoRespons;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.MotpartBarnRelasjon;
import no.nav.bidrag.reisekostnad.konfigurasjon.Profil;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.MotpartBarnRelasjon.Relasjon.FAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

@DisplayName("MapperTest")
@ActiveProfiles(Profil.TEST)
@EnableMockOAuth2Server
@AutoConfigureWireMock(port = 0)
@AutoConfigureTestDatabase(replace = Replace.ANY)
@SpringBootTest(classes = {Mapper.class, ForespørselDao.class, BidragReisekostnadApiTestapplikasjon.class})
public class MapperTest {

    private @Autowired Mapper mapper;

    private @Autowired BarnDao barnDao;
    private @Autowired ForelderDao forelderDao;
    private @Autowired ForespørselDao forespørselDao;

    private @MockBean BidragPersonkonsument bidragPersonkonsument;

    private @MockBean TokenValidationContextHolder tokenValidationContextHolder;

    private static final Testperson HOVEDPART = new Testperson("10001", "Pegasus", 35);
    private static final Testperson MOTPART = new Testperson("11111", "Zuez", 43);
    private static final Testperson BARN_OVER_FEMTEN = new Testperson("12345", "Alfa", 16);
    private static final Testperson BARN_UNDER_FEMTEN = new Testperson("44444", "Beta", 12);
    private static final Testperson BARN_OVER_ATTEN = new Testperson("00000", "Myndig", 18);

    @BeforeEach
    void sletteTestdata() {
        barnDao.deleteAll();
        forelderDao.deleteAll();
        forespørselDao.deleteAll();
        ;
    }

    @Test
    void testeMappingFraHentFamilieResponsTilBrukerinformasjonDtoSomHovedpart() {

        // gitt
        var familierespons = oppretteHentFamilieRespons(HOVEDPART, MOTPART);

        forespørselDao.save(oppretteForespørsel(familierespons.getPerson().getIdent(),
                familierespons.getPersonensMotpartBarnRelasjon().get(0).getMotpart().getIdent(),
                familierespons.getPersonensMotpartBarnRelasjon().stream()
                        .flatMap(mbr -> mbr.getFellesBarn().stream())
                        .filter(barn -> barn.getFoedselsdato().isAfter(LocalDate.now().minusYears(18)))
                        .map(familiemedlem -> Barn.builder().personident(familiemedlem.getIdent()).build()).collect(Collectors.toSet())));

        bidragPersonkonsumentTestrespons();

        // hvis
        var brukerinformasjonDto = mapper.tilDto(familierespons);

        // så
        var barnMinstFemtenÅr = brukerinformasjonDto.getBarnMinstFemtenÅr();
        var motpartOgBarnUnderFemtenÅr = brukerinformasjonDto.getMotparterMedFellesBarnUnderFemtenÅr();
        var forespørslerSomHovedpart = brukerinformasjonDto.getForespørslerSomHovedpart();

        assertAll(
                () -> assertThat(brukerinformasjonDto.getFornavn()).isEqualTo(HOVEDPART.getFornavn()),
                () -> assertThat(brukerinformasjonDto.isKanSøkeOmFordelingAvReisekostnader()).isTrue(),
                () -> assertThat(barnMinstFemtenÅr.size()).isEqualTo(1),
                () -> assertThat(barnMinstFemtenÅr.stream().findFirst().get().getFornavn()).isEqualTo(BARN_OVER_FEMTEN.getFornavn()),
                () -> assertThat(barnMinstFemtenÅr.stream().findFirst().get().getFornavn()).isEqualTo(BARN_OVER_FEMTEN.getFornavn()),
                () -> assertThat(barnMinstFemtenÅr.stream().findFirst().get().getFødselsdato()).isEqualTo(BARN_OVER_FEMTEN.getFødselsdato()),
                () -> assertThat(motpartOgBarnUnderFemtenÅr.size()).isEqualTo(1),
                () -> assertThat(motpartOgBarnUnderFemtenÅr.stream().findFirst().get().getMotpart().getFornavn()).isEqualTo(MOTPART.getFornavn()),
                () -> assertThat(motpartOgBarnUnderFemtenÅr.stream().findFirst().get().getMotpart().getFødselsdato()).isEqualTo(MOTPART.getFødselsdato()),
                () -> assertThat(
                        motpartOgBarnUnderFemtenÅr.stream().findFirst().get().getFellesBarnUnder15År().stream().findFirst().get().getFornavn()).isEqualTo(
                        BARN_UNDER_FEMTEN.getFornavn()),
                () -> assertThat(
                        motpartOgBarnUnderFemtenÅr.stream().findFirst().get().getFellesBarnUnder15År().stream().findFirst().get().getFødselsdato()).isEqualTo(
                        BARN_UNDER_FEMTEN.getFødselsdato()),
                () -> assertThat(forespørslerSomHovedpart).isNotEmpty(),
                () -> assertThat(forespørslerSomHovedpart.size()).isEqualTo(1),
                () -> assertThat(forespørslerSomHovedpart.stream().findFirst().get().getHovedpart().getFødselsdato()).isEqualTo(HOVEDPART.getFødselsdato()),
                () -> assertThat(forespørslerSomHovedpart.stream().findFirst().get().getHovedpart().getFornavn()).isEqualTo(HOVEDPART.getFornavn()),
                () -> assertThat(forespørslerSomHovedpart.stream().findFirst().get().getMotpart().getFødselsdato()).isEqualTo(MOTPART.getFødselsdato()),
                () -> assertThat(forespørslerSomHovedpart.stream().findFirst().get().getMotpart().getFornavn()).isEqualTo(MOTPART.getFornavn()),
                () -> assertThat(forespørslerSomHovedpart.stream().findFirst().get().getBarn().size()).isEqualTo(2),
                () -> assertThat(forespørslerSomHovedpart.stream().findFirst().get()
                        .getBarn().stream().filter(b -> b.getFornavn().equals(BARN_OVER_FEMTEN.getFornavn())).findFirst().get().getFødselsdato()
                        .isEqual(BARN_OVER_FEMTEN.getFødselsdato())),
                () -> assertThat(forespørslerSomHovedpart.stream().findFirst().get()
                        .getBarn().stream().filter(b -> b.getFornavn().equals(BARN_UNDER_FEMTEN.getFornavn())).findFirst().get().getFødselsdato()
                        .isEqual(BARN_UNDER_FEMTEN.getFødselsdato())),
                () -> assertThat(brukerinformasjonDto.getForespørslerSomMotpart().size()).isEqualTo(0)
        );
    }

    @Test
    void testeMappingFraHentFamilieResponsTilBrukerinformasjonDtoMedSøknaderSomMotpart() {

        // gitt
        var testperson = HOVEDPART;
        var denAndreParten = MOTPART;
        var familierespons = oppretteHentFamilieRespons(testperson, denAndreParten);

        forespørselDao.save(oppretteForespørsel(denAndreParten.getIdent(),
                testperson.getIdent(),
                familierespons.getPersonensMotpartBarnRelasjon().stream()
                        .flatMap(mbr -> mbr.getFellesBarn().stream())
                        .filter(barn -> barn.getFoedselsdato().isAfter(LocalDate.now().minusYears(18)))
                        .map(familiemedlem -> Barn.builder().personident(familiemedlem.getIdent()).build()).collect(Collectors.toSet())));

        bidragPersonkonsumentTestrespons();

        // hvis
        var brukerinformasjonDto = mapper.tilDto(familierespons);

        // så
        var barnMinstFemtenÅr = brukerinformasjonDto.getBarnMinstFemtenÅr();
        var motpartOgBarnUnderFemtenÅr = brukerinformasjonDto.getMotparterMedFellesBarnUnderFemtenÅr();
        var forespørslerSomMotpart = brukerinformasjonDto.getForespørslerSomMotpart();

        assertAll(
                () -> assertThat(brukerinformasjonDto.getFornavn()).isEqualTo(testperson.getFornavn()),
                () -> assertThat(brukerinformasjonDto.isKanSøkeOmFordelingAvReisekostnader()).isTrue(),
                () -> assertThat(barnMinstFemtenÅr.size()).isEqualTo(1),
                () -> assertThat(barnMinstFemtenÅr.stream().findFirst().get().getFornavn()).isEqualTo(BARN_OVER_FEMTEN.getFornavn()),
                () -> assertThat(barnMinstFemtenÅr.stream().findFirst().get().getFornavn()).isEqualTo(BARN_OVER_FEMTEN.getFornavn()),
                () -> assertThat(barnMinstFemtenÅr.stream().findFirst().get().getFødselsdato()).isEqualTo(BARN_OVER_FEMTEN.getFødselsdato()),
                () -> assertThat(motpartOgBarnUnderFemtenÅr.size()).isEqualTo(1),
                () -> assertThat(motpartOgBarnUnderFemtenÅr.stream().findFirst().get().getMotpart().getFornavn()).isEqualTo(denAndreParten.getFornavn()),
                () -> assertThat(motpartOgBarnUnderFemtenÅr.stream().findFirst().get().getMotpart().getFødselsdato()).isEqualTo(
                        denAndreParten.getFødselsdato()),
                () -> assertThat(
                        motpartOgBarnUnderFemtenÅr.stream().findFirst().get().getFellesBarnUnder15År().stream().findFirst().get().getFornavn()).isEqualTo(
                        BARN_UNDER_FEMTEN.getFornavn()),
                () -> assertThat(
                        motpartOgBarnUnderFemtenÅr.stream().findFirst().get().getFellesBarnUnder15År().stream().findFirst().get().getFødselsdato()).isEqualTo(
                        BARN_UNDER_FEMTEN.getFødselsdato()),
                () -> assertThat(forespørslerSomMotpart).isNotEmpty(),
                () -> assertThat(forespørslerSomMotpart.size()).isEqualTo(1),
                () -> assertThat(forespørslerSomMotpart.stream().findFirst().get().getHovedpart().getFødselsdato()).isEqualTo(
                        denAndreParten.getFødselsdato()),
                () -> assertThat(forespørslerSomMotpart.stream().findFirst().get().getHovedpart().getFornavn()).isEqualTo(denAndreParten.getFornavn()),
                () -> assertThat(forespørslerSomMotpart.stream().findFirst().get().getMotpart().getFødselsdato()).isEqualTo(testperson.getFødselsdato()),
                () -> assertThat(forespørslerSomMotpart.stream().findFirst().get().getMotpart().getFornavn()).isEqualTo(testperson.getFornavn()),
                () -> assertThat(forespørslerSomMotpart.stream().findFirst().get().getBarn().size()).isEqualTo(2),
                () -> assertThat(forespørslerSomMotpart.stream().findFirst().get()
                        .getBarn().stream().filter(b -> b.getFornavn().equals(BARN_OVER_FEMTEN.getFornavn())).findFirst().get().getFødselsdato()
                        .isEqual(BARN_OVER_FEMTEN.getFødselsdato())),
                () -> assertThat(forespørslerSomMotpart.stream().findFirst().get()
                        .getBarn().stream().filter(b -> b.getFornavn().equals(BARN_UNDER_FEMTEN.getFornavn())).findFirst().get().getFødselsdato()
                        .isEqual(BARN_UNDER_FEMTEN.getFødselsdato())),
                () -> assertThat(brukerinformasjonDto.getForespørslerSomHovedpart().size()).isEqualTo(0)
        );
    }

    @Test
    void skalIkkeInkludereBarnOver18År() {

        // gitt
        var testperson = HOVEDPART;
        var denAndreParten = MOTPART;
        var familierespons = oppretteHentFamilieRespons(testperson, denAndreParten);

        bidragPersonkonsumentTestrespons();

        // hvis
        var brukerinformasjonDto = mapper.tilDto(familierespons);

        // så
        var barnMinstFemtenÅr = brukerinformasjonDto.getBarnMinstFemtenÅr();
        var motpartOgBarnUnderFemtenÅr = brukerinformasjonDto.getMotparterMedFellesBarnUnderFemtenÅr();

        assertAll(
                () -> assertThat(familierespons.getPersonensMotpartBarnRelasjon().stream().flatMap(m -> m.getFellesBarn().stream())
                        .filter(b -> b.getFoedselsdato().isEqual(BARN_OVER_ATTEN.getFødselsdato())).findFirst().isPresent()),
                () -> assertThat(brukerinformasjonDto.getFornavn()).isEqualTo(testperson.getFornavn()),
                () -> assertThat(brukerinformasjonDto.isKanSøkeOmFordelingAvReisekostnader()).isTrue(),
                () -> assertThat(barnMinstFemtenÅr.size()).isEqualTo(1),
                () -> assertThat(barnMinstFemtenÅr.stream().findFirst().get().getFornavn()).isEqualTo(BARN_OVER_FEMTEN.getFornavn()),
                () -> assertThat(barnMinstFemtenÅr.stream().findFirst().get().getFornavn()).isEqualTo(BARN_OVER_FEMTEN.getFornavn()),
                () -> assertThat(barnMinstFemtenÅr.stream().findFirst().get().getFødselsdato()).isEqualTo(BARN_OVER_FEMTEN.getFødselsdato()),
                () -> assertThat(motpartOgBarnUnderFemtenÅr.size()).isEqualTo(1),
                () -> assertThat(motpartOgBarnUnderFemtenÅr.stream().findFirst().get().getMotpart().getFornavn()).isEqualTo(denAndreParten.getFornavn()),
                () -> assertThat(motpartOgBarnUnderFemtenÅr.stream().findFirst().get().getMotpart().getFødselsdato()).isEqualTo(
                        denAndreParten.getFødselsdato()),
                () -> assertThat(
                        motpartOgBarnUnderFemtenÅr.stream().findFirst().get().getFellesBarnUnder15År().stream().findFirst().get().getFornavn()).isEqualTo(
                        BARN_UNDER_FEMTEN.getFornavn()),
                () -> assertThat(
                        motpartOgBarnUnderFemtenÅr.stream().findFirst().get().getFellesBarnUnder15År().stream().findFirst().get().getFødselsdato()).isEqualTo(
                        BARN_UNDER_FEMTEN.getFødselsdato()),
                () -> assertThat(brukerinformasjonDto.getForespørslerSomHovedpart().size()).isEqualTo(0)
        );
    }

    @Test
    void skalIkkeInkludereBarnDersomMotpartManglerPersonident() {

        // gitt
        var testperson = HOVEDPART;
        var ukjentMotpart = new Testperson(null, null, 50);
        var familierespons = oppretteHentFamilieRespons(testperson, ukjentMotpart);

        bidragPersonkonsumentTestrespons();

        // hvis
        var brukerinformasjonDto = mapper.tilDto(familierespons);

        // så
        var barnMinstFemtenÅr = brukerinformasjonDto.getBarnMinstFemtenÅr();
        var motpartOgBarnUnderFemtenÅr = brukerinformasjonDto.getMotparterMedFellesBarnUnderFemtenÅr();

        assertAll(
                () -> assertThat(familierespons.getPersonensMotpartBarnRelasjon().stream().flatMap(m -> m.getFellesBarn().stream())
                        .filter(b -> b.getFoedselsdato().isEqual(BARN_OVER_ATTEN.getFødselsdato())).findFirst().isPresent()),
                () -> assertThat(brukerinformasjonDto.getFornavn()).isEqualTo(testperson.getFornavn()),
                () -> assertThat(brukerinformasjonDto.isKanSøkeOmFordelingAvReisekostnader()).isFalse(),
                () -> assertThat(barnMinstFemtenÅr.size()).isEqualTo(0),
                () -> assertThat(motpartOgBarnUnderFemtenÅr.size()).isEqualTo(0),
                () -> assertThat(brukerinformasjonDto.getForespørslerSomHovedpart().size()).isEqualTo(0)
        );
    }

  @Test
  void skalInkludereBarnekullMedKjentMotpart() {

    // gitt
    var testperson = HOVEDPART;
    var kjentMotpart = MOTPART;
    var ukjentMotpart = new Testperson(null, null, 50);
    var familierespons = oppretteHentFamilieRespons(testperson, List.of(oppretteMotpartBarnRelasjon(kjentMotpart), oppretteMotpartBarnRelasjon(ukjentMotpart)));

    bidragPersonkonsumentTestrespons();

    // hvis
    var brukerinformasjonDto = mapper.tilDto(familierespons);

    // så
    var barnMinstFemtenÅr = brukerinformasjonDto.getBarnMinstFemtenÅr();
    var motpartOgBarnUnderFemtenÅr = brukerinformasjonDto.getMotparterMedFellesBarnUnderFemtenÅr();

    assertAll(
            () -> assertThat(familierespons.getPersonensMotpartBarnRelasjon().stream().flatMap(m -> m.getFellesBarn().stream())
                    .filter(b -> b.getFoedselsdato().isEqual(BARN_OVER_ATTEN.getFødselsdato())).findFirst().isPresent()),
            () -> assertThat(brukerinformasjonDto.getFornavn()).isEqualTo(testperson.getFornavn()),
            () -> assertThat(brukerinformasjonDto.isKanSøkeOmFordelingAvReisekostnader()).isTrue(),
            () -> assertThat(barnMinstFemtenÅr.size()).isEqualTo(1),
            () -> assertThat(motpartOgBarnUnderFemtenÅr.size()).isEqualTo(1),
            () -> assertThat(brukerinformasjonDto.getForespørslerSomHovedpart().size()).isEqualTo(0)
    );
  }

    private void bidragPersonkonsumentTestrespons() {
        when(bidragPersonkonsument.hentPersoninfo(HOVEDPART.getIdent())).thenReturn(HentPersoninfoRespons.builder()
                .fornavn(HOVEDPART.getFornavn())
                .foedselsdato(HOVEDPART.getFødselsdato()).build());

        when(bidragPersonkonsument.hentPersoninfo(MOTPART.getIdent())).thenReturn(HentPersoninfoRespons.builder()
                .fornavn(MOTPART.getFornavn())
                .foedselsdato(MOTPART.getFødselsdato()).build());

        when(bidragPersonkonsument.hentPersoninfo(BARN_OVER_FEMTEN.getIdent())).thenReturn(HentPersoninfoRespons.builder()
                .fornavn(BARN_OVER_FEMTEN.getFornavn())
                .foedselsdato(BARN_OVER_FEMTEN.getFødselsdato()).build());

        when(bidragPersonkonsument.hentPersoninfo(BARN_UNDER_FEMTEN.getIdent())).thenReturn(HentPersoninfoRespons.builder()
                .fornavn(BARN_UNDER_FEMTEN.getFornavn())
                .foedselsdato(BARN_UNDER_FEMTEN.getFødselsdato()).build());

        when(bidragPersonkonsument.hentPersoninfo(BARN_OVER_ATTEN.getIdent())).thenReturn(HentPersoninfoRespons.builder()
                .fornavn(BARN_OVER_ATTEN.getFornavn())
                .foedselsdato(BARN_OVER_ATTEN.getFødselsdato()).build());
    }

    private HentFamilieRespons oppretteHentFamilieRespons(Testperson hovedpart, List<MotpartBarnRelasjon> motpartBarnReasjoner) {
      return HentFamilieRespons.builder()
              .person(Familiemedlem.builder()
                      .ident(hovedpart.getIdent())
                      .fornavn(hovedpart.getFornavn())
                      .foedselsdato(hovedpart.getFødselsdato())
                      .build())
              .personensMotpartBarnRelasjon(motpartBarnReasjoner)
              .build();
    }

    private HentFamilieRespons oppretteHentFamilieRespons(Testperson hovedpart, Testperson motpart) {
        return oppretteHentFamilieRespons(hovedpart, List.of(oppretteMotpartBarnRelasjon(motpart)));
    }

    private MotpartBarnRelasjon oppretteMotpartBarnRelasjon(Testperson motpart) {
        return MotpartBarnRelasjon.builder()
                .relasjonMotpart(FAR)
                .motpart(Familiemedlem.builder()
                        .ident(motpart.getIdent())
                        .fornavn(motpart.getFornavn())
                        .foedselsdato(motpart.getFødselsdato())
                        .build())
                .fellesBarn(
                        List.of(
                                Familiemedlem.builder()
                                        .ident(BARN_UNDER_FEMTEN.getIdent())
                                        .fornavn(BARN_UNDER_FEMTEN.getFornavn())
                                        .foedselsdato(BARN_UNDER_FEMTEN.getFødselsdato())
                                        .build(),
                                Familiemedlem.builder()
                                        .ident(BARN_OVER_FEMTEN.getIdent())
                                        .fornavn(BARN_OVER_FEMTEN.getFornavn())
                                        .foedselsdato(BARN_OVER_FEMTEN.getFødselsdato())
                                        .build(),
                                Familiemedlem.builder()
                                        .ident(BARN_OVER_ATTEN.getIdent())
                                        .fornavn(BARN_OVER_ATTEN.getFornavn())
                                        .foedselsdato(BARN_OVER_ATTEN.getFødselsdato())
                                        .build()
                        )
                )
                .build();
    }

    private Forespørsel oppretteForespørsel(String identHovedpart, String identMotpart, Set<Barn> barn) {

        return Forespørsel.builder()
                .barn(barn)
                .hovedpart(Forelder.builder().personident(identHovedpart).build())
                .motpart(Forelder.builder().personident(identMotpart).build())
                .opprettet(LocalDateTime.now().minusDays(5))
                .samtykket(LocalDateTime.now().minusDays(5))
                .journalført(LocalDateTime.now().minusDays(5))
                .build();
    }
}

