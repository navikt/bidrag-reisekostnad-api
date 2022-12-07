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
  @ExceptionHandler(InternFeil.class)
  protected ResponseEntity<?> håndtereInternFeil(InternFeil internFeil) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .header(HttpHeaders.WARNING, internFeil.getFeilkode().toString()).build();
  }

  @ResponseBody
  @ExceptionHandler(Valideringsfeil.class)
  protected ResponseEntity<?> håndtereValideringsfeil(Valideringsfeil valideringsfeil) {
    return ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .header(HttpHeaders.WARNING, valideringsfeil.getFeilkode().toString()).build();
  }

  @ResponseBody
  @ExceptionHandler(DataAccessException.class)
  protected ResponseEntity<?> håndtereDatatilgangsfeil(DataAccessException dataAccessException) {
    return ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .header(HttpHeaders.WARNING, dataAccessException.getMessage()).build();
  }

  @ResponseBody
  @ExceptionHandler(Persondatafeil.class)
  protected ResponseEntity<?> håndtereIntegrasjonsfeil(Persondatafeil e) {
    return ResponseEntity
        .status(e.getHttpStatus())
        .header(HttpHeaders.WARNING, e.getFeilkode().toString()).build();
  }
}
