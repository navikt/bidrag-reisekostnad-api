package no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon;

import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.SIKKER_LOGG;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
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

  public void oppretteBeskjedTilBruker(String personidentForelder, DynamiskMelding meldingTilBruker, boolean medEksternVarsling, NokkelInput nokkel) {
    oppretteBeskjedTilBruker(personidentForelder, meldingTilBruker, medEksternVarsling, false, nokkel);
  }

  public void oppretteBeskjedTilBruker(String personidentForelder, DynamiskMelding meldingTilBruker, boolean medEksternVarsling,
      boolean lenkeTilOversikt,
      NokkelInput nokkel) {

    var farskapsportalUrl = lenkeTilOversikt ? farskapsportalUrlOversikt : farskapsportalUrlForside;
    var beskjed = oppretteBeskjed(meldingTilBruker.hentFormatertMelding(), medEksternVarsling, farskapsportalUrl);

    try {
      kafkaTemplate.send(egenskaper.getBrukernotifikasjon().getEmneBeskjed(), nokkel, beskjed);
    } catch (Exception e) {
      log.error("Opprettelse av beskjed {} til forelder feilet!", meldingTilBruker.getMelding(), e);
      SIKKER_LOGG.error("Opprettelse av beskjed {} til forelder med personident {} feilet!", meldingTilBruker.getMelding(), personidentForelder);
    }

    var medEllerUten = medEksternVarsling ? "med" : "uten";
    log.info("Beskjed {}, {} ekstern varsling og eventId {} er sendt til forelder.", meldingTilBruker.getMelding(), medEllerUten,
        nokkel.getEventId());
    SIKKER_LOGG.info("Beskjed {}, {} ekstern varsling og eventId {} er sendt til forelder med personid.", meldingTilBruker.getMelding(), medEllerUten,
        nokkel.getEventId(), personidentForelder);
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