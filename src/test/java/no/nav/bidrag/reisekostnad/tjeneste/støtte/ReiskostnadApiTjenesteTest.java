package no.nav.bidrag.reisekostnad.tjeneste.støtte;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.BidragPersonkonsument;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.Familiemedlem;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.HentFamilieRespons;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.MotpartBarnRelasjon;
import no.nav.bidrag.reisekostnad.tjeneste.Databasetjeneste;
import no.nav.bidrag.reisekostnad.tjeneste.ReisekostnadApiTjeneste;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReiskostnadApiTjenesteTest {

  @Mock
  private BidragPersonkonsument bidragPersonkonsument;
  @Mock
  private Databasetjeneste databasetjeneste;

  @Mock
  private Mapper mapper;

  @InjectMocks
  private ReisekostnadApiTjeneste reisekostnadApiTjeneste;

  @Test
  void skalOppretteForespørselKunForMotpartsBarn() {

    // gitt
    var personidentHovedperson = "77712365478";
    var personidentMotpart = "90904513277";
    var personidentFellesBarn = "12345670000";
    var brotherFromADifferentMother = "98765432154";
    var aDifferentMother = "975465451234";
    var valgteKrypterteBarn = Set.of(Krypteringsverktøy.kryptere(personidentFellesBarn), Krypteringsverktøy.kryptere(brotherFromADifferentMother));
    var familierespons = HentFamilieRespons.builder()
        .person(
            Familiemedlem.builder()
                .foedselsdato(LocalDate.now().minusYears(43))
                .ident(personidentHovedperson)
                .fornavn("Ufin")
                .build())
        .personensMotpartBarnRelasjon(List.of(
            MotpartBarnRelasjon.builder()
                .motpart(Familiemedlem.builder()
                    .foedselsdato(LocalDate.now().minusYears(39))
                    .fornavn("Direkte")
                    .ident(personidentMotpart)
                    .build())
                .fellesBarn(List.of(Familiemedlem.builder()
                    .ident(personidentFellesBarn)
                    .fornavn("Småstein")
                    .foedselsdato(LocalDate.now().minusYears(8))
                    .build()))
                .build(),
            MotpartBarnRelasjon.builder()
                .motpart(Familiemedlem.builder()
                    .ident(aDifferentMother)
                    .fornavn("Anderledes")
                    .foedselsdato(LocalDate.now().minusYears(44))
                    .build())
                .fellesBarn(List.of(Familiemedlem.builder()
                    .ident(brotherFromADifferentMother)
                    .fornavn("Bror")
                    .foedselsdato(LocalDate.now().minusYears(17))
                    .build()))
                .build()
        ))
        .build();

    when(bidragPersonkonsument.hentFamilie(personidentHovedperson)).thenReturn(Optional.of(familierespons));
    when(databasetjeneste.lagreNyForespørsel(personidentHovedperson, personidentMotpart, Set.of(personidentFellesBarn), true)).thenReturn(1);

    // hvis
    var respons = reisekostnadApiTjeneste.oppretteForespørselOmFordelingAvReisekostnader(personidentHovedperson, valgteKrypterteBarn);

    // så
    assertThat(respons.is2xxSuccessful());
  }
}
