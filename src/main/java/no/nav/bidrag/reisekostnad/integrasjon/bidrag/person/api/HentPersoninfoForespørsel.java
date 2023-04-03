package no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import javax.validation.constraints.NotNull;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HentPersoninfoForespørsel {

  @NotNull
  String ident;

  String verdi = ident;
}
