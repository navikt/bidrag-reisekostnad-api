package no.nav.bidrag.reisekostnad.api;

import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.ISSUER_TOKENX;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.SIKKER_LOGG;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.api.dto.inn.NyForespørselDto;
import no.nav.bidrag.reisekostnad.api.dto.ut.BrukerinformasjonDto;
import no.nav.bidrag.reisekostnad.konfigurasjon.Tokeninfo;
import no.nav.bidrag.reisekostnad.tjeneste.ReisekostnadApiTjeneste;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/reisekostnad")
@ProtectedWithClaims(issuer = ISSUER_TOKENX)
public class ReisekostnadApiKontroller {

  @Autowired
  private ReisekostnadApiTjeneste reisekostnadApiTjeneste;

  @GetMapping(value = "/brukerinformasjon")
  @Operation(description = "Hente familierelasjoner for pålogget person samt evnt aktive fordelingsforespørsler",
      security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Ingen feil ved henting av brukerinformasjon"),
      @ApiResponse(responseCode = "400", description = "Ugyldig fødselsnummer"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke fødselsnummer"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<BrukerinformasjonDto> henteBrukerinformasjon() {
    log.info("Henter brukerinformasjon");
    var personident = Tokeninfo.Companion.hentPaaloggetPerson();
    SIKKER_LOGG.info("Henter brukerinformasjon for person med ident {}", personident);
    var respons = reisekostnadApiTjeneste.henteBrukerinformasjon(personident);
    var statuskode = respons.getResponseEntity().getStatusCode();
    SIKKER_LOGG.info("Hent brukerinformasjonstjenesten svarte med httpkode {} person med ident {}", statuskode, personident);
    return new ResponseEntity<>(respons.getResponseEntity().getBody(), statuskode);
  }

  @PostMapping("/forespoersel/ny")
  @Operation(description = "Opprette forespørsel om fordeling av reisekostnader",
      security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Forespørsel opprettet"),
      @ApiResponse(responseCode = "400", description = "Feil opplysninger oppgitt"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke fødselsnummer eller navn"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<Void> oppretteForespørselOmFordelingAvReisekostnader(@Valid @RequestBody NyForespørselDto nyForespørselDto) {
    log.info("Oppretter forespørsel om fordeling av reisekostnader for et sett med barn");
    var personidentHovedpart = Tokeninfo.Companion.hentPaaloggetPerson();
    SIKKER_LOGG.info("Oppretter forespørsel om fordeling av reisekostnader for hovedperson med ident {}", personidentHovedpart);
    var respons = reisekostnadApiTjeneste.oppretteForespørselOmFordelingAvReisekostnader(personidentHovedpart,
        nyForespørselDto.getIdenterBarn());
    return new ResponseEntity<>(respons.getResponseEntity().getStatusCode());
  }

  @PutMapping("/forespoersel/samtykke")
  @Operation(description = "Oppdatere forespørsel med motparts samtykke til fordeling av reisekostnader",
      security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Forespørsel oppdatert uten feil"),
      @ApiResponse(responseCode = "400", description = "Feil opplysninger oppgitt"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke forespørsel"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<Void> giSamtykkeTilFordelingAvReisekostnader(
      @Parameter(name = "id", description = "ID til forespørsel som skal oppdateres") @RequestParam(name = "id", defaultValue = "-1") int idForespørsel) {
    log.info("Gi samtykke til fordeling av reisekostnader (forespørsel med id {})", idForespørsel);
    var personidentPåloggetBruker = Tokeninfo.Companion.hentPaaloggetPerson();
    SIKKER_LOGG.info("Person med ident {} samtykker til at NAV skal fordele reisekostnader.", personidentPåloggetBruker);
    var respons = reisekostnadApiTjeneste.oppdatereForespørselMedSamtykke(idForespørsel, personidentPåloggetBruker);
    return new ResponseEntity<>(respons.getResponseEntity().getBody(), respons.getResponseEntity().getStatusCode());
  }

  @PutMapping("/forespoersel/trekke")
  @Operation(description = "Trekke forespørsel om fordeling av reisekostnader",
      security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "Forespørsel trukket uten feil"),
      @ApiResponse(responseCode = "400", description = "Feil opplysninger oppgitt"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke forespørsel"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<Void> trekkeForespørsel(
      @Parameter(name = "id", description = "ID til forespørsel som skal trekkes") @RequestParam(name = "id", defaultValue = "-1") int idForespørsel) {
    log.info("Trekke forespørsel (id: {}) om fordeling av reisekostnader", idForespørsel);
    var personidentPåloggetBruker = Tokeninfo.Companion.hentPaaloggetPerson();
    SIKKER_LOGG.info("Person med ident {} ønsker å trekke forespørsel om fordeling av reisekostnader.", personidentPåloggetBruker);
    var respons = reisekostnadApiTjeneste.trekkeForespørsel(idForespørsel, personidentPåloggetBruker);
    return new ResponseEntity<>(respons.getResponseEntity().getBody(), respons.getResponseEntity().getStatusCode());
  }
}
