package no.nav.bidrag.reisekostnad.api.dto.ut;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.HashSet;
import java.util.Set;
import lombok.Builder;
import lombok.Value;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api.Kjønn;

@Schema
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BrukerinformasjonDto {

  @Parameter(description = "Brukers fornavn som registrert i Folkeregisteret", example = "Kari Nordmann")
  String fornavn;

  @Parameter(description = "Brukers gjeldende kjønn som registrert i Folkeregisteret", example = "KVINNE")
  Kjønn kjønn;

  @Builder.Default
  @Parameter(description = "Brukers gjeldende kjønn som registrert i Folkeregisteret", example = "false")
  boolean harDiskresjon = false;

  @Builder.Default
  @Parameter(description = "Personen kan opprette søknad om fordeling av reisekostnader", example = "true")
  boolean kanSøkeOmFordelingAvReisekostnader = true;

  @Builder.Default
  @Parameter(description = "Minst en av personens familieenheter har blitt skjult pga diskresjon", example = "false")
  boolean harSkjulteFamilieenheterMedDiskresjon = false;

  @Builder.Default
  @Parameter(description = "Forespørsler personen har opprettet")
  Set<ForespørselDto> forespørslerSomHovedpart = new HashSet<>();

  @Builder.Default
  @Parameter(description = "Forespørsler hvor personen er motpart")
  Set<ForespørselDto> forespørslerSomMotpart = new HashSet<>();

  @Builder.Default
  @Parameter(description = "Brukers motparter med felles barn under femten år")
  Set<MotpartDto> motparterMedFellesBarnUnderFemtenÅr = new HashSet<>();

  @Builder.Default
  @Parameter(description = "Brukers barn over femten år")
  Set<PersonDto> barnMinstFemtenÅr = new HashSet<>();
}
