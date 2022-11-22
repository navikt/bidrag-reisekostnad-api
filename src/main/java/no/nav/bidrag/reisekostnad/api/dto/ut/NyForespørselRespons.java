package no.nav.bidrag.reisekostnad.api.dto.ut;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Deprecated
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NyForespørselRespons {

  int idForespørselForBarnOver15År;
  int idForespørselForBarnUnder15År;
}
