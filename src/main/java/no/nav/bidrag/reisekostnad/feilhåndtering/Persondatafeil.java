package no.nav.bidrag.reisekostnad.feilhåndtering;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class Persondatafeil extends ReisekostnadApiFeil {

  public Persondatafeil(Feilkode feilkode, HttpStatus httpStatus) {
    super(feilkode, httpStatus);
  }
}