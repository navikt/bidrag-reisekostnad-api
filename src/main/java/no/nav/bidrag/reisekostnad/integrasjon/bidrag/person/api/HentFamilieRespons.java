package no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HentFamilieRespons {

  Familiemedlem person;
  @Builder.Default
  List<MotpartBarnRelasjon> personensMotpartBarnRelasjon = new ArrayList<>();
}
