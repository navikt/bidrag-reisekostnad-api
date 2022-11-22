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
  @Parameter(description = "Personidenter til barn som reisekostnader skal fordeles for", example = "77777700000")
  Set<String> personidenterBarn;
}
