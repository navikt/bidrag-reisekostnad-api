package no.nav.bidrag.reisekostnad.feilhåndtering;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Slf4j
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class Valideringsfeil extends ReisekostnadApiFeil {

  public Valideringsfeil(Feilkode feilkode) {
    super(feilkode, HttpStatus.BAD_REQUEST);
    log.warn("Validering av oppgitte brukerdata feilet.");
  }
}

