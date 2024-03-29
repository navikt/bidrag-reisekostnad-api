package no.nav.bidrag.reisekostnad.database.dao;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ForespørselDao extends JpaRepository<Forespørsel, Integer> {

  @Query("select f from Forespørsel f where f.hovedpart.personident = :personidentHovedpart and f.anonymisert is null")
  Set<Forespørsel> henteForespørslerForHovedpart(String personidentHovedpart);

  @Query("select f from Forespørsel f where f.motpart.personident = :personidentMotpart and f.anonymisert is null")
  Set<Forespørsel> henteForespørslerForMotpart(String personidentMotpart);

  @Query("select f from Forespørsel f where f.id = :idForespørsel and f.deaktivert is null")
  Optional<Forespørsel> henteAktivForespørsel(int idForespørsel);

  @Query("select f.id from Forespørsel f where f.deaktivert is null and f.opprettet < :opprettetFør")
  Set<Integer> henteIdTilAktiveForespørsler(LocalDateTime opprettetFør);

  @Query("select f.id from Forespørsel f where f.deaktivert is null and f.journalført is not null and f.opprettet < :opprettetFør")
  Set<Integer> henteIdTilAktiveJournalførteForespørsler(LocalDateTime opprettetFør);

  @Query("select f.id from Forespørsel f "
      + "where f.deaktivert is null and f.samtykket is not null and f.journalført is null")
  Set<Integer> henteAktiveOgSamtykkedeForespørslerSomErKlareForInnsending();

  @Query("select f.id from Forespørsel  f "
      + "where f.deaktivert is null and f.samtykket is null and f.journalført is null and f.kreverSamtykke = false")
  Set<Integer> henteAktiveForespørslerSomIkkeKreverSamtykkOgErKlareForInnsending();

  @Query("select f from Forespørsel f inner join f.barn b "
      + "where f.deaktivert is null and f.kreverSamtykke = true and f.samtykket is null and b.fødselsdato <= :date")
  Set<Forespørsel> henteForespørslerSomKreverSamtykkeOgInneholderBarnFødtSammeDagEllerEtterDato(LocalDate date);

  default Set<Forespørsel> henteSynligeForespørslerForHovedpart(String personident, LocalDateTime deaktivertEtter) {
    return henteForespørslerForHovedpart(personident).stream()
        .filter(f -> erSynlig(f, deaktivertEtter)).collect(Collectors.toSet());
  }

  default Set<Forespørsel> henteSynligeForespørslerForMotpart(String personident, LocalDateTime deaktivertEtter) {
    return henteForespørslerForMotpart(personident).stream()
        .filter(f -> erSynlig(f, deaktivertEtter)).collect(Collectors.toSet());
  }

  default boolean erSynlig(Forespørsel forespørsel, LocalDateTime deaktivertEtter) {

    var journalført = forespørsel.getJournalført();
    var deaktivert = forespørsel.getDeaktivert();

    return (journalført == null && (deaktivert == null || deaktivert.isAfter(deaktivertEtter))
        || !(journalført != null && deaktivert != null));
  }
}
