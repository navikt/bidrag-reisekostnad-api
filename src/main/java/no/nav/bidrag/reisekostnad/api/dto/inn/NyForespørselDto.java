package no.nav.bidrag.reisekostnad.api.dto.inn;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Value;
import org.springframework.validation.annotation.Validated;

@Value
@Schema
@Validated
public class NyForespørselDto {

  @NotNull
  @Parameter(description = "Koder som identifiserer hvilke barn reisekostnader skal fordeles for", example = "e0TT/+3WKAEx1KaBSH+b+A==")
  Set<String> identerBarn;
}
