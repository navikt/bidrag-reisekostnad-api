package no.nav.bidrag.reisekostnad.tjeneste.støtte;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.BidragPersonkonsument;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.Familiemedlem;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.HentFamilieRespons;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.HentPersoninfoRespons;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.MotpartBarnRelasjon;
import no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon.Brukernotifikasjonkonsument;
import no.nav.bidrag.reisekostnad.tjeneste.Arkiveringstjeneste;
import no.nav.bidrag.reisekostnad.tjeneste.Databasetjeneste;
import no.nav.bidrag.reisekostnad.tjeneste.ReisekostnadApiTjeneste;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReisekostnadApiTjenesteTest {

  private @Mock BidragPersonkonsument bidragPersonkonsument;
  private @Mock Brukernotifikasjonkonsument brukernotifikasjonkonsument;
  private @Mock Arkiveringstjeneste arkiveringstjeneste;
  private @Mock Databasetjeneste databasetjeneste;
  private @Mock Mapper mapper;
  private @InjectMocks ReisekostnadApiTjeneste reisekostnadApiTjeneste;

  @Test
  void skalOppretteForespørselKunForMotpartsBarn() {

    // gitt
    var personidentHovedperson = "77712365478";
    var personidentMotpart = "90904513277";
    var småstein = Familiemedlem.builder()
        .ident("12345670000")
        .fornavn("Småstein")
        .foedselsdato(LocalDate.now().minusYears(8))
        .build();
    var brorAvEnAnnenMor = Familiemedlem.builder()
        .ident("98765432154")
        .fornavn("Bror")
        .foedselsdato(LocalDate.now().minusYears(17))
        .build();
    var aDifferentMother = "975465451234";
    var valgteKrypterteBarn = Set.of(Krypteringsverktøy.kryptere(småstein.getIdent()), Krypteringsverktøy.kryptere(brorAvEnAnnenMor.getIdent()));
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
                .fellesBarn(List.of(småstein))
                .build(),
            MotpartBarnRelasjon.builder()
                .motpart(Familiemedlem.builder()
                    .ident(aDifferentMother)
                    .fornavn("Anderledes")
                    .foedselsdato(LocalDate.now().minusYears(44))
                    .build())
                .fellesBarn(List.of(brorAvEnAnnenMor))
                .build()
        ))
        .build();

    var hentPersoninfoSmåstein = HentPersoninfoRespons.builder()
        .foedselsdato(småstein.getFoedselsdato())
        .fornavn(småstein.getFornavn())
        .build();

    var hentPersoninfoBrorAvEnAnnenMor = HentPersoninfoRespons.builder()
        .foedselsdato(brorAvEnAnnenMor.getFoedselsdato())
        .fornavn(brorAvEnAnnenMor.getFornavn())
        .build();

    when(bidragPersonkonsument.hentFamilie(personidentHovedperson)).thenReturn(Optional.of(familierespons));
    when(bidragPersonkonsument.hentPersoninfo(småstein.getIdent())).thenReturn(hentPersoninfoSmåstein);
    when(bidragPersonkonsument.hentPersoninfo(brorAvEnAnnenMor.getIdent())).thenReturn(hentPersoninfoBrorAvEnAnnenMor);
    when(databasetjeneste.lagreNyForespørsel(personidentHovedperson, personidentMotpart, Set.of(småstein.getIdent()), true)).thenReturn(1);
    doNothing().when(brukernotifikasjonkonsument).oppretteOppgaveTilMotpartOmSamtykke(anyInt(), anyString());

    // hvis
    var respons = reisekostnadApiTjeneste.oppretteForespørselOmFordelingAvReisekostnader(personidentHovedperson, valgteKrypterteBarn);

    // så
    assertThat(respons.is2xxSuccessful());
  }
}
