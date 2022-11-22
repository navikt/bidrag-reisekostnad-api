package no.nav.bidrag.reisekostnad.feilhåndtering;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@AllArgsConstructor
@RestControllerAdvice
public class FeilhåndteringRestkontroller {

  @ResponseBody
  @ExceptionHandler(Valideringsfeil.class)
  protected ResponseEntity<?> håndtereValideringsfeil(Valideringsfeil valideringsfeil) {
    log.warn("Validering av brukerinput feilet med kode: {}", valideringsfeil.getFeilkode());
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .header(HttpHeaders.WARNING, valideringsfeil.getFeilkode().toString())
        .build();
  }

  @ResponseBody
  @ExceptionHandler(DataAccessException.class)
  protected ResponseEntity<?> håndtereDatatilgangsfeil(DataAccessException dataAccessException) {
    log.error("En feil oppstod i kommunikasjon med databasen: {}.", dataAccessException.getMessage());
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .header(HttpHeaders.WARNING, dataAccessException.getMessage())
        .build();

  }

  @ResponseBody
  @ExceptionHandler(Persondatafeil.class)
  protected ResponseEntity<?> handleFeilNavnOppgittException(Persondatafeil e) {
    return ResponseEntity
        .status(e.getHttpStatus())
        .header(HttpHeaders.WARNING, e.getFeilkode().toString())
        .build();
  }
}
