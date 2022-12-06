package no.nav.bidrag.reisekostnad.tjeneste;

import java.time.LocalDateTime;
import javax.transaction.Transactional;
import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.doument.BidragDokumentkonsument;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.doument.pdf.PdfGenerator;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Mapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class Arkiveringstjeneste {

  private final static String REISEKOSTNAD_REFERANSEIDPREFIKS = "REISEKOSTNAD_";
  private final BidragDokumentkonsument bidragDokumentkonsument;
  private final Mapper mapper;
  private final Databasetjeneste databasetjeneste;
  private final PdfGenerator pdfGenerator;

  @Autowired
  public Arkiveringstjeneste(
      BidragDokumentkonsument bidragDokumentkonsument,
      Mapper mapper,
      Databasetjeneste databasetjeneste,
      PdfGenerator pdfGenerator) {
    this.bidragDokumentkonsument = bidragDokumentkonsument;
    this.mapper = mapper;
    this.databasetjeneste = databasetjeneste;
    this.pdfGenerator = pdfGenerator;
  }

  @Transactional
  public String arkivereForespørsel(int idForespørsel) {
    var forespørsel = databasetjeneste.henteAktivForespørsel(idForespørsel);
    var respons = bidragDokumentkonsument.oppretteJournalpost(forespørsel.getHovedpart().getPersonident(),
        REISEKOSTNAD_REFERANSEIDPREFIKS + idForespørsel);

    forespørsel.setJournalført(LocalDateTime.now());
    forespørsel.setIdJournalpost(respons.getJournalpostId());

    return respons.getJournalpostId();
  }

  private byte[] opprettePdf(Forespørsel forespørsel) {

    var barn = mapper.tilPersonDto(forespørsel.getBarn());
    var hovedpart = mapper.tilPersonDto(forespørsel.getHovedpart().getPersonident());
    var motpart = mapper.tilPersonDto(forespørsel.getMotpart().getPersonident());

    return pdfGenerator.genererePdf(barn, hovedpart, motpart);
  }
}
