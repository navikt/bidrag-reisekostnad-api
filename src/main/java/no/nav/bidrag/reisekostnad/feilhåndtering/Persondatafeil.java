package no.nav.bidrag.reisekostnad.feilhåndtering;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
@Getter
public class Persondatafeil extends ReisekostnadApiFeil {

  public Persondatafeil(Feilkode feilkode, HttpStatus httpStatus) {
    super(feilkode, httpStatus);
  }
}
