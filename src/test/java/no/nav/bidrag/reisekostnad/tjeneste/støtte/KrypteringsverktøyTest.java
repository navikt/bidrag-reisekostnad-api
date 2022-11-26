package no.nav.bidrag.reisekostnad.tjeneste.støtte;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class KrypteringsverktøyTest {

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
