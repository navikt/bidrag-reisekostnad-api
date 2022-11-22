package no.nav.bidrag.reisekostnad.api.dto.ut;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MotpartDto {

  @Parameter(description = "Forelderens motpart")
  private PersonDto motpart;
  @Builder.Default
  @Parameter(description = "Forelder og motparts felles barn under 15 år")
  private Set<PersonDto> fellesBarnUnder15År = new HashSet<>();
}
