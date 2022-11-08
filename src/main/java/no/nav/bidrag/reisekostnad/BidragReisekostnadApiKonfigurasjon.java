package no.nav.bidrag.reisekostnad;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.tilgangskontroll.SecurityUtils;
import no.nav.security.token.support.core.context.TokenValidationContextHolder;
import no.nav.security.token.support.core.jwt.JwtToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EntityScan("no.nav.bidrag.reisekostnad.database.datamodell")
@OpenAPIDefinition(
    info = @Info(title = "bidrag-reisekostnad-api", version = "v1"),
    security = @SecurityRequirement(name = "bearer-key"))
public class BidragReisekostnadApiKonfigurasjon {

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
  public OidcTokenManager oidcTokenManager(TokenValidationContextHolder tokenValidationContextHolder) {
    return () -> Optional.ofNullable(tokenValidationContextHolder).map(TokenValidationContextHolder::getTokenValidationContext)
        .map(tokenValidationContext -> tokenValidationContext.getJwtTokenAsOptional(ISSUER_TOKENX))
        .map(Optional::get)
        .map(JwtToken::getTokenAsString)
        .orElseThrow(() -> new IllegalStateException("Kunne ikke videresende Bearer token"));
  }

  @Bean
  public OidcTokenSubjectExtractor oidcTokenSubjectExtractor(OidcTokenManager oidcTokenManager) {
    return () -> henteIdentFraToken(oidcTokenManager);
  }

  private String henteIdentFraToken(OidcTokenManager oidcTokenManager) {
    var ident = SecurityUtils.hentePid(oidcTokenManager.hentIdToken());
    return erNumerisk(ident) ? ident : SecurityUtils.henteSubject(oidcTokenManager.hentIdToken());
  }

  @FunctionalInterface
  public interface OidcTokenManager {

    String hentIdToken();
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


}
