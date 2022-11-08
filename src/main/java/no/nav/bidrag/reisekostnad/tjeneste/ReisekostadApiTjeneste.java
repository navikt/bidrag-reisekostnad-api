package no.nav.bidrag.reisekostnad.tjeneste;

import no.nav.bidrag.reisekostnad.api.dto.BrukerinformasjonDto;
import org.springframework.stereotype.Service;

@Service
public class ReisekostadApiTjeneste {

  public BrukerinformasjonDto henteBrukerinformasjon(String fnrPaaloggetBruker) {
    return BrukerinformasjonDto.builder().build();
  }
}
