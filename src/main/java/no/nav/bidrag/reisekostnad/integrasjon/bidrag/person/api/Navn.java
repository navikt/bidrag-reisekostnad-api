package no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Navn {

  String fornavn;
  String mellomnavn;
  String etternavn;
}
