package no.nav.bidrag.reisekostnad.database.dao;

import java.util.Optional;
import javax.transaction.Transactional;
import no.nav.bidrag.reisekostnad.database.datamodell.Forelder;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ForelderDao extends CrudRepository<Forelder, Integer> {

  @Query("select f from Forelder  f where f.personident = :personident")
  Optional<Forelder> finnMedPersonident(String personident);

}
