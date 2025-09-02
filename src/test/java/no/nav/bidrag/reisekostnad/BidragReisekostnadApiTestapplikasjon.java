package no.nav.bidrag.reisekostnad;

import static org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE;

import no.nav.bidrag.reisekostnad.konfigurasjon.Profil;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;


@EntityScan("no.nav.bidrag.reisekostnad.database")
@ComponentScan(excludeFilters = {
    @ComponentScan.Filter(type = ASSIGNABLE_TYPE, value = {BidragReiesekostnadApiApplikasjon.class})})
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, ManagementWebSecurityAutoConfiguration.class})
@EnableJwtTokenValidation(ignore = {"springfox.documentation.swagger.web.ApiResourceController"})
public class BidragReisekostnadApiTestapplikasjon {

  public static void main(String... args) {
    String profile = args.length < 1 ? Profil.TEST : args[0];
    SpringApplication app = new SpringApplication(BidragReisekostnadApiTestapplikasjon.class);
    app.setAdditionalProfiles(profile);
    app.run(args);
  }
}

