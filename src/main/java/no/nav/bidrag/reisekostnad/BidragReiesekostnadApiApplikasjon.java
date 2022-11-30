package no.nav.bidrag.reisekostnad;

import lombok.extern.slf4j.Slf4j;
import no.nav.security.token.support.client.spring.oauth2.EnableOAuth2Client;
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

@Slf4j
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class,  ManagementWebSecurityAutoConfiguration.class})
@EnableJwtTokenValidation(ignore = {"org.springdoc", "org.springframework"})
public class BidragReiesekostnadApiApplikasjon {
	public static void main(String[] args) {
		SpringApplication.run(BidragReiesekostnadApiApplikasjon.class, args);
	}

}
