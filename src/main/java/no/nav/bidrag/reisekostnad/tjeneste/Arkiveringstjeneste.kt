package no.nav.bidrag.reisekostnad.tjeneste

import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.doument.BidragDokumentkonsument
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.doument.pdf.PdfGenerator
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Mapper
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import javax.transaction.Transactional

@Service
class Arkiveringstjeneste(
    private val bidragDokumentkonsument: BidragDokumentkonsument,
    private val mapper: Mapper,
    private val databasetjeneste: Databasetjeneste
) {
    @Transactional
    fun arkivereForespørsel(idForespørsel: Int): String? {
        val forespørsel = databasetjeneste.henteAktivForespørsel(idForespørsel)
        val pdfDokument = opprettPdf(forespørsel)
        val referanseId = "$REISEKOSTNAD_REFERANSEIDPREFIKS$idForespørsel"

        val respons = bidragDokumentkonsument.opprettJournalpost(forespørsel.hovedpart.personident, referanseId, pdfDokument)

        forespørsel.journalført = LocalDateTime.now()
        forespørsel.idJournalpost = respons.journalpostId
        return respons.journalpostId
    }

    private fun opprettPdf(forespørsel: Forespørsel): ByteArray {
        val barn = mapper.tilPersonDto(forespørsel.barn)
        val hovedpart = mapper.tilPersonDto(forespørsel.hovedpart.personident)
        val motpart = mapper.tilPersonDto(forespørsel.motpart.personident)
        return PdfGenerator.genererePdf(barn, hovedpart, motpart)
    }

    companion object {
        private const val REISEKOSTNAD_REFERANSEIDPREFIKS = "REISEKOSTNAD_"
    }
}