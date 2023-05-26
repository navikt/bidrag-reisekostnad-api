package no.nav.bidrag.reisekostnad.integrasjon.bidrag.person;

import static no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode.PDL_FEIL;
import static no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode.PDL_PERSON_IKKE_FUNNET;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.SIKKER_LOGG;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Cachekonfig.CACHE_FAMILIE;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Cachekonfig.CACHE_PERSON;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.domain.ident.PersonIdent;
import no.nav.bidrag.reisekostnad.feilhåndtering.Persondatafeil;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.HentFamilieRespons;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.HentPersoninfoRespons;
import no.nav.bidrag.reisekostnad.konfigurasjon.cache.UserCacheable;
import no.nav.bidrag.transport.person.PersonRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class BidragPersonkonsument {

  public static final String ENDEPUNKT_MOTPART_BARN_RELASJON = "/motpartbarnrelasjon";
  public static final String ENDEPUNKT_PERSONINFO = "/informasjon";
  public static final String BIDRAG_PERSON_KONTEKSTROT = "/bidrag-person";
  private final RestTemplate clientCredentialsRestTemplate;

  public static final String FORMAT_FØDSELSDATO = "yyyy-MM-dd";

  @Autowired
  public BidragPersonkonsument(@Qualifier("bidrag-person-azure-client-credentials") RestTemplate clientCredentialsRestTemplate) {
    this.clientCredentialsRestTemplate = clientCredentialsRestTemplate;
  }

  @UserCacheable(CACHE_FAMILIE)
  @Retryable(value = Exception.class, backoff = @Backoff(delay = 5000, multiplier = 2.0))
  public Optional<HentFamilieRespons> hentFamilie(String personident) {
    var forespørsel = new PersonRequest(new PersonIdent(personident));

    try {
      var hentFamilieRespons = clientCredentialsRestTemplate.exchange(BIDRAG_PERSON_KONTEKSTROT + ENDEPUNKT_MOTPART_BARN_RELASJON, HttpMethod.POST,
          new HttpEntity<>(forespørsel),
          HentFamilieRespons.class);
      return Optional.of(hentFamilieRespons).map(ResponseEntity::getBody);
    } catch (HttpStatusCodeException hsce) {
      if (HttpStatus.NOT_FOUND.equals(hsce.getStatusCode())) {
        SIKKER_LOGG.warn("Kall mot bidrag-person for henting av familierelasjoner returnerte httpstatus {} for personident {}",
            hsce.getStatusCode(), personident);
        throw new Persondatafeil(PDL_PERSON_IKKE_FUNNET, hsce.getStatusCode());
      } else {
        SIKKER_LOGG.warn("Kall mot bidrag-person for henting av familierelasjoner returnerte httpstatus {} for personident {}", hsce.getStatusCode(),
            personident);
        throw new Persondatafeil(PDL_FEIL, hsce.getStatusCode());
      }
    }
  }

  @UserCacheable(CACHE_PERSON)
  @Retryable(value = Exception.class, backoff = @Backoff(delay = 1000, multiplier = 2.0))
  public HentPersoninfoRespons hentPersoninfo(String personident) {
    var forespørsel = new PersonRequest(new PersonIdent(personident));
    try {
      var hentPersoninfo = clientCredentialsRestTemplate.exchange(BIDRAG_PERSON_KONTEKSTROT + ENDEPUNKT_PERSONINFO, HttpMethod.POST,
          new HttpEntity<>(forespørsel),
          HentPersoninfoRespons.class);
      return hentPersoninfo.getBody();
    } catch (HttpStatusCodeException hsce) {
      SIKKER_LOGG.warn("Kall mot bidrag-person for henting av personinfo returnerte httpstatus {} for personident {}", hsce.getStatusCode(),
          personident, hsce);
      var feilkode = HttpStatus.NOT_FOUND.equals(hsce.getStatusCode()) ? PDL_PERSON_IKKE_FUNNET : PDL_FEIL;
      throw new Persondatafeil(feilkode, hsce.getStatusCode());
    }
  }
}
