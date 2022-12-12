package no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.database.datamodell.Forelder;
import no.nav.bidrag.reisekostnad.konfigurasjon.Egenskaper;
import no.nav.brukernotifikasjon.schemas.builders.BeskjedInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.BeskjedInput;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Beskjedprodusent {

  KafkaTemplate<NokkelInput, BeskjedInput> kafkaTemplate;
  URL farskapsportalUrlForside;
  URL farskapsportalUrlOversikt;
  Egenskaper egenskaper;

  public void oppretteBeskjedTilBruker(Forelder forelder, String meldingTilBruker, boolean medEksternVarsling, NokkelInput nokkel) {
    oppretteBeskjedTilBruker(forelder, meldingTilBruker, medEksternVarsling, false, nokkel);
  }

  public void oppretteBeskjedTilBruker(Forelder forelder, String meldingTilBruker, boolean medEksternVarsling, boolean lenkeTilOversikt,
      NokkelInput nokkel) {

    var farskapsportalUrl = lenkeTilOversikt ? farskapsportalUrlOversikt : farskapsportalUrlForside;

    var beskjed = oppretteBeskjed(meldingTilBruker, medEksternVarsling, farskapsportalUrl);

    try {
      kafkaTemplate.send(egenskaper.getBrukernotifikasjon().getEmneBeskjed(), nokkel, beskjed);
    } catch (Exception e) {
      log.error("Opprettelse av beskjed til forelder med id {} feilet!", forelder.getId(), e);
    }

    var medEllerUten = medEksternVarsling ? "med" : "uten";
    log.info("Beskjed {} ekstern varsling og eventId {} er sendt til forelder (id {}).", medEllerUten, nokkel.getEventId(), forelder.getId());
  }

  private BeskjedInput oppretteBeskjed(String meldingTilBruker, boolean medEksternVarsling, URL lenke) {

    return new BeskjedInputBuilder()
        .withTidspunkt(LocalDateTime.now(ZoneId.of("UTC")))
        .withEksternVarsling(medEksternVarsling)
        .withSynligFremTil(
            LocalDateTime.now(ZoneId.of("UTC")).withHour(0)
                .plusMonths(egenskaper.getBrukernotifikasjon().getSynlighetBeskjedAntallMaaneder()))
        .withSikkerhetsnivaa(egenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed())
        .withLink(lenke)
        .withTekst(meldingTilBruker)
        .build();
  }
}
