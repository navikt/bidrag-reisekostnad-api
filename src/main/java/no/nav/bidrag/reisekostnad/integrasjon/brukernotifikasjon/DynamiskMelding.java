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
      "Den andre forelderen samtykket ikke til at NAV behandler fordeling av reisekostnade"),
  MELDING_TIL_MOTPART_OM_AVSLÅTT_SAMTYKKE("Du har avslått en forespørsel om fordeling av reisekostnader."),
  MELDING_OM_MANGLENDE_SAMTYKKE(
      "Aksjon kreves: Farskapserklæring opprettet den %s for barn med %s er ikke ferdigstilt. Våre systemer mangler informasjon om at far har signert. Far må logge inn på Farskapsportal og forsøke å signere eller oppdatere status på ny. Ta kontakt med NAV ved problemer."),
  MELDING_OM_IKKE_UTFOERT_SAMTYKKEOPPGAVE(
      "Motpart har ikke gitt samtykke innen firsten til at NAV skal behandle fordeling av reisekostnader for felles barn.");
  private String tekst;

  Melding(String tekst) {
    this.tekst = tekst;
  }

  public String getTekst() {
    return this.tekst;
  }
}
