package no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MotpartBarnRelasjon {

  public enum Relasjon {MOR, FAR, BARN}

  Relasjon relasjonMotpart;
  Familiemedlem motpart;
  @Builder.Default
  List<Familiemedlem> fellesBarn = new ArrayList<>();
}


