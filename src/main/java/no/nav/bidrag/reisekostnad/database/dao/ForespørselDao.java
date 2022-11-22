package no.nav.bidrag.reisekostnad.database.dao;

import java.util.Set;
import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForespørselDao extends CrudRepository<Forespørsel, Integer> {

  @Query("select f from Forespørsel f where f.hovedpart.personident = :personident and f.deaktivert is null")
  Set<Forespørsel> henteAktiveForespørslerHvorPersonErHovedpart(String personident);

  @Query("select f from Forespørsel f where f.motpart.personident = :personident and f.deaktivert is null")
  Set<Forespørsel> henteAktiveForespørslerHvorPersonErMotpart(String personident);

}
