package no.nav.bidrag.reisekostnad.database.dao;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.nav.bidrag.reisekostnad.database.datamodell.Forelder;
import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface ForelderDao extends CrudRepository<Forelder, Integer> {

  Set<Forelder> findAll();

  @Query("select f from Forelder  f where f.personident is not null and f.personident = :personident")
  Optional<Forelder> finnMedPersonident(String personident);

  default Set<Forelder> henteForeldreUtenTilknytningTilAktiveForespørsler(LocalDate forespørslerDeaktivertFør) {
    var alleForeldre = findAll();

    var sletteklareForeldre = alleForeldre.stream()
        .filter(f -> erAnonymiseringsklar(f.getForespørslerHovdedpart(), forespørslerDeaktivertFør))
        .filter(f -> erAnonymiseringsklar(f.getForespørslerMotpart(), forespørslerDeaktivertFør))
        .collect(Collectors.toSet());

    return sletteklareForeldre;
  }

  default boolean erAnonymiseringsklar(Set<Forespørsel> forespørsler, LocalDate deaktivertFør) {
    return forespørsler == null
        || forespørsler.size() < 1
        || forespørsler.stream()
        .filter(f -> f.getDeaktivert() == null || deaktivertFør.atStartOfDay().isBefore(f.getDeaktivert())).findFirst().isEmpty();
  }
}
