package no.nav.bidrag.reisekostnad.integrasjon.bidrag.dokument.pdf;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import no.nav.bidrag.reisekostnad.api.dto.ut.PersonDto;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.doument.pdf.PdfGenerator;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Krypteringsverktøy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PdfGeneratorTest {

  private static boolean skriveUtPdf = true;


  @Test
  void skalOpprettePdf() {

    // gitt
    var barn = Set.of(
        new PersonDto(Krypteringsverktøy.kryptere("00087324682"), "Sandstrand", "Sandstrand", LocalDate.now().minusMonths(126)),
        new PersonDto(Krypteringsverktøy.kryptere("25987324683"), "Verksted", "Verksted", LocalDate.now().minusMonths(159)));

    var hovedpart = new PersonDto(Krypteringsverktøy.kryptere("659873746879"), "Parkas", "Parkas", LocalDate.now().minusMonths(465));
    var motpart = new PersonDto(Krypteringsverktøy.kryptere("45987324687"), "Bonjour", "Bonjour", LocalDate.now().minusMonths(512));

    // hvis
    var pdfstrøm = PdfGenerator.genererePdf(barn, hovedpart, motpart, LocalDateTime.now());

    // så
    if (skriveUtPdf) {
      skriveUtPdfForInspeksjon(pdfstrøm);
    }

    assertAll(
        () -> assertThat(skriveUtPdf).isFalse(),
        () -> assertThat(pdfstrøm).isNotNull()
    );
  }

  private void skriveUtPdfForInspeksjon(byte[] pdfstroem) {
    try (final FileOutputStream filstroem = new FileOutputStream("forespørsel.pdf")) {
      filstroem.write(pdfstroem);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

}
