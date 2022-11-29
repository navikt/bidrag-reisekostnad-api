package no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
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
  @Getter(AccessLevel.NONE)
  Kjønn kjønn;
  LocalDate doedsdato;
  LocalDate foedselsdato;
  String diskresjonskode;

  public Kjønn getKjønn() {
    return kjønn == null ? Kjønn.UKJENT : kjønn;
  }
}
