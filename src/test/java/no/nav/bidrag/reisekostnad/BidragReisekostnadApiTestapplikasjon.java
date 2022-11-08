package no.nav.bidrag.reisekostnad;

import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.context.EmbeddedKafka;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = ASSIGNABLE_TYPE, value = {BidragReiesekostnadApiApplikasjon.class})})
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
    topics = {"aapen-brukernotifikasjon-nyBeskjed-v1", "aapen-brukernotifikasjon-done-v1", "aapen-brukernotifikasjon-nyOppgave-v1"})
@EnableJwtTokenValidation(ignore = {"org.springdoc", "org.springframework"})
@Slf4j
@EntityScan("no.nav.bidrag.reisekostnad.database.datamodell")
public class BidragReisekostnadApiTestapplikasjon {

  public static void main(String... args) {

    String profile = args.length < 1 ? BidragReisekostnaderApiTestkonfig.LOKAL_PROFIL : args[0];
    SpringApplication app = new SpringApplication(BidragReisekostnadApiTestapplikasjon.class);
    app.setAdditionalProfiles(profile);
    app.run(args);
  }

}

@Configuration
 class BidragReisekostnaderApiTestkonfig {
  public static final String LOKAL_PROFIL = "lokal";

}
