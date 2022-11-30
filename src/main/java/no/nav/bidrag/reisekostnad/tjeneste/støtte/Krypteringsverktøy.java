package no.nav.bidrag.reisekostnad.tjeneste.støtte;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
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
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.InternFeil;
import org.hibernate.validator.HibernateValidator;
import org.springframework.stereotype.Component;

@Component
public class Krypteringsverktøy {

  private static final String ALGORITME_ADVANECD_EMCRYPTION_STANDARD = "AES";
  private static final String KRYPTERINGSALGORITME = "PBKDF2WithHmacSHA256";
  private static final String TRANSFORMERINGSALGORITME = ALGORITME_ADVANECD_EMCRYPTION_STANDARD + "/CBC/PKCS5Padding";

  private static final String KRYPTERINGSSALT = "iwianRpIjoiZDBhN2YyNWEtNGI2ZS00MDQyL";

  private static final String PASSORD = "The owls Are N0T what tHey seeM";

  static String password = "baeldung";
  static String salt = "12345678";

  public static String kryptere(String ikkeKryptertStreng) {
    try {
      var cipher = Cipher.getInstance(TRANSFORMERINGSALGORITME);
      cipher.init(Cipher.ENCRYPT_MODE, henteHemmeligNøkkel(), generereIv());
      return Base64.getEncoder().encodeToString(cipher.doFinal(ikkeKryptertStreng.getBytes()));
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
    bytes = "bW2IK20bbZ_UW-CV".getBytes();
    return new IvParameterSpec(bytes);
  }

  private static SecretKey henteHemmeligNøkkel() {
    try {
      var factory = SecretKeyFactory.getInstance(KRYPTERINGSALGORITME);
      var spec = new PBEKeySpec(PASSORD.toCharArray(), KRYPTERINGSSALT.getBytes(), 65536, 256);
      return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), ALGORITME_ADVANECD_EMCRYPTION_STANDARD);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new InternFeil(Feilkode.KRYPTERING, e);
    }
  }
}
