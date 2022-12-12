package no.nav.bidrag.reisekostnad.integrasjon.brukernotifikasjon;

import static no.nav.bidrag.reisekostnad.konfigurasjon.Brukernotifikasjonskonfig.NAMESPACE_BIDRAG;

import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.InternFeil;
import no.nav.bidrag.reisekostnad.konfigurasjon.Egenskaper;
import no.nav.bidrag.reisekostnad.tjeneste.Databasetjeneste;
import no.nav.brukernotifikasjon.schemas.builders.NokkelInputBuilder;
import no.nav.brukernotifikasjon.schemas.builders.OppgaveInputBuilder;
import no.nav.brukernotifikasjon.schemas.input.NokkelInput;
import no.nav.brukernotifikasjon.schemas.input.OppgaveInput;
import org.springframework.kafka.core.KafkaTemplate;

@Slf4j
@AllArgsConstructor
public class Oppgaveprodusent {

  private KafkaTemplate kafkaTemplate;
  private Databasetjeneste databasetjeneste;
  private URL farskapsportalUrl;
  private Egenskaper egenskaper;

  public void oppretteOppgaveOmSamtykke(int idForespørsel, String personidentMotpart, DynamiskMelding oppgavetekst,
      boolean medEksternVarsling) {

    var nokkel = new NokkelInputBuilder()
        .withEventId(UUID.randomUUID().toString())
        .withGrupperingsId(egenskaper.getBrukernotifikasjon().getGrupperingsidReisekostnad())
        .withFodselsnummer(personidentMotpart)
        .withAppnavn(egenskaper.getAppnavnReisekostnad())
        .withNamespace(NAMESPACE_BIDRAG)
        .build();

    var melding = oppretteOppgave(oppgavetekst.hentFormatertMelding(), medEksternVarsling, farskapsportalUrl);

    var farsAktiveSigneringsoppgaver = databasetjeneste.henteAktiveOppgaverMotpart(idForespørsel, personidentMotpart);

    if (farsAktiveSigneringsoppgaver.isEmpty()) {
      log.info("Oppretter oppgave om samtykke til motpart i forespørsel med id {}", idForespørsel);

      oppretteOppgave(nokkel, melding);

      log.info("Samtykkeoppgave opprettet for forespørsel med id {}.", idForespørsel);
      databasetjeneste.lagreNyOppgavebestilling(idForespørsel, nokkel.getEventId());
    }
  }

  private void oppretteOppgave(NokkelInput nokkel, OppgaveInput melding) {
    try {
      kafkaTemplate.send(egenskaper.getBrukernotifikasjon().getEmneOppgave(), nokkel, melding);
    } catch (Exception e) {
      e.printStackTrace();
      throw new InternFeil(Feilkode.BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE, e);
    }
  }

  private OppgaveInput oppretteOppgave(String oppgavetekst, boolean medEksternVarsling, URL farskapsportalUrl) {

    return new OppgaveInputBuilder()
        .withTidspunkt(ZonedDateTime.now(ZoneId.of("UTC")).toLocalDateTime())
        .withEksternVarsling(medEksternVarsling)
        .withLink(farskapsportalUrl)
        .withSikkerhetsnivaa(egenskaper.getBrukernotifikasjon().getSikkerhetsnivaaOppgave())
        .withSynligFremTil(
            ZonedDateTime.now(ZoneId.of("UTC")).plusDays(egenskaper.getBrukernotifikasjon().getLevetidOppgaveAntallDager())
                .toLocalDateTime())
        .withTekst(oppgavetekst).build();
  }
}
