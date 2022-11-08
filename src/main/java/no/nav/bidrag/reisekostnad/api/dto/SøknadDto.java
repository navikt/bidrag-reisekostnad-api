package no.nav.bidrag.reisekostnad.api.dto;

import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.nav.bidrag.reisekostnad.database.datamodell.Barn;
import no.nav.bidrag.reisekostnad.database.datamodell.Forelder;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SøknadDto {

  @Parameter(description = "Liste med barn søknaden gjelder for")
  private Set<Barn> barn;
  @Parameter(description = "Forelder som har opprettet søknaden")
  private Forelder hovedpart;
  @Parameter(description = "Forelder som reisekostnader skal fordeles med")
  private Forelder motpart;
  @Parameter(description = "Tidspunkt for oppretelse av søknad")
  private LocalDateTime opprettet;
  @Parameter(description = "Tidspunkt samtykke ble gitt")
  private LocalDateTime samtykket;
  @Parameter(description = "Tidspunkt for journalføring")
  private LocalDateTime journalfoert;
}
