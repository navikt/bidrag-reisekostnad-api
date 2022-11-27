package no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Familiemedlem {

  String ident;
  String fornavn;
  String mellomnavn;
  String etternavn;
  String foedselsdato;
  String diskresjonskode;
}
