package no.nav.bidrag.reisekostnad.api.dto.ut;

import io.swagger.v3.oas.annotations.Parameter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import no.nav.bidrag.reisekostnad.database.datamodell.Deaktivator;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ForespørselDto {

  @Parameter(description = "Identifikator for forespørselen")
  private int id;
  @Parameter(description = "Angir om forespørselen krever samtykke fra motpart")
  private boolean kreverSamtykke;
  @Parameter(description = "Liste med barn forespørselen gjelder for")
  private Set<PersonDto> barn;
  @Parameter(description = "Forelder som har opprettet forespørselen")
  private PersonDto hovedpart;
  @Parameter(description = "Forelder som reisekostnader skal fordeles med")
  private PersonDto motpart;
  @Parameter(description = "Tidspunkt for oppretelse av forespørselen")
  private LocalDateTime opprettet;
  @Parameter(description = "Tidspunkt samtykke ble gitt")
  private LocalDateTime samtykket;
  @Parameter(description = "Forespørselen må samtykkes innen")
  private LocalDate samtykkefrist;
  @Parameter(description = "Tidspunkt for journalføring")
  private LocalDateTime journalført;
  @Parameter(description = "Tidspunkt for deaktivering")
  private LocalDateTime deaktivert;
  @Parameter(description = "Angir hvilken rolle har deaktivert forespørselen")
  private Deaktivator deaktivertAv;
}
