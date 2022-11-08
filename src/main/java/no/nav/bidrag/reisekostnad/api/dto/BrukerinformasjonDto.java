package no.nav.bidrag.reisekostnad.api.dto;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Schema
@Value
@Builder
public class BrukerinformasjonDto {

  @Parameter(description = "Brukers fornavn som registrert i Folkeregisteret", example = "Kari Nordmann")
  String brukersFornavn;
  @Parameter(description = "Personen kan opprette søknad om fordeling av reisekostnader", example = "true")
  boolean kanSøkeOmFordelingAvReisekostnader;
  @Parameter(description = "Søknader personen har opprettet")
  Set<SøknadDto> søknaderSomHovedpart;
  @Parameter(description = "Søknader hvor personen er motpart")
  Set<SøknadDto> søknaderSomMotpart;
  @Parameter(description = "Brukers barn under femten år")
  Set<BarnDto> barnUnderFemtenAar;
  @Parameter(description = "Brukers barn over femten år")
  Set<BarnDto> barnOverFemtenAar;
}
