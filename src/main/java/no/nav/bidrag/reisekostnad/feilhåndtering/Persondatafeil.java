package no.nav.bidrag.reisekostnad.feilhåndtering;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class Persondatafeil extends RuntimeException implements ReisekostnadApiFeil {

  private final Feilkode feilkode;
  private final HttpStatus httpStatus;

  public Persondatafeil(Feilkode feilkode, HttpStatus httpStatus) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
    this.httpStatus = httpStatus;
  }

  public @Override String getFeilmelding() {
    return this.feilkode.getBeskrivelse();
  }
}
