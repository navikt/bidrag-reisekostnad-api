package no.nav.bidrag.reisekostnad.database.datamodell;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Builder
@Getter
@Setter
@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
public class Forespørsel implements Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  @ManyToOne(cascade = CascadeType.ALL)
  private Forelder hovedpart;

  @ManyToOne(cascade = CascadeType.ALL)
  private Forelder motpart;

  @JoinColumn(name = "forespørsel_id")
  @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
  private Set<Barn> barn = new HashSet<>();

  private boolean kreverSamtykke;

  private LocalDateTime opprettet;
  private LocalDateTime samtykket;
  private LocalDate samtykkefrist;
  private LocalDateTime journalført;
  private LocalDateTime deaktivert;
  @Enumerated(EnumType.STRING)
  private Deaktivator deaktivertAv;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (barn == null ? 0 : barn.hashCode()) + (hovedpart == null ? 0 : hovedpart.hashCode()) + (motpart == null ? 0
        : motpart.hashCode());

    return result;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Forespørsel other = (Forespørsel) obj;

    if (!barn.containsAll(other.barn)) {
      return false;
    }
    if (!hovedpart.equals(other.hovedpart)) {
      return false;
    }

    if (deaktivert == null ^ other.deaktivert == null) {
      return false;
    } else if ((deaktivert != null && other.deaktivert != null) && (!deaktivert.equals(other.deaktivert))) {
      return false;
    }

    return motpart.equals(other.motpart);
  }
}
