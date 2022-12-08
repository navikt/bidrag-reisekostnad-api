package no.nav.bidrag.reisekostnad.integrasjon.bidrag.doument;

import static no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode.ARKIVERINGSFEIL;
import static no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode.ARKIVERINGSFEIL_OPPGITTE_DATA;
import static no.nav.bidrag.reisekostnad.konfigurasjon.Applikasjonskonfig.SIKKER_LOGG;

import no.nav.bidrag.dokument.dto.MottakUtsendingKanal;
import no.nav.bidrag.dokument.dto.OpprettJournalpostRequest;
import no.nav.bidrag.dokument.dto.OpprettJournalpostResponse;
import no.nav.bidrag.reisekostnad.feilhåndtering.Arkiveringsfeil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class BidragDokumentkonsument {

  private static final String BEHANDLINGSTEMA_REISEKOSTNADER = "ab0129";
  private static final String DOKUMENTTITTEL = "Fordeling av reisekostnader";
  public static final String ENDEPUNKT_OPPRETTE_JOURNALPOST = "/journalpost/%s";
  public static final String KONTEKST_ROT_BIDRAG_DOKUMENT = "/bidrag-dokument";
  private final RestTemplate restTemplate;

  @Autowired
  public BidragDokumentkonsument(@Qualifier("bidrag-dokument-azure-client-credentials") RestTemplate bidragDokumentAzureCCRestTemplate) {
    this.restTemplate = bidragDokumentAzureCCRestTemplate;
  }

  @Retryable(value = RestClientException.class, maxAttempts = 3, backoff = @Backoff(delay = 100000))
  public OpprettJournalpostResponse oppretteJournalpost(String gjelderIdent, String referanseid) {

    var forespørsel = oppretteJournalpostforespørsel(gjelderIdent, referanseid);

    try {
      var respons = restTemplate.exchange(KONTEKST_ROT_BIDRAG_DOKUMENT
              + hentStiEndepunktOppretteJournalpost(), HttpMethod.POST, new HttpEntity<>(forespørsel), OpprettJournalpostResponse.class);
      return respons.getBody();
    } catch (HttpStatusCodeException hsce) {
      if (HttpStatus.BAD_REQUEST.equals(hsce.getStatusCode())) {
        SIKKER_LOGG.warn("Kall mot bidrag-dokument for opprettelse av journalpost returnerte httpstatus {} for gjelder-ident {}",
            hsce.getStatusCode(), forespørsel.getGjelderIdent());
        throw new Arkiveringsfeil(ARKIVERINGSFEIL_OPPGITTE_DATA, hsce.getStatusCode());
      } else {
        SIKKER_LOGG.warn("Kall mot bidrag-dokument for opprettelse av journalpost returnerte httpstatus {} for referanseid {}", hsce.getStatusCode(),
            forespørsel.getReferanseId());
        throw new Arkiveringsfeil(ARKIVERINGSFEIL, hsce.getStatusCode());
      }
    }
  }

  private OpprettJournalpostRequest oppretteJournalpostforespørsel(String gjelderIdent, String referanseid) {
    return new OpprettJournalpostRequest(true, DOKUMENTTITTEL, null, gjelderIdent, null,
        null, null, BEHANDLINGSTEMA_REISEKOSTNADER, null,
        MottakUtsendingKanal.DIGITALT, Tema.BID.name(), null, referanseid, null, null, null);
  }

  enum Tema {BID}

  private String hentStiEndepunktOppretteJournalpost() {
    return String.format(ENDEPUNKT_OPPRETTE_JOURNALPOST, Arkivssystem.JOARK.name());
  }

  enum Arkivssystem {
    BIDRAG, JOARK
  }
}
