package no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DynamiskMelding {

  private final Melding melding;

  private final List<String> meldingsparametre;

  public DynamiskMelding(Melding melding) {
    this.melding = melding;
    this.meldingsparametre = new ArrayList<>();
  }

  public String hentFormatertMelding() {
    return meldingsparametre.size() > 0 ? String.format(melding.getTekst(), meldingsparametre) : melding.getTekst();
  }
}

enum Melding {

  MELDING_OM_VENTENDE_FORESPØRSEL("Trykk her for å se en forespørsel du har fått om fordeling av reisekostnader for barn."),
  MELDING_TIL_HOVEDPART_OM_AVSLÅTT_SAMTYKKE(
      "Den andre forelderen samtykket ikke til at NAV skal behandle fordeling av reisekostnader"),
  MELDING_TIL_MOTPART_OM_AVSLÅTT_SAMTYKKE("Du har avslått en forespørsel om fordeling av reisekostnader."),
  MELDING_TIL_FORELDRE_OM_UTLØPT_SAMTYKKEFRIST("Forespørselen om fordeling av reisekostnader kan ikke behandles av NAV. Dette er fordi vi ikke har mottatt samtykke til dette."),
  MELDING_OM_AUTOMATISK_INNSENDING("Forespørsel om fordeling av reisekostnader for barn med fødselsdato %s ble automatisk sendt til behandling da barnet fylte femten år");
  private String tekst;

  Melding(String tekst) {
    this.tekst = tekst;
  }

  public String getTekst() {
    return this.tekst;
  }
}
