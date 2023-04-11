package no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import javax.validation.constraints.NotNull;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HentPersoninfoForespørsel {

  @NotNull
  String ident;

  @NotNull
  String verdi;

  public HentPersoninfoForespørsel(String verdi) {
    this.ident = verdi;
    this.verdi = verdi;
  }
}
