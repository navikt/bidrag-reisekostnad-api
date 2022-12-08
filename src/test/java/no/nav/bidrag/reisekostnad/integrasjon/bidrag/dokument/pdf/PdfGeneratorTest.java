package no.nav.bidrag.reisekostnad.integrasjon.bidrag.dokument.pdf;

import java.time.LocalDate;
import java.util.Set;
import no.nav.bidrag.reisekostnad.api.dto.ut.PersonDto;
import no.nav.bidrag.reisekostnad.integrasjon.bidrag.doument.pdf.PdfGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PdfGeneratorTest {


  @Test
  void skalOpprettePdf() {

    // gitt
    var barn = Set.of(
        new PersonDto("00087324682", "Sandstrand", LocalDate.now().minusMonths(126)),
        new PersonDto("25987324683", "Verksted", LocalDate.now().minusMonths(159)));

    var hovedpart = new PersonDto("659873746879", "Parkas", LocalDate.now().minusMonths(465));
    var motpart = new PersonDto("45987324687", "Bonjour", LocalDate.now().minusMonths(512));


    // hvis
    PdfGenerator.genererePdf(barn, hovedpart, motpart);

    // så

  }


}
