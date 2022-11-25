package no.nav.bidrag.reisekostnad.integrasjon.bidrag.person;

import static no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode.PDL_FEIL;
import static no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode.PDL_PERSON_IKKE_FUNNET;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.SIKKER_LOGG;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Cachekonfig.CACHE_FAMILIE;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Cachekonfig.CACHE_PERSON;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.feilhåndtering.Persondatafeil;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.HentFamilieRespons;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.HentPersoninfoForespørsel;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.HentPersoninfoRespons;
import no.nav.bidrag.reisekostnad.konfigurasjon.cache.UserCacheable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class BidragPersonkonsument {

  public static final String ENDEPUNKT_MOTPART_BARN_RELASJON = "/motpartbarnrelasjon";
  public static final String ENDEPUNKT_PERSONINFO = "/informasjon";
  public static final String BIDRAG_PERSON_KONTEKSTROT = "/bidrag-person";
  private final RestTemplate restTemplate;
  private final RestTemplate clientCredentialsRestTemplate;

  public static final String FORMAT_FØDSELSDATO = "yyyy-MM-dd";

  @Autowired
  public BidragPersonkonsument(@Qualifier("bidrag-person") RestTemplate restTemplate, @Qualifier("bidrag-person-azure-client-credentials") RestTemplate clientCredentialsRestTemplate) {
    this.restTemplate = restTemplate;
    this.clientCredentialsRestTemplate = clientCredentialsRestTemplate;
  }
  @UserCacheable(CACHE_FAMILIE)
  public Optional<HentFamilieRespons> hentFamilie(String personident) {
    var forespørsel = HentPersoninfoForespørsel.builder().ident(personident).build();

    try {
      var person = hentPersoninfo(personident);
      var hentFamilieRespons = clientCredentialsRestTemplate.exchange(BIDRAG_PERSON_KONTEKSTROT + ENDEPUNKT_MOTPART_BARN_RELASJON, HttpMethod.POST,
          new HttpEntity<>(forespørsel),
          HentFamilieRespons.class);
      return Optional.of(hentFamilieRespons).map(ResponseEntity::getBody);
    } catch (HttpStatusCodeException hsce) {
      if (HttpStatus.NOT_FOUND.equals(hsce.getStatusCode())) {
        SIKKER_LOGG.warn("Kall mot bidrag-person for henting av familierelasjoner returnerte http status {} for personident {}",
            hsce.getStatusCode(), personident);
        throw new Persondatafeil(PDL_PERSON_IKKE_FUNNET, hsce.getStatusCode());
      } else {
        SIKKER_LOGG.warn("Kall mot bidrag-person for henting av familierelasjoner returnerte http status {} for personident {}", hsce.getStatusCode(),
            personident);
        throw new Persondatafeil(PDL_FEIL, hsce.getStatusCode());
      }
    }
  }
  @UserCacheable(CACHE_PERSON)
  public Optional<HentPersoninfoRespons> hentPersoninfo(String personident) {
    var forespørsel = HentPersoninfoForespørsel.builder().ident(personident).build();
    var hentPersoninfo = restTemplate.exchange(BIDRAG_PERSON_KONTEKSTROT + ENDEPUNKT_PERSONINFO, HttpMethod.POST,
        new HttpEntity<>(forespørsel),
        HentPersoninfoRespons.class);
    return Optional.of(hentPersoninfo).map(ResponseEntity::getBody);
  }
}
