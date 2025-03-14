package no.nav.bidrag.reisekostnad.konfigurasjon;

import no.nav.bidrag.commons.security.service.SecurityTokenService;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import no.nav.bidrag.commons.web.HttpHeaderRestTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RootUriTemplateHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class RestTemplatekonfig {

  @Bean
  @Profile(value = {Profil.I_SKY, Profil.LOKAL_SKY})
  public ClientHttpRequestInterceptor tokenxInterceptor(SecurityTokenService securityTokenService) {
    return securityTokenService.authTokenInterceptor();
  }

  @Bean
  @Profile(value = {Profil.I_SKY, Profil.LOKAL_SKY, Profil.HENDELSE})
  public ClientHttpRequestInterceptor bidragDokumentClientCredentialsTokenInterceptor(SecurityTokenService securityTokenService) {
    return securityTokenService.serviceUserAuthTokenInterceptor("bidrag-dokument");
  }

  @Bean
  @Profile(value = {Profil.I_SKY, Profil.LOKAL_SKY, Profil.HENDELSE})
  public ClientHttpRequestInterceptor bidragPersonClientCredentialsTokenInterceptor(SecurityTokenService securityTokenService) {
    return securityTokenService.serviceUserAuthTokenInterceptor("bidrag-person");
  }

  @Bean
  @Scope("prototype")
  public HttpHeaderRestTemplate httpHeaderRestTemplate() {
    HttpHeaderRestTemplate httpHeaderRestTemplate = new HttpHeaderRestTemplate(new HttpComponentsClientHttpRequestFactory());
    httpHeaderRestTemplate.addHeaderGenerator(CorrelationIdFilter.CORRELATION_ID_HEADER, CorrelationIdFilter::fetchCorrelationIdForThread);

    return httpHeaderRestTemplate;
  }

  @Bean
  @Scope("prototype")
  @Qualifier("bidrag-dokument-azure-client-credentials")
  public HttpHeaderRestTemplate bidragDokumentAzureCCRestTemplate(
      @Value("${integrasjon.bidrag.dokument.url}") String urlBidragPerson,
      HttpHeaderRestTemplate httpHeaderRestTemplate,
      ClientHttpRequestInterceptor bidragDokumentClientCredentialsTokenInterceptor
  ) {
    httpHeaderRestTemplate.getInterceptors().add(bidragDokumentClientCredentialsTokenInterceptor);
    httpHeaderRestTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(urlBidragPerson));
    return httpHeaderRestTemplate;
  }

  @Bean
  @Scope("prototype")
  @Qualifier("bidrag-person-azure-client-credentials")
  public HttpHeaderRestTemplate bidragPersonAzureCCRestTemplate(
      @Value("${integrasjon.bidrag.person.url}") String urlBidragPerson,
      HttpHeaderRestTemplate httpHeaderRestTemplate,
      ClientHttpRequestInterceptor bidragPersonClientCredentialsTokenInterceptor
  ) {
    httpHeaderRestTemplate.getInterceptors().add(bidragPersonClientCredentialsTokenInterceptor);
    httpHeaderRestTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(urlBidragPerson));
    return httpHeaderRestTemplate;
  }
}
