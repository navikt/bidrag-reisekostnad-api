package no.nav.bidrag.reisekostnad.api.dto.ut;

import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonDto {

  @Parameter(description = "Identifikator for personen", example="e0TT/+3WKAEx1KaBSH+b+A==")
  private String ident;
  @Parameter(description = "Personens fornavn", example = "Bob Builder")
  private String fornavn;
  @Parameter(description = "Personens kortnavn", example = "Bob Builder")
  private String kortNavn;
  @Parameter(description = "Personens fødselsdato", example = "01012000")
  private LocalDate fødselsdato;
}
