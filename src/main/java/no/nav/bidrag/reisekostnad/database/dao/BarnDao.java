package no.nav.bidrag.reisekostnad.database.dao;

import java.util.Optional;
import javax.transaction.Transactional;
import no.nav.bidrag.reisekostnad.database.datamodell.Barn;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface BarnDao extends CrudRepository<Barn, Integer> {

  @Query("select b from Barn b where b.personident = :personidentBarn and b.forespørsel.deaktivert is null")
  Optional<Barn> henteBarnTilknyttetAktivForespørsel(String personidentBarn);

}
