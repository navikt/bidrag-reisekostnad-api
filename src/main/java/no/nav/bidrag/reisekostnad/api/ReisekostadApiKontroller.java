package no.nav.bidrag.reisekostnad.api;

import static no.nav.bidrag.reisekostnad.BidragReisekostnadApiKonfigurasjon.ISSUER_TOKENX;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.BidragReisekostnadApiKonfigurasjon;
import no.nav.bidrag.reisekostnad.api.dto.BrukerinformasjonDto;
import no.nav.bidrag.reisekostnad.tjeneste.ReisekostadApiTjeneste;
import no.nav.security.token.support.core.api.ProtectedWithClaims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/reisekostnad")
@ProtectedWithClaims(issuer = ISSUER_TOKENX)
public class ReisekostadApiKontroller {

  @Autowired
  private ReisekostadApiTjeneste reisekostadApiTjeneste;
  @Autowired
  private BidragReisekostnadApiKonfigurasjon.OidcTokenSubjectExtractor oidcTokenSubjectExtractor;

  @GetMapping(value = "/brukerinformasjon")
  @Operation(description = "Avgjør foreldrerolle til person. Henter ventende farskapserklæringer. Henter nyfødte barn",
      security = {@SecurityRequirement(name = "bearer-key")})
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Ingen feil ved bestemming av rolle"),
      @ApiResponse(responseCode = "400", description = "Ugyldig fødselsnummer"),
      @ApiResponse(responseCode = "401", description = "Sikkerhetstoken mangler, er utløpt, eller av andre årsaker ugyldig"),
      @ApiResponse(responseCode = "404", description = "Fant ikke fødselsnummer"),
      @ApiResponse(responseCode = "500", description = "Serverfeil"),
      @ApiResponse(responseCode = "503", description = "Tjeneste utilgjengelig")})
  public ResponseEntity<BrukerinformasjonDto> henteBrukerinformasjon() {
    log.info("Henter brukerinformasjon");
    var personident = oidcTokenSubjectExtractor.hentPaaloggetPerson();
    BidragReisekostnadApiKonfigurasjon.SIKKER_LOGG.info("Henter brukerinformasjon for person med ident {}", personident);
    var brukerinformasjon = reisekostadApiTjeneste.henteBrukerinformasjon(personident);
    return new ResponseEntity<>(brukerinformasjon, HttpStatus.OK);
  }

}
