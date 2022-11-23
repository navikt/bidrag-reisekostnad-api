package no.nav.bidrag.reisekostnad;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import com.github.tomakehurst.wiremock.extension.responsetemplating.ResponseTemplateTransformer;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.konfigurasjon.Profil;
import no.nav.security.mock.oauth2.MockOAuth2Server;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.Rule;
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

@Slf4j
@Profile(Profil.LOKAL)
@AutoConfigureWireMock(port = 0)
@EnableJwtTokenValidation(ignore = {"org.springdoc", "org.springframework"})
@EntityScan("no.nav.bidrag.reisekostnad.database.datamodell")
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
    topics = {"aapen-brukernotifikasjon-nyBeskjed-v1", "aapen-brukernotifikasjon-done-v1", "aapen-brukernotifikasjon-nyOppgave-v1"})
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = ASSIGNABLE_TYPE, value = {BidragReiesekostnadApiApplikasjon.class})})
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
public class BidragReisekostnadApiLokalTestapplikasjon {

  public static void main(String... args) {

    String[] profiles = args.length < 1 ? new String[]{Profil.LOKAL} : args;
    SpringApplication app = new SpringApplication(BidragReisekostnadApiLokalTestapplikasjon.class);
    app.setAdditionalProfiles(profiles);
    app.run(args);
  }
}

@Configuration
@Profile(Profil.LOKAL)
@EnableMockOAuth2Server
@AutoConfigureWireMock(port = 0)
class LokalTestkonfig {

  @Autowired
  private MockOAuth2Server mockOAuth2Server;

  @Bean
  @Primary
  public ClientHttpRequestInterceptor accessTokenInterceptor() {
    return (request, body, execution) -> {
      request.getHeaders().setBearerAuth(mockOAuth2Server.issueToken().serialize());
      return execution.execute(request, body);
    };
  }

  @Rule
  public WireMockRule wm = new WireMockRule(options()
      .extensions(new ResponseTemplateTransformer(false))
  );

}
