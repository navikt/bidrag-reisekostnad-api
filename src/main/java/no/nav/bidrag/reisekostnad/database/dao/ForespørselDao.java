package no.nav.bidrag.reisekostnad.database.dao;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForespørselDao extends CrudRepository<Forespørsel, Integer> {

  @Query("select f from Forespørsel f where f.hovedpart.personident = :personident and (f.deaktivert is null or f.deaktivert > :deaktivertEtter)")
  Set<Forespørsel> henteSynligeForespørslerHvorPersonErHovedpart(String personident, LocalDateTime deaktivertEtter);

  @Query("select f from Forespørsel f where f.motpart.personident = :personident and (f.deaktivert is null or f.deaktivert > :deaktivertEtter)")
  Set<Forespørsel> henteSynligeForespørslerHvorPersonErMotpart(String personident, LocalDateTime deaktivertEtter);

  @Query("select f from Forespørsel f "
      + "where f.id = :idForespørsel and f.deaktivert is null")
  Optional<Forespørsel> henteAktivForespørsel(int idForespørsel);

  @Query("select f from Forespørsel f "
      + "where f.deaktivert is null and f.samtykket is not null and f.journalført is null")
  Set<Integer> henteAktiveOgSamtykkedeForespørslerSomErKlareForInnsending();

  @Query("select f from Forespørsel  f "
      + "where f.deaktivert is null and f.samtykket is null and f.journalført is null and f.kreverSamtykke is false")
  Set<Integer> henteAktiveForespørslerSomIkkeKreverSamtykkOgErKlareForInnsending();
}
