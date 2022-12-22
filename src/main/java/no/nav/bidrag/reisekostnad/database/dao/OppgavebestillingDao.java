package no.nav.bidrag.reisekostnad.database.dao;

import java.util.Optional;
import java.util.Set;
import no.nav.bidrag.reisekostnad.database.datamodell.Oppgavebestilling;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OppgavebestillingDao extends CrudRepository<Oppgavebestilling, Integer> {

  @Query("select o from Oppgavebestilling o where o.forespørsel.id = :idForespørsel and o.forelder.personident = :personident and o.eventId is not null and o.ferdigstilt is null")
  Set<Oppgavebestilling> henteAktiveOppgaver(int idForespørsel, String personident);

  @Query("select o from Oppgavebestilling o where o.ferdigstilt is null and o.forelder.personident = :personident")
  Set<Oppgavebestilling> henteAktiveOppgaver(String personident);

  @Query("select o from Oppgavebestilling o where o.eventId = :eventId")
  Optional<Oppgavebestilling> henteOppgavebestilling(String eventId);

  @Query("select o from Oppgavebestilling o where o.ferdigstilt is null and o.forespørsel.deaktivert is not null")
  Set<Oppgavebestilling> henteAktiveOppgaverKnyttetTilDeaktiverteForespørsler();

}
