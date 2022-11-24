package no.nav.bidrag.reisekostnad.konfigurasjon;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration;
import no.nav.bidrag.commons.security.service.OidcTokenManager;
import no.nav.bidrag.commons.security.utils.TokenUtils;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@EntityScan("no.nav.bidrag.reisekostnad.database.datamodell")
@io.swagger.v3.oas.annotations.security.SecurityScheme(
    bearerFormat = "JWT",
    name = "bearer-key",
    scheme = "bearer",
    type = SecuritySchemeType.HTTP
)
@OpenAPIDefinition(
    info = @Info(title = "bidrag-reisekostnad-api", version = "v1"),
    security = @SecurityRequirement(name = "bearer-key"))
@EnableSecurityConfiguration
public class Applikasjonskonfig {

  public static final String ISSUER_TOKENX = "tokenx";
  public static final Logger SIKKER_LOGG = LoggerFactory.getLogger("secureLogger");

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .components(new Components()
            .addSecuritySchemes("bearer-key", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
        ).info(new io.swagger.v3.oas.models.info.Info().title("bidrag-reisekostnad-api").version("v1"));
  }

  @Bean
  public OidcTokenSubjectExtractor oidcTokenSubjectExtractor(OidcTokenManager oidcTokenManager) {
    return () -> henteIdentFraToken(oidcTokenManager);
  }

  private String henteIdentFraToken(OidcTokenManager oidcTokenManager) {
    var ident = TokenUtils.fetchSubject(oidcTokenManager.fetchTokenAsString());
    return erNumerisk(ident) ? ident : TokenUtils.fetchSubject(oidcTokenManager.fetchTokenAsString());
  }

  @FunctionalInterface
  public interface OidcTokenSubjectExtractor {

    String hentPaaloggetPerson();
  }

  public static boolean erNumerisk(String ident) {
    try {
      Long.parseLong(ident);
      log.info("Identen er numerisk");
      return true;
    } catch (NumberFormatException e) {
      log.warn("Identen er ikke numerisk");
      return false;
    }
  }

  @Configuration
  @Profile(Profil.I_SKY)
  public static class FlywayConfiguration {

    @Autowired
    public FlywayConfiguration(@Qualifier("dataSource") DataSource dataSource)
        throws InterruptedException {
      Thread.sleep(30000);
      Flyway.configure().dataSource(dataSource).load().migrate();
    }
  }
}
