package no.nav.bidrag.reisekostnad.feilhåndtering;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public abstract class ReisekostnadApiFeil extends RuntimeException{

  protected final Feilkode feilkode;
  protected final HttpStatusCode httpStatus;

  public ReisekostnadApiFeil(Feilkode feilkode, HttpStatusCode httpStatus) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
    this.httpStatus = httpStatus;
  }
}