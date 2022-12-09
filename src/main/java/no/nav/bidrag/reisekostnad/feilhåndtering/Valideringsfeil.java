package no.nav.bidrag.reisekostnad.feilhåndtering;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class Valideringsfeil extends ReisekostnadApiFeil {

  public Valideringsfeil(Feilkode feilkode) {
    super(feilkode, HttpStatus.BAD_REQUEST);
  }
}
