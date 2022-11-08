package no.nav.bidrag.reisekostnad.database.datamodell;

import java.time.LocalDateTime;
import java.util.Set;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Søknad {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @OneToMany
  private Set<Barn> barn;

  @ManyToOne
  private Forelder hovedpart;

  @ManyToOne
  private Forelder motpart;

  private LocalDateTime opprettet;
  private LocalDateTime samtykket;
  private LocalDateTime journalfoert;





}
