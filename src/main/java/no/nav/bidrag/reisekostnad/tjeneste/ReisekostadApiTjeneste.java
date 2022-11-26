package no.nav.bidrag.reisekostnad.tjeneste;

import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.SIKKER_LOGG;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.reisekostnad.api.dto.ut.BrukerinformasjonDto;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.Persondatafeil;
import no.nav.bidrag.reisekostnad.feilhåndtering.Valideringsfeil;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.BidragPersonkonsument;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Krypteringsverktøy;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ReisekostadApiTjeneste {

  private final BidragPersonkonsument bidragPersonkonsument;
  private Databasetjeneste databasetjeneste;
  private final Mapper mapper;

  @Autowired
  public ReisekostadApiTjeneste(BidragPersonkonsument bidragPersonkonsument, Databasetjeneste databasetjeneste, Mapper mapper) {
    this.bidragPersonkonsument = bidragPersonkonsument;
    this.databasetjeneste = databasetjeneste;
    this.mapper = mapper;
  }

  public HttpResponse<BrukerinformasjonDto> henteBrukerinformasjon(String fnrPaaloggetBruker) {
    SIKKER_LOGG.info("Henter brukerinformasjon for person med ident {}", fnrPaaloggetBruker);

    var familieRespons = bidragPersonkonsument.hentFamilie(fnrPaaloggetBruker);

    if (!familieRespons.isPresent() || familieRespons.get().getPerson() == null) {
      throw new Persondatafeil(Feilkode.PDL_PERSON_IKKE_FUNNET, HttpStatus.NOT_FOUND);
    }

    return HttpResponse.from(HttpStatus.OK, mapper.tilDto(familieRespons.get()));
  }

  public HttpResponse<Void> oppretteForespørselOmFordelingAvReisekostnader(String hovedperson, Set<String> krypterteIdenterBarn) {

    var personidenterBarn = krypterteIdenterBarn.stream().map(k -> dekryptere(k)).collect(Collectors.toSet());

    // Kaster Valideringsfeil dersom hovedperson ikke er registrert med familierelasjoner eller mangler relasjon til minst ett av de oppgigge barna.
    validereRelasjonTilBarn(hovedperson, personidenterBarn);

    var familie = bidragPersonkonsument.hentFamilie(hovedperson);

    familie.get().getPersonensMotpartBarnRelasjon().stream().filter(Objects::nonNull)
        .forEach(m -> lagreForespørsel(hovedperson, m.getMotpart().getIdent(), mapper.tilStringSet(m.getFellesBarn())));

    return HttpResponse.from(HttpStatus.CREATED);
  }

  private void lagreForespørsel(String hovedperson, String motpart, Set<String> barn) {

    var barnOver15År = filtrereUtBarnOver15År(barn);
    var barnUnder15År = barn.stream().filter(b -> !erPersonOver15År(b)).collect(Collectors.toSet());

    if (barnOver15År.size() > 0) {
      databasetjeneste.lagreNyForespørsel(hovedperson, motpart, barnOver15År, false);
    }

    if (barnUnder15År.size() > 0) {
      databasetjeneste.lagreNyForespørsel(hovedperson, motpart, barnUnder15År, true);
    }
  }

  public HttpResponse<Void> oppdatereForespørselMedSamtykke(int idForespørsel, String personidentMotpart) {
    // Kaster Valideringsfeil dersom forespørsel ikke finnes eller oppdatering av samtykke  feiler
    databasetjeneste.giSamtykke(idForespørsel, personidentMotpart);
    return HttpResponse.from(HttpStatus.OK, null);
  }

  public HttpResponse<Void> trekkeForespørsel(int idForespørsel, String personidentHovedpart) {
    // Kaster Valideringsfeil dersom forespørsel ikke finnes eller deaktivering feiler
    databasetjeneste.deaktivereForespørsel(idForespørsel, personidentHovedpart);
    return HttpResponse.from(HttpStatus.OK, null);
  }

  private void validereRelasjonTilBarn(String hovedperson, Set<String> barn) {
    var respons = bidragPersonkonsument.hentFamilie(hovedperson);
    if (respons.isEmpty()) {
      throw new Valideringsfeil(Feilkode.VALIDERING_NY_FOREPØRSEL_INGEN_FAMILIERELASJONER);
    }

    var allePersonensBarnMedDeltForeldreansvar = respons.get().getPersonensMotpartBarnRelasjon().stream().flatMap(m -> m.getFellesBarn().stream())
        .map(f -> f.getIdent())
        .collect(Collectors.toSet());

    var alleBarnaIForespørselenHarRelasjonTilPåloggetPerson = erSubsett(barn, allePersonensBarnMedDeltForeldreansvar);

    if (!alleBarnaIForespørselenHarRelasjonTilPåloggetPerson) {
      throw new Valideringsfeil(Feilkode.VALIDERING_NY_FOREPØRSEL_MANGLER_RELASJON_BARN);
    }
  }

  private boolean erSubsett(Set<String> subsett, Set<String> supersett) {

    if (subsett == null || subsett.size() < 1 || supersett == null || supersett.size() < 1) {
      return false;
    }

    for (String element : subsett) {
      if (!supersett.contains(element)) {
        return false;
      }
    }
    return true;
  }


  private boolean inneholderBarnOver15År(Set<String> personidenterBarn) {
    return personidenterBarn.stream().filter(Objects::nonNull).filter(this::erPersonOver15År).findFirst().isPresent();
  }

  private boolean erPersonOver15År(String personident) {
    var respons = bidragPersonkonsument.hentPersoninfo(personident);
    if (respons.isPresent()) {
      var fødselsdato = respons.get().getFoedselsdato();
      return fødselsdato != null && fødselsdato.isBefore(LocalDate.now().minusYears(15));
    } else {
      return false;
    }
  }

  private Set<String> filtrereUtBarnOver15År(Set<String> alleBarn) {
    return alleBarn.stream().filter(this::erPersonOver15År).collect(Collectors.toSet());
  }

  private String dekryptere(String kryptertPersonident) {
    return Krypteringsverktøy.dekryptere(kryptertPersonident);
  }

}
