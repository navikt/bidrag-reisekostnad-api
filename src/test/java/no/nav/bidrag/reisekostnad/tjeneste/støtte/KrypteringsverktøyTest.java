package no.nav.bidrag.reisekostnad.tjeneste.støtte;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import no.nav.bidrag.reisekostnad.konfigurasjon.Profil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

// Tell JUnit to use Spring's property loading capabilities
@ExtendWith(SpringExtension.class)
// Manage Krypteringsverktøy and load and parse application-test.yml
@ContextConfiguration(classes = Krypteringsverktøy.class, initializers = ConfigDataApplicationContextInitializer.class)
// Specifically load application-test.yml
@ActiveProfiles(Profil.TEST)
public class KrypteringsverktøyTest {

  @Value("${KRYPTERINGSPASSORD}") // Reads from application-test.yml
  private String krypteringsPassord;
  @Value("${KRYPTERINGSSALT}") // Reads from application-test.yml
  private String krypteringsSalt;

  @BeforeEach
  void setUp() {
    // Now need for manual constructor initialization as Spring will inject the values
    // into the fields because proper annotation makes it an integration test that
    // starts a Spring ApplicationContext.
  }

  @Test
  void vedÅDekryptereEnKryptertPersonidentSkalViFåDenOriginaleIdenten() {

    // gitt
    var personident = "12345678910";

    // hvis
    var kryptertPersonident = Krypteringsverktøy.kryptere(personident);

    // så
    // Kryptert personident skal være ulik originalen
    assertThat(kryptertPersonident).isNotEqualTo(personident);

    var dekryptertPersonident = Krypteringsverktøy.dekryptere(kryptertPersonident);

    // Dekrypterer den krypterte personidenten gir originalen
    Assertions.assertEquals(personident, dekryptertPersonident);
  }
}
