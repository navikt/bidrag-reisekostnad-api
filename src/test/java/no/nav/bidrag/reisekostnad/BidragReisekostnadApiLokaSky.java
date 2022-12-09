package no.nav.bidrag.reisekostnad;

import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.konfigurasjon.Profil;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.context.EmbeddedKafka;

@Slf4j
@Profile({Profil.LOKAL_H2, Profil.LOKAL_POSTGRES})
@EnableJwtTokenValidation(ignore = {"org.springdoc", "org.springframework"})
@EntityScan("no.nav.bidrag.reisekostnad.database.datamodell")
@EmbeddedKafka(partitions = 1, brokerProperties = {"listeners=PLAINTEXT://localhost:9092", "port=9092"},
    topics = {"aapen-brukernotifikasjon-nyBeskjed-v1", "aapen-brukernotifikasjon-done-v1", "aapen-brukernotifikasjon-nyOppgave-v1"})
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = ASSIGNABLE_TYPE, value = {BidragReiesekostnadApiApplikasjon.class})})
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
public class BidragReisekostnadApiLokaSky {

  public static void main(String... args) {
    SpringApplication app = new SpringApplication(BidragReisekostnadApiLokalTestapplikasjon.class);
    app.run(args);
  }
}


