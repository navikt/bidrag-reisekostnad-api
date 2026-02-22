package no.nav.bidrag.reisekostnad;

import static no.nav.bidrag.reisekostnad.konfigurasjon.Profil.DATABASES_AND_NOT_LOKAL_SKY;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.konfigurasjon.Profil;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import no.nav.security.token.support.core.api.Unprotected;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@Slf4j
@Profile({Profil.LOKAL_H2, Profil.LOKAL_POSTGRES})
@AutoConfigureWireMock(port = 0)
@EnableJwtTokenValidation(ignore = {"org.springdoc", "org.springframework"})
@EntityScan("no.nav.bidrag.reisekostnad.database.datamodell")
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
    topics = {"aapen-brukervarsel-v1"})
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = ASSIGNABLE_TYPE, value = {BidragReiesekostnadApiApplikasjon.class})})
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
public class BidragReisekostnadApiLokalTestapplikasjon {

  public static void main(String... args) {
    SpringApplication app = new SpringApplication(BidragReisekostnadApiLokalTestapplikasjon.class);
    app.run(args);
  }
}

@Configuration
@Profile(DATABASES_AND_NOT_LOKAL_SKY)
@EnableMockOAuth2Server
class Lokalkonfig {

  @Autowired
  private MockOAuth2Server mockOAuth2Server;

  @Bean
  @Primary
  public ClientHttpRequestInterceptor clientCredentialsTokenInterceptor() {
    return (request, body, execution) -> {
      request.getHeaders().setBearerAuth(mockOAuth2Server.issueToken().serialize());
      return execution.execute(request, body);
    };
  }

  @Bean
  public LocalCookieController localCookieController() {
    return new LocalCookieController(mockOAuth2Server);
  }
}

@Slf4j
@RestController
class LocalCookieController {

  private final MockOAuth2Server mockOAuth2Server;

  public LocalCookieController(MockOAuth2Server mockOAuth2Server) {
    this.mockOAuth2Server = mockOAuth2Server;
  }

  private static final Set<String> ALLOWED_SUBJECTS = Set.of(
      "12345678910",  // Gråtass
      "55555678910",  // Råtass
      "11111122222"   // Streng
  );

  private static final String ALLOWED_ISSUER = "tokenx";

  @Unprotected
  @GetMapping("/local/cookie")
  public String getCookie(
      @RequestParam(value = "issuerId") String issuerId,
      @RequestParam(value = "audience") String audience,
      @RequestParam(value = "subject") String subject,
      HttpServletResponse response) {

    if (!ALLOWED_SUBJECTS.contains(subject)) {
      return "Invalid subject. Allowed values: " + ALLOWED_SUBJECTS;
    }
    if (!ALLOWED_ISSUER.equals(issuerId)) {
      return "Invalid issuerId. Allowed value: " + ALLOWED_ISSUER;
    }

    var token = mockOAuth2Server.issueToken(
        issuerId,
        subject,  // Use subject as the client_id so it becomes the 'sub' claim
        "access_token",
        Map.of(
            "aud", audience,
            "pid", subject,
            "acr", "Level4"
        )
    );

    var tokenString = token.serialize();

    Cookie cookie = new Cookie("localhost-idtoken", tokenString);
    cookie.setPath("/");
    cookie.setMaxAge(3600);
    response.addCookie(cookie);

    log.info("Cookie set with token for issuer: {}, audience: {}", issuerId, audience);
    return "Cookie set with token for issuer: " + issuerId + ", audience: " + audience +
           "\n\nToken (for Authorization header): Bearer " + tokenString;
  }
}


