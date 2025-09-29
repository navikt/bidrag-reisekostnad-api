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

  MELDING_OM_AUTOMATISK_INNSENDING(
      "Forespørsel om fordeling av reisekostnader for barn med fødselsdato %s ble automatisk sendt til behandling da barnet fylte femten år"),
  MELDING_OM_VENTENDE_FORESPØRSEL("Trykk her for å se en forespørsel du har fått om fordeling av reisekostnader for barn."),
  MELDING_TIL_FORELDRE_OM_UTLØPT_SAMTYKKEFRIST(
      "Forespørselen om fordeling av reisekostnader kan ikke behandles av Nav. Dette er fordi vi ikke har mottatt samtykke til dette."),
  MELDING_TIL_HOVEDPART_OM_TRUKKET_FORESPØRSEL("Du har trukket tilbake en forespørsel om fordeling av reisekostnader."),
  MELDING_TIL_MOTPART_OM_TRUKKET_FORESPØRSEL("En forespørsel om fordeling av reisekostnader ble trukket tilbake av den andre forelderen."),
  MELDING_TIL_HOVEDPART_OM_AVSLÅTT_SAMTYKKE("Den andre forelderen samtykket ikke til at Nav skal behandle fordeling av reisekostnader"),
  MELDING_TIL_HOVEDPART_OM_FORESPØRSEL_SOM_VENTER_PÅ_SAMTYKKE(
      "Du har sendt forespørsel om fordeling av reisekostnader for barn under 15 år. Forespørselen venter på samtykke fra den andre forelderen."),
  MELDING_TIL_MOTPART_OM_AVSLÅTT_SAMTYKKE("NDu har avslått en forespørsel om fordeling av reisekostnader.");

  private String tekst;

  Melding(String tekst) {
    this.tekst = tekst;
  }

  public String getTekst() {
    return this.tekst;
  }
}
