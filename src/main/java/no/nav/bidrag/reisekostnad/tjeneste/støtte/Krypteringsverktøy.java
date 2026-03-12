package no.nav.bidrag.reisekostnad.tjeneste.støtte;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.InternFeil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class Krypteringsverktøy {

  private static final String ADVANCED_ENCRYPTION_STANDARD_ALGORITME = "AES";
  private static final String KRYPTERINGSALGORITME = "PBKDF2WithHmacSHA256";
  private static final String TRANSFORMERINGSALGORITME = ADVANCED_ENCRYPTION_STANDARD_ALGORITME + "/CBC/PKCS5Padding";
  private static String krypteringsPassord;
  private static String krypteringsSalt;

  public Krypteringsverktøy(@Value("${KRYPTERINGSPASSORD:MISSING}") String krypteringsPassord,
                            @Value("${KRYPTERINGSSALT:MISSING}") String krypteringsSalt) {
    if ("MISSING".equals(krypteringsPassord) || "MISSING".equals(krypteringsSalt)) {
      log.error("KRYPTERINGSPASSORD or KRYPTERINGSSALT was not found in the environment!");
      throw new IllegalStateException("KRYPTERINGSPASSORD or KRYPTERINGSSALT was not found in the environment!");
    }
    this.krypteringsPassord = krypteringsPassord.strip();
    this.krypteringsSalt = krypteringsSalt.strip();
  }

  public static String kryptere(String ikkeKryptertStreng) {
    try {
      var cipher = Cipher.getInstance(TRANSFORMERINGSALGORITME);
      cipher.init(Cipher.ENCRYPT_MODE, henteHemmeligNøkkel(), generereIv());
      return Base64.getEncoder().encodeToString(cipher.doFinal(ikkeKryptertStreng.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException |
             IllegalBlockSizeException e) {
      e.printStackTrace();
      throw new InternFeil(Feilkode.KRYPTERING, e);
    }
  }

  public static String dekryptere(String kryptertStreng) {
    try {
      var cipher = Cipher.getInstance(TRANSFORMERINGSALGORITME);
      cipher.init(Cipher.DECRYPT_MODE, henteHemmeligNøkkel(), generereIv());
      return new String(cipher.doFinal(Base64.getDecoder().decode(kryptertStreng)));
    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidAlgorithmParameterException | InvalidKeyException | BadPaddingException |
             IllegalBlockSizeException e) {
      e.printStackTrace();
      throw new InternFeil(Feilkode.KRYPTERING, e);
    }
  }

  private static IvParameterSpec generereIv() {
    var bytes = new byte[16];
    bytes = "bW2IK20bbZ_UW-CV".getBytes(StandardCharsets.UTF_8);
    return new IvParameterSpec(bytes);
  }

  private static SecretKey henteHemmeligNøkkel() {
    try {
      var factory = SecretKeyFactory.getInstance(KRYPTERINGSALGORITME);
      var spec = new PBEKeySpec(krypteringsPassord.toCharArray(), krypteringsSalt.getBytes(StandardCharsets.UTF_8), 65536, 256);
      return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), ADVANCED_ENCRYPTION_STANDARD_ALGORITME);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new InternFeil(Feilkode.KRYPTERING, e);
    }
  }
}
