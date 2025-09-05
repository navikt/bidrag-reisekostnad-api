package no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon;

import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.SIKKER_LOGG;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.konfigurasjon.Egenskaper;
import no.nav.tms.varsel.action.Sensitivitet;
import no.nav.tms.varsel.action.Varseltype;
import no.nav.tms.varsel.builder.OpprettVarselBuilder;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@Value
public class Beskjedprodusent {

  KafkaTemplate<String, String> kafkaTemplate;
  URL reisekostnadForside;
  Egenskaper egenskaper;

  public void oppretteBeskjedTilBruker(String personidentForelder, DynamiskMelding meldingTilBruker, boolean medEksternVarsling, String varselId) {

    var beskjed = oppretteBeskjed(meldingTilBruker.hentFormatertMelding(), reisekostnadForside, varselId, personidentForelder, medEksternVarsling);

    if (!egenskaper.getBrukernotifikasjon().getSkruddPaa()) {
      log.warn("Brukernotifikasjoner er skrudd av - {} ble derfor ikke sendt.", meldingTilBruker.getMelding());
      return;
    }

    try {
      kafkaTemplate.send(egenskaper.getBrukernotifikasjon().getEmneBrukernotifikasjon(), varselId, beskjed);
    } catch (Exception e) {
      log.error("Opprettelse av beskjed {} til forelder feilet!", meldingTilBruker.getMelding(), e);
      SIKKER_LOGG.error("Opprettelse av beskjed {} til forelder med personident {} feilet!", meldingTilBruker.getMelding(), personidentForelder);
    }

    var medEllerUten = medEksternVarsling ? "med" : "uten";
    log.info("Beskjed {}, {} ekstern varsling og varselId {} er sendt til forelder.", meldingTilBruker.getMelding(), medEllerUten,
        varselId);
    SIKKER_LOGG.info("Beskjed {}, {} ekstern varsling og varselId {} er sendt til forelder med personid {}.", meldingTilBruker.getMelding(),
        medEllerUten, varselId, personidentForelder);
  }

  private String oppretteBeskjed(String meldingTilBruker, URL lenke, String varselId, String fodselsnummer, Boolean medEksternVarsling) {

    OpprettVarselBuilder builder = OpprettVarselBuilder.newInstance()
        .withType(Varseltype.Beskjed)
        .withVarselId(varselId)
        .withSensitivitet(
            Sensitivitet.valueOf(
                egenskaper.getBrukernotifikasjon().getSikkerhetsnivaaBeskjed()))
        .withIdent(fodselsnummer)
        .withTekst("nb", meldingTilBruker, true)
        .withLink(lenke.toString())
        .withAktivFremTil(
            ZonedDateTime.now(ZoneId.of("UTC"))
                .withHour(0)
                .plusMonths(
                    egenskaper
                        .getBrukernotifikasjon()
                        .getSynlighetBeskjedAntallMaaneder()))
        .withProdusent(
            egenskaper.getCluster(),
            egenskaper.getNamespace(),
            egenskaper.getAppnavn());

    if (medEksternVarsling) {
      builder = builder.withEksternVarsling();
    }

    return builder.build();
  }
}
