package no.nav.bidrag.reisekostnad.konfigurasjon;

import java.util.Optional;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

@Configuration
public class RestTemplatekonfig {

  @Bean
  @Scope("prototype")
  public HttpHeaderRestTemplate httpHeaderRestTemplate() {
    HttpHeaderRestTemplate httpHeaderRestTemplate = new HttpHeaderRestTemplate(new HttpComponentsClientHttpRequestFactory());
    httpHeaderRestTemplate.addHeaderGenerator(CorrelationIdFilter.CORRELATION_ID_HEADER, CorrelationIdFilter::fetchCorrelationIdForThread);

    return httpHeaderRestTemplate;
  }

  @Bean
  @Scope("prototype")
  @Qualifier("bidrag-person")
  public HttpHeaderRestTemplate bidragPersonRestTemplate(
      @Value("${integrasjon.bidrag.person.url}") String urlBidragPerson,
      HttpHeaderRestTemplate httpHeaderRestTemplate,
      ClientHttpRequestInterceptor accessTokenInterceptor) {

    httpHeaderRestTemplate.getInterceptors().add(accessTokenInterceptor);
    httpHeaderRestTemplate.setUriTemplateHandler(new RootUriTemplateHandler(urlBidragPerson));
    return httpHeaderRestTemplate;
  }

  @Bean
  @Profile({Profil.I_SKY, Profil.TEST})
  public ClientHttpRequestInterceptor accessTokenInterceptor(
      ClientConfigurationProperties clientConfigurationProperties,
      OAuth2AccessTokenService oAuth2AccessTokenService
  ) {

    ClientProperties clientProperties =
        Optional.ofNullable(clientConfigurationProperties.getRegistration().get("bidrag-person"))
            .orElseThrow(() -> new RuntimeException("fant ikke oauth2-klientkonfig for bidrag-person"));

    return (request, body, execution) -> {
      OAuth2AccessTokenResponse response =
          oAuth2AccessTokenService.getAccessToken(clientProperties);
      request.getHeaders().setBearerAuth(response.getAccessToken());
      return execution.execute(request, body);
    };
  }
}
