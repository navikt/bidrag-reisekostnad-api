package no.nav.bidrag.reisekostnad.database.datamodell;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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
public class Barn implements Person, Serializable {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private int id;

  private String personident;
  private LocalDate fødselsdato;
  private LocalDateTime anonymisert;

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
