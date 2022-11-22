package no.nav.bidrag.reisekostnad;

import com.google.common.net.HttpHeaders;
import no.nav.bidrag.commons.web.test.HttpHeaderTestRestTemplate;
import no.nav.bidrag.reisekostnad.konfigurasjon.Profil;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;

@Profile(Profil.TEST)
@EnableJwtTokenValidation
public class Testkonfig {

  @Autowired
  private MockOAuth2Server mockOAuth2Server;

  @Bean("api")
  HttpHeaderTestRestTemplate httpHeaderTestRestTemplateApi() {
    TestRestTemplate testRestTemplate = new TestRestTemplate(new RestTemplateBuilder());
    HttpHeaderTestRestTemplate httpHeaderTestRestTemplate = new HttpHeaderTestRestTemplate(testRestTemplate);
    httpHeaderTestRestTemplate.add(HttpHeaders.AUTHORIZATION, () -> generereTesttoken());
    httpHeaderTestRestTemplate.add(HttpHeaders.CONTENT_TYPE, () -> MediaType.APPLICATION_JSON.toString());
    return httpHeaderTestRestTemplate;
  }

  private String generereTesttoken() {
    var token = mockOAuth2Server.issueToken("tokenx", "aud-localhost", "aud-localhost");
    return "Bearer " + token.serialize();
  }
}
