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
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import no.nav.bidrag.commons.ExceptionLogger;
import no.nav.bidrag.commons.security.api.EnableSecurityConfiguration;
import no.nav.bidrag.commons.web.CorrelationIdFilter;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

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
@EnableRetry
@EnableAspectJAutoProxy
public class Applikasjonskonfig {

  public static final String ISSUER_TOKENX = "tokenx";
  public static final Logger SIKKER_LOGG = LoggerFactory.getLogger("secureLogger");
  public static final long FORESPØRSLER_SYNLIGE_I_ANTALL_DAGER_ETTER_SISTE_STATUSOPPDATERING = 30L;

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .components(new Components()
            .addSecuritySchemes("bearer-key", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT"))
        ).info(new io.swagger.v3.oas.models.info.Info().title("bidrag-reisekostnad-api").version("v1"));
  }

  @Bean
  public ExceptionLogger exceptionLogger() {
    return new ExceptionLogger(Applikasjonskonfig.class.getSimpleName());
  }

  @Bean
  public CorrelationIdFilter correlationIdFilter() {
    return new CorrelationIdFilter();
  }

  @Configuration
  @Profile(Profil.NAIS)
  public static class FlywayConfiguration {

    @Autowired
    public FlywayConfiguration(@Qualifier("dataSource") DataSource dataSource)
        throws InterruptedException {
      Thread.sleep(30000);
      Flyway.configure().dataSource(dataSource).load().migrate();
    }
  }

  @Configuration
  @Profile({Profil.NAIS, Profil.LOKAL_POSTGRES})
  @EnableScheduling
  @EnableSchedulerLock(defaultLockAtMostFor = "PT30S")
  public class SchedulerConfiguration {

    @Bean
    public LockProvider lockProvider(DataSource dataSource) {
      return new JdbcTemplateLockProvider(dataSource);
    }
  }
}
