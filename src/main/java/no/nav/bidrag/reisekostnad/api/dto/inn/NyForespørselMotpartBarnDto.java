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
@Deprecated
public class NyForespørselMotpartBarnDto {

  @NotNull
  @Parameter(description = "Motpartens personident", example = "11111122222")
  String personidentMotpart;
  @NotNull
  @Parameter(description = "Personidenter til felles barn som reisekostnader skal fordeles for", example = "77777700000")
  Set<String> personidenterBarn;
}
