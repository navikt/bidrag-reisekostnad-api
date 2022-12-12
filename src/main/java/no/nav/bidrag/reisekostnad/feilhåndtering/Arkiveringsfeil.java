package no.nav.bidrag.reisekostnad.feilhåndtering;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
@Getter
public class Arkiveringsfeil extends ReisekostnadApiFeil {

  public Arkiveringsfeil(Feilkode feilkode, HttpStatus httpStatus) {
    super(feilkode, httpStatus);
    log.warn("Det oppstod en feil ved arkivering av forespørsel");
  }
}
