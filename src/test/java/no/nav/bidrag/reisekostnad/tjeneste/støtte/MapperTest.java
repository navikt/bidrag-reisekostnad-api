package no.nav.bidrag.reisekostnad.tjeneste.støtte;

import static no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.MotpartBarnRelasjon.Relasjon.FAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Value;
import no.nav.bidrag.reisekostnad.BidragReisekostnadApiTestapplikasjon;
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

@DisplayName("MapperTest")
@ActiveProfiles(Profil.TEST)
@EnableMockOAuth2Server
@AutoConfigureWireMock(port=0)
@AutoConfigureTestDatabase(replace = Replace.ANY)
@SpringBootTest(classes = {Mapper.class, ForespørselDao.class, BidragReisekostnadApiTestapplikasjon.class})
public class MapperTest {

  private @Autowired Mapper mapper;

  private @Autowired BarnDao barnDao;
  private @Autowired ForelderDao forelderDao;
  private @Autowired ForespørselDao forespørselDao;

  private @MockBean BidragPersonkonsument bidragPersonkonsument;

  private @MockBean TokenValidationContextHolder tokenValidationContextHolder;

  private static final Testperson HOVEDPART = new Testperson("00000", "Pegasus", 35);
  private static final Testperson MOTPART = new Testperson("11111", "Zuez", 43);
  private static final Testperson BARN_OVER_FEMTEN = new Testperson("12345", "Alfa", 16);
  private static final Testperson BARN_UNDER_FEMTEN = new Testperson("44444", "Beta", 12);

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

    forespørselDao.save(oppretteForespørsel(denAndreParten.getFødselsnummer(),
        testperson.getFødselsnummer(),
        familierespons.getPersonensMotpartBarnRelasjon().stream()
            .flatMap(mbr -> mbr.getFellesBarn().stream())
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

  private void bidragPersonkonsumentTestrespons() {
    when(bidragPersonkonsument.hentPersoninfo(HOVEDPART.getFødselsnummer())).thenReturn(HentPersoninfoRespons.builder()
        .fornavn(HOVEDPART.getFornavn())
        .foedselsdato(HOVEDPART.getFødselsdato()).build());

    when(bidragPersonkonsument.hentPersoninfo(MOTPART.getFødselsnummer())).thenReturn(HentPersoninfoRespons.builder()
        .fornavn(MOTPART.getFornavn())
        .foedselsdato(MOTPART.getFødselsdato()).build());

    when(bidragPersonkonsument.hentPersoninfo(BARN_OVER_FEMTEN.getFødselsnummer())).thenReturn(HentPersoninfoRespons.builder()
        .fornavn(BARN_OVER_FEMTEN.getFornavn())
        .foedselsdato(BARN_OVER_FEMTEN.getFødselsdato()).build());

    when(bidragPersonkonsument.hentPersoninfo(BARN_UNDER_FEMTEN.getFødselsnummer())).thenReturn(HentPersoninfoRespons.builder()
        .fornavn(BARN_UNDER_FEMTEN.getFornavn())
        .foedselsdato(BARN_UNDER_FEMTEN.getFødselsdato()).build());
  }

  private HentFamilieRespons oppretteHentFamilieRespons(Testperson hovedpart, Testperson motpart) {
    return HentFamilieRespons.builder()
        .person(Familiemedlem.builder()
            .ident(hovedpart.getFødselsnummer())
            .fornavn(hovedpart.getFornavn())
            .foedselsdato(hovedpart.getFødselsdato())
            .build())
        .personensMotpartBarnRelasjon(List.of(oppretteMotpartBarnRelasjon(motpart)))
        .build();
  }

  private MotpartBarnRelasjon oppretteMotpartBarnRelasjon(Testperson motpart) {
    return MotpartBarnRelasjon.builder()
        .relasjonMotpart(FAR)
        .motpart(Familiemedlem.builder()
            .ident(motpart.getFødselsnummer())
            .fornavn(motpart.getFornavn())
            .foedselsdato(motpart.getFødselsdato())
            .build())
        .fellesBarn(
            List.of(
                Familiemedlem.builder()
                    .ident(BARN_UNDER_FEMTEN.getFødselsnummer())
                    .fornavn(BARN_UNDER_FEMTEN.getFornavn())
                    .foedselsdato(BARN_UNDER_FEMTEN.getFødselsdato())
                    .build(),
                Familiemedlem.builder()
                    .ident(BARN_OVER_FEMTEN.getFødselsnummer())
                    .fornavn(BARN_OVER_FEMTEN.getFornavn())
                    .foedselsdato(BARN_OVER_FEMTEN.getFødselsdato())
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

@Value
class Testperson {

  String personnummer;
  String fornavn;
  int alder;

  public LocalDate getFødselsdato() {
    return LocalDate.now().minusYears(alder);
  }

  public String getFødselsnummer() {
    return LocalDate.now().minusYears(alder).format(DateTimeFormatter.ofPattern("ddMMyy")) + personnummer;
  }
}
