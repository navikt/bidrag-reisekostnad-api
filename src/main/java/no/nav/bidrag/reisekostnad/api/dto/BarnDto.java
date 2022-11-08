package no.nav.bidrag.reisekostnad.api.dto;

import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDate;

public class BarnDto {

  @Parameter(description = "Barnets fornavn")
  private String fornavn;

  @Parameter(description = "Barnets fødselsdato")
  private LocalDate fødselsdato;

}
