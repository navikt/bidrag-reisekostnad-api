package no.nav.bidrag.reisekostnad.api.dto.ut;

import lombok.Builder;
import lombok.Value;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;

@Value
@Builder
public class ReisekostnadApiFeilrespons {

  Feilkode feilkode;
  String feilkodebeskrivelse;
}