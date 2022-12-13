package no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HentPersoninfoRespons {

  String fornavn;
  String kortNavn;
  LocalDate foedselsdato;
}
