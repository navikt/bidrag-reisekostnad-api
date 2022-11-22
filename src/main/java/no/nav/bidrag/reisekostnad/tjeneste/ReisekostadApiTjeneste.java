package no.nav.bidrag.reisekostnad.tjeneste;

import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.SIKKER_LOGG;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.web.HttpResponse;
import no.nav.bidrag.reisekostnad.api.dto.inn.NyForespørselMotpartBarnDto;
import no.nav.bidrag.reisekostnad.api.dto.ut.BrukerinformasjonDto;
import no.nav.bidrag.reisekostnad.api.dto.ut.NyForespørselRespons;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.Persondatafeil;
import no.nav.bidrag.reisekostnad.feilhåndtering.Valideringsfeil;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.BidragPersonkonsument;
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

  public HttpResponse<Void> oppretteForespørselOmFordelingAvReisekostnader(String hovedperson, Set<String> barn) {
    // Kaster Valideringsfeil dersom hovedperson ikke er registrert med familierelasjoner eller mangler relasjon til minst ett av de oppgigge barna.
    validereRelasjonTilBarn(hovedperson, barn);

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


  @Deprecated
  public HttpResponse<NyForespørselRespons> oppretteForespørselOmFordelingAvReisekostnader(String hovedperson, NyForespørselMotpartBarnDto nyForespørselMotpartBarnDto) {

    validereNyForespørsel(hovedperson, nyForespørselMotpartBarnDto);

    var respons = new NyForespørselRespons();
    if (inneholderBarnOver15År(nyForespørselMotpartBarnDto.getPersonidenterBarn())) {

      var barnOver15År = filtrereUtBarnOver15År(nyForespørselMotpartBarnDto.getPersonidenterBarn());
      var barnUnder15År = nyForespørselMotpartBarnDto.getPersonidenterBarn().stream().filter(barn -> !erPersonOver15År(barn)).collect(Collectors.toSet());

      if (barnOver15År.size() > 0) {
        respons.setIdForespørselForBarnOver15År(
            databasetjeneste.lagreNyForespørsel(hovedperson, nyForespørselMotpartBarnDto.getPersonidentMotpart(), barnOver15År, false));
      }

      if (barnUnder15År.size() > 0) {
        respons.setIdForespørselForBarnUnder15År(
            databasetjeneste.lagreNyForespørsel(hovedperson, nyForespørselMotpartBarnDto.getPersonidentMotpart(), barnUnder15År, true));
      }

    } else {
      respons.setIdForespørselForBarnUnder15År(
          databasetjeneste.lagreNyForespørsel(hovedperson, nyForespørselMotpartBarnDto.getPersonidentMotpart(), nyForespørselMotpartBarnDto.getPersonidenterBarn(), true));
    }

    return HttpResponse.from(HttpStatus.CREATED, respons);
  }

  public HttpResponse<Void> oppdatereForespørselMedSamtykke(int idForespørsel, String personident) {
    return HttpResponse.from(HttpStatus.OK, null);
  }

  public HttpResponse<Void> trekkeForespørsel(int idForespørsel, String personident) {
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

  @Deprecated
  private void validereNyForespørsel(String påloggetPerson, NyForespørselMotpartBarnDto nyForespørselMotpartBarnDto) {
    log.info("Validerer ny forespørsel");

    SIKKER_LOGG.info("Validerer ny forespørsel for pålogget person med ident {}.", påloggetPerson);

    var respons = bidragPersonkonsument.hentFamilie(påloggetPerson);

    if (respons.isEmpty()) {
      log.warn("Fant ingen familierelasjoner for pålogget person.");
      SIKKER_LOGG.warn("Fant ingen familierelasjoner for pålogget person med ident {}.", påloggetPerson);
      throw new Valideringsfeil(Feilkode.VALIDERING_NY_FOREPØRSEL);
    }

    if (nyForespørselMotpartBarnDto.getPersonidenterBarn().size() < 1) {
      log.warn("Ny forspørsel inneholder ingen barn.");
      throw new Valideringsfeil(Feilkode.VALIDERING_NY_FOREPØRSEL);
    }

    var påloggetPersonHarRelasjonTilOppgittMotpart = respons.get().getPersonensMotpartBarnRelasjon().stream()
        .filter(m -> m.getMotpart().getIdent().equals(nyForespørselMotpartBarnDto.getPersonidentMotpart())).findFirst().isPresent();

    var allePersonensBarnMedDeltForeldreansvar = respons.get().getPersonensMotpartBarnRelasjon().stream().flatMap(m -> m.getFellesBarn().stream())
        .map(f -> f.getIdent())
        .collect(Collectors.toSet());

    var alleBarnaIForespørselenHarRelasjonTilPåloggetPerson = erSubsett(nyForespørselMotpartBarnDto.getPersonidenterBarn(),
        allePersonensBarnMedDeltForeldreansvar);

    if (!påloggetPersonHarRelasjonTilOppgittMotpart || !alleBarnaIForespørselenHarRelasjonTilPåloggetPerson) {
      var feilkode = !påloggetPersonHarRelasjonTilOppgittMotpart ? Feilkode.VALIDERING_NY_FOREPØRSEL_MANGLER_RELASJON_MOTPART
          : Feilkode.VALIDERING_NY_FOREPØRSEL_MANGLER_RELASJON_BARN;
      throw new Valideringsfeil(feilkode);
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

}
