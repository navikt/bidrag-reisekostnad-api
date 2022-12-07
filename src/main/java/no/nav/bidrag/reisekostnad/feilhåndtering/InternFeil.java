package no.nav.bidrag.reisekostnad.feilhåndtering;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class InternFeil extends ReisekostnadApiFeil {

  public InternFeil(Feilkode feilkode) {
    super(feilkode, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public InternFeil(Feilkode feilkode, Exception e) {
    super(feilkode, HttpStatus.INTERNAL_SERVER_ERROR);
    e.printStackTrace();
  }
}