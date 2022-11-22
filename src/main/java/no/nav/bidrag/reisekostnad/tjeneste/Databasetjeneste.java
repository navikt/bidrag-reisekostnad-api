package no.nav.bidrag.reisekostnad.tjeneste;

import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.SIKKER_LOGG;

import java.time.LocalDateTime;
import java.util.Set;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.database.dao.BarnDao;
import no.nav.bidrag.reisekostnad.database.dao.ForelderDao;
import no.nav.bidrag.reisekostnad.database.dao.ForespørselDao;
import no.nav.bidrag.reisekostnad.database.datamodell.Forelder;
import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.Valideringsfeil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Databasetjeneste {

  private BarnDao barnDao;
  private ForelderDao forelderDao;
  private ForespørselDao forespørselDao;

  private Mapper mapper;

  @Autowired
  public Databasetjeneste(BarnDao barnDao, ForelderDao forelderDao, ForespørselDao forespørselDao, Mapper mapper) {
    this.barnDao = barnDao;
    this.forelderDao = forelderDao;
    this.forespørselDao = forespørselDao;
    this.mapper = mapper;
  }

  @Transactional
  public int lagreNyForespørsel(String hovedpart, String motpart, Set<String> identerBarn, boolean kreverSamtykke) {

    for (String ident : identerBarn) {
      var barn = barnDao.henteBarnTilknyttetAktivForespørsel(ident);
      if (barn.isPresent()) {
        log.warn("Validering feilet. Det finnes allerede en aktiv forespørsel for et av de oppgitte barna.");
        SIKKER_LOGG.warn("Validering feilet. Barn med ident {} er allerede tilknyttet en aktiv forespørsel", ident);
        throw new Valideringsfeil(Feilkode.VALIDERING_NY_FOREPØRSEL_BARN_I_AKTIV_FORESPØRSEL);
      }
    }

    var ekisterendeHovedpart = forelderDao.finnMedPersonident(hovedpart);
    var eksisterendeMotpart = forelderDao.finnMedPersonident(motpart);

    var nyForespørsel = Forespørsel.builder()
        .opprettet(LocalDateTime.now())
        .hovedpart(ekisterendeHovedpart.orElseGet(() -> Forelder.builder().personident(hovedpart).build()))
        .motpart(eksisterendeMotpart.orElseGet(() -> Forelder.builder().personident(motpart).build()))
        .barn(mapper.tilEntitet(identerBarn))
        .kreverSamtykke(kreverSamtykke)
        .build();

    var lagretForespørsel = forespørselDao.save(nyForespørsel);

    return lagretForespørsel.getId();
  }
}
