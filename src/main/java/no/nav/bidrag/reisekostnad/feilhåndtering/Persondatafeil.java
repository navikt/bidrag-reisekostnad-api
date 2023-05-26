package no.nav.bidrag.reisekostnad.feilhåndtering;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class Persondatafeil extends ReisekostnadApiFeil {

  public Persondatafeil(Feilkode feilkode, HttpStatusCode httpStatus) {
    super(feilkode, httpStatus);
  }
}