package no.nav.bidrag.reisekostnad.feilhåndtering;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InternFeil extends RuntimeException implements ReisekostnadApiFeil {

  private final Feilkode feilkode;

  public InternFeil(Feilkode feilkode, Exception e) {
    super(feilkode.getBeskrivelse(), e);
    this.feilkode = feilkode;
  }

  public @Override String getFeilmelding() {
    return this.feilkode.getBeskrivelse();
  }

}
