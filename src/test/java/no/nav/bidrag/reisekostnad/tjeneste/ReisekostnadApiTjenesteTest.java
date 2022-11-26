package no.nav.bidrag.reisekostnad.tjeneste;

import no.nav.bidrag.reisekostnad.BidragReisekostnadApiTestapplikasjon;
import no.nav.bidrag.reisekostnad.database.dao.ForespørselDao;
import no.nav.bidrag.reisekostnad.konfigurasjon.Profil;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Mapper;
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@DisplayName("ReisekostnadApiTjeneste")
@ActiveProfiles(Profil.TEST)
@EnableMockOAuth2Server
@AutoConfigureTestDatabase(replace = Replace.ANY)
@SpringBootTest(classes = {Mapper.class, ForespørselDao.class, BidragReisekostnadApiTestapplikasjon.class})
public class ReisekostnadApiTjenesteTest {

  @Autowired
  private ReisekostadApiTjeneste reisekostadApiTjeneste;



}
