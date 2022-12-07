package no.nav.bidrag.reisekostnad.tjeneste

import mu.KotlinLogging
import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.doument.BidragDokumentkonsument
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.doument.pdf.PdfGenerator
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Mapper
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.FileSystems
import java.time.LocalDateTime
import javax.transaction.Transactional


private val log = KotlinLogging.logger {}

@Service
class Arkiveringstjeneste(
    private val bidragDokumentkonsument: BidragDokumentkonsument,
    private val mapper: Mapper,
    private val databasetjeneste: Databasetjeneste
) {
    @Transactional
    fun arkivereForespørsel(idForespørsel: Int) {
        val forespørsel = databasetjeneste.henteAktivForespørsel(idForespørsel)
        val pdfDokument = opprettPdf(forespørsel)
        val referanseId = "$REISEKOSTNAD_REFERANSEIDPREFIKS$idForespørsel"
//        val targetFile = File.createTempFile("pdfdoc", "pdf")
//        val file = org.apache.commons.io.FileUtils.getFile("test.pdf")
//        org.apache.commons.io.FileUtils.writeByteArrayToFile(targetFile, pdfDokument)
//        val absolutePath: String = targetFile.toString()
//        println("Temp file : $absolutePath")
//
//        val separator = FileSystems.getDefault().separator
//        val tempFilePath = absolutePath
//            .substring(0, absolutePath.lastIndexOf(separator))
//
//        println("Temp file path : $tempFilePath")
        try {
            val respons = bidragDokumentkonsument.opprettJournalpost(forespørsel.hovedpart.personident, referanseId, pdfDokument)
            forespørsel.journalført = LocalDateTime.now()
            forespørsel.idJournalpost = respons.journalpostId
            log.info { "Arkivert dokument for forespørsel $idForespørsel med journalpostid ${respons.journalpostId}" }
        } catch (e: Exception){
            log.error("Det skjedde en feil ved arkivering av dokument for forespørsel $idForespørsel", e)
        }

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