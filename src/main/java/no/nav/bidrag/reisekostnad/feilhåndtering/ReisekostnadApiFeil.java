package no.nav.bidrag.reisekostnad.feilhåndtering;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class ReisekostnadApiFeil extends RuntimeException{

  protected final Feilkode feilkode;
  protected final HttpStatus httpStatus;

  public ReisekostnadApiFeil(Feilkode feilkode, HttpStatus httpStatus) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
    this.httpStatus = httpStatus;
  }
}
