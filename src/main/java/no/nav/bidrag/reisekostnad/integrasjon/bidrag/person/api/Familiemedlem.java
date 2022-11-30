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
  Kjønn kjoenn;
  LocalDate doedsdato;
  LocalDate foedselsdato;
  String diskresjonskode;

  public Kjønn getKjoenn() {
    return kjoenn == null ? Kjønn.UKJENT : kjoenn;
  }
}
