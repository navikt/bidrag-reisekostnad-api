package no.nav.bidrag.reisekostnad.database.datamodell;

import java.io.Serializable;
import java.time.LocalDate;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.NaturalId;

@Entity
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Barn implements Person, Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  private String personident;
  private LocalDate fødselsdato;

  @ManyToOne(cascade = CascadeType.PERSIST)
  private Forespørsel forespørsel;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result =
        prime * result + (personident == null ? 0 : personident.hashCode());

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
    final Barn other = (Barn) obj;

    if (personident == null) {
      return other.personident == null;
    }

    return other.personident != null ? personident.equals(other.personident) : false;
  }

  @Override
  public String toString() {
    if (personident != null) {
      return "Barn med personident som starter med: " + personident.substring(0, 6);
    }
    return "Objektet mangler data";
  }
}
