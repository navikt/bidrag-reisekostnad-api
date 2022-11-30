package no.nav.bidrag.reisekostnad.feilhåndtering;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class Valideringsfeil extends RuntimeException implements ReisekostnadApiFeil {

  private final Feilkode feilkode;

  public Valideringsfeil(Feilkode feilkode) {
    super(feilkode.getBeskrivelse());
    this.feilkode = feilkode;
  }

  public Feilkode getFeilkode() {
    return this.feilkode;
  }

  public @Override String getFeilmelding() {
    return this.feilkode.getBeskrivelse();
  }

}

