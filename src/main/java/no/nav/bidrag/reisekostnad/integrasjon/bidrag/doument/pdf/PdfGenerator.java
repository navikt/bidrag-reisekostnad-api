package no.nav.bidrag.reisekostnad.integrasjon.bidrag.doument.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder.PdfAConformance;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import no.nav.bidrag.reisekostnad.api.dto.ut.PersonDto;
import no.nav.bidrag.reisekostnad.feilhåndtering.Feilkode;
import no.nav.bidrag.reisekostnad.feilhåndtering.InternFeil;
import no.nav.bidrag.reisekostnad.tjeneste.støtte.Krypteringsverktøy;
import org.apache.commons.compress.utils.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.jsoup.nodes.Element;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;

@Component
@Slf4j
public class PdfGenerator {

  private static final String STI_TIL_PDF_TEMPLATE = "/pdf-template/";
  private static final Map<Elementnavn, String> elementnavnTilEngelsk = Map.ofEntries(
      new AbstractMap.SimpleEntry<>(Elementnavn.BARN, "child"),
      new AbstractMap.SimpleEntry<>(Elementnavn.BESKRIVELSE, "description"),
      new AbstractMap.SimpleEntry<>(Elementnavn.FØDSELSDATO, "date-of-birth"),
      new AbstractMap.SimpleEntry<>(Elementnavn.PERSONIDENT, "ssn"),
      new AbstractMap.SimpleEntry<>(Elementnavn.FORNAVN, "first-name")
  );

  private static final Map<Elementnavn, String> nynorskeElementnavn = Map.ofEntries(
      new AbstractMap.SimpleEntry<>(Elementnavn.BESKRIVELSE, "forklaring"),
      new AbstractMap.SimpleEntry<>(Elementnavn.FORNAVN, "namn")
  );

  private static final Map<Tekst, String> tekstBokmål = Map.of(
      Tekst.FØDSELSDATO, "Fødselsdato",
      Tekst.PERSONIDENT, "Fødselsnummer",
      Tekst.FOEDESTED, "Fødested",
      Tekst.FORNAVN, "Navn",
      Tekst.OPPLYSNINGER_OM_BARNET, "Opplysninger om barnet",
      Tekst.HAR_SAMTYKKET, "Har samtykket"
  );

  private static final Map<Tekst, String> tekstNynorsk = Map.of(
      Tekst.FORNAVN, "Namn",
      Tekst.OPPLYSNINGER_OM_BARNET, "Opplysningar om barnet"
  );

  private static final Map<Tekst, String> tekstEngelsk = Map.of(
      Tekst.FØDSELSDATO, "Date of birth",
      Tekst.PERSONIDENT, "Social security number",
      Tekst.FORNAVN, "Name"
  );

  public static byte[] genererePdf(Set<PersonDto> barn, PersonDto hovedperson, PersonDto motpart, LocalDateTime samtykketDato) {

    var skriftspråk = Skriftspråk.BOKMÅL;

    log.info("Oppretter dokument for reisekostnad på språk {}", skriftspråk);

    var html = byggeHtmlstrengFraMal(STI_TIL_PDF_TEMPLATE, skriftspråk, barn, hovedperson, motpart, samtykketDato);
    try (final ByteArrayOutputStream pdfStream = new ByteArrayOutputStream()) {

      var htmlSomStrøm = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
      org.jsoup.nodes.Document jsoupDoc = Jsoup.parse(htmlSomStrøm, "UTF-8", "pdf-template/template.html");
      Document doc = new W3CDom().fromJsoup(jsoupDoc);
      var builder = new PdfRendererBuilder();

      try (InputStream colorProfile = PdfGenerator.class.getResourceAsStream("/pdf-template/ISOcoated_v2_300_bas.ICC")) {
        byte[] colorProfileBytes = IOUtils.toByteArray(colorProfile);
        builder.useColorProfile(colorProfileBytes);
      }

      try (InputStream fontStream = PdfGenerator.class.getResourceAsStream("/pdf-template/Arial.ttf")) {
        final File midlertidigFil = File.createTempFile("Arial", "ttf");
        midlertidigFil.deleteOnExit();
        try (FileOutputStream ut = new FileOutputStream(midlertidigFil)) {
          IOUtils.copy(fontStream, ut);
        }
        builder.useFont(midlertidigFil, "ArialNormal");
      }

      try {
        InputStream colorProfile = PdfGenerator.class.getResourceAsStream("/pdf-template/sRGB.icc");
        builder.useProtocolsStreamImplementation(new ClassPathStreamFactory(), "classpath")
            .useFastMode()
            .usePdfAConformance(PdfAConformance.PDFA_2_A)
            .withW3cDocument(doc, "classpath:/pdf-template/")
            .useColorProfile(colorProfile.readAllBytes())
            .toStream(pdfStream)
            .run();

      } catch (Exception e) {
        e.printStackTrace();
      }

      var innhold = pdfStream.toByteArray();
      pdfStream.close();

      return innhold;
    } catch (IOException ioe) {
      throw new InternFeil(Feilkode.PDF_OPPRETTELSE_FEILET, ioe);
    }
  }

  private static void leggeTilDataBarn(Element barnElement, Set<PersonDto> barna, Skriftspråk skriftspråk) {

    var barnaSortert = barna.stream().sorted((a,b) -> a.getFødselsdato().isAfter(b.getFødselsdato()) ? -1 : 1);
    var detaljerFørsteBarn = barnElement.getElementById("detaljer_barn_1");

    var tekstformatBarnNavn = tekstvelger(Tekst.FORNAVN, skriftspråk) + ": %s";
    var tekstformatBarnFødselsdato = tekstvelger(Tekst.PERSONIDENT, skriftspråk) + ": %s";
    var it = barnaSortert.iterator();
    var barn1 = it.next();
    detaljerFørsteBarn
        .getElementsByClass(henteElementnavn(Elementnavn.NAVN, skriftspråk)).first()
        .text(String.format(tekstformatBarnNavn, barn1.getKortnavn()));

    detaljerFørsteBarn
        .getElementsByClass(henteElementnavn(Elementnavn.PERSONIDENT, skriftspråk)).first()
        .text(String.format(tekstformatBarnFødselsdato, dekryptere(barn1.getIdent())));

    var antallBarn = 1;

    while(it.hasNext()) {
      var barn = it.next();
      var mellomrom =  new Element("p");
      mellomrom.appendTo(barnElement);
      var nesteBarnIRekka = new Element("div");
      nesteBarnIRekka.id("detaljer_barn_" +  ++antallBarn);

      var navnElement = new Element("div");
      var fødselsnummerElement = new Element("div");

      navnElement.text(String.format(tekstformatBarnNavn, barn.getFornavn()));
      fødselsnummerElement.text(String.format(tekstformatBarnFødselsdato, dekryptere(barn.getIdent())));

      navnElement.appendTo(nesteBarnIRekka);
      fødselsnummerElement.appendTo(nesteBarnIRekka);
      nesteBarnIRekka.appendTo(barnElement);
    }
  }

  private static void leggTilSamtykketInfo(Element element, Skriftspråk skriftspraak, LocalDateTime samtykketDato){
    var samtykket = element.getElementsByClass(henteElementnavn(Elementnavn.SAMTYKKET, skriftspraak));

    var samtykketResultat = samtykketDato == null ? "Nei" : "Ja";
    samtykket.first().text(tekstvelger(Tekst.HAR_SAMTYKKET, skriftspraak) + ": " + samtykketResultat);
  }
  private static void leggeTilDataForelder(Element forelderelement, PersonDto forelder, Skriftspråk skriftspraak) {
    var navn = forelderelement.getElementsByClass(henteElementnavn(Elementnavn.NAVN, skriftspraak));

    navn.first().text(tekstvelger(Tekst.FORNAVN, skriftspraak) + ": " + forelder.getKortnavn());

    var foedselsnummer = forelderelement.getElementsByClass(henteElementnavn(Elementnavn.PERSONIDENT, skriftspraak));
    foedselsnummer.first().text(tekstvelger(Tekst.PERSONIDENT, skriftspraak) + ": " + dekryptere(forelder.getIdent()));
  }

  private static String byggeHtmlstrengFraMal(String pdfmal, Skriftspråk skriftspråk, Set<PersonDto> barn, PersonDto hovedperson, PersonDto motpart, LocalDateTime samtykketDato) {
    try {
      var input = new ClassPathResource(pdfmal + skriftspråk.toString().toLowerCase() + ".html").getInputStream();
      var document = Jsoup.parse(input, "UTF-8", "");

      // Legge til informasjon om barn
      leggeTilDataBarn(document.getElementById(henteElementnavn(Elementnavn.BARN, skriftspråk)), barn, skriftspråk);
      // Legge til informasjon om mor
      leggeTilDataForelder(document.getElementById(henteElementnavn(Elementnavn.HOVEDPART, skriftspråk)), hovedperson, skriftspråk);
      // Legge til informasjon om far
      var motpartElement = document.getElementById(henteElementnavn(Elementnavn.MOTPART, skriftspråk));
      leggeTilDataForelder(motpartElement, motpart, skriftspråk);
      leggTilSamtykketInfo(motpartElement, skriftspråk, samtykketDato);

      var datoElement = document.getElementById(henteElementnavn(Elementnavn.DATO_OPPRETTET, skriftspråk));
      datoElement.text(String.format("Dato: %s", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))));

      // jsoup fjerner tagslutt for <link> og <meta> - legger på manuelt ettersom dette er påkrevd av PDFBOX
      var html = document.html().replaceFirst("charset=utf-8\">", "charset=utf-8\"/>");
      html = html.replaceFirst("href=\"style.css\">", "href=\"style.css\"/>");

      return html;

    } catch (IOException ioe) {
      throw new InternFeil(Feilkode.PDF_OPPRETTELSE_FEILET, ioe);
    }
  }

  private static String henteElementnavn(Elementnavn element, Skriftspråk skriftspraak) {

    switch (skriftspraak) {
      case ENGELSK -> {
        return elementnavnTilEngelsk.get(element);
      }
      case NYNORSK -> {
        if (nynorskeElementnavn.containsKey(element)) {
          return nynorskeElementnavn.get(element);
        }
      }
    }

    // bokmål
    return element.toString().toLowerCase();
  }

  private static String tekstvelger(Tekst tekst, Skriftspråk skriftspråk) {
    switch (skriftspråk) {
      case ENGELSK -> {
        return tekstEngelsk.get(tekst);
      }
      case NYNORSK -> {
        if (tekstNynorsk.containsKey(tekst)) {
          return tekstNynorsk.get(tekst);
        } else {
          return tekstBokmål.get(tekst);
        }
      }
      default -> {
        return tekstBokmål.get(tekst);
      }
    }
  }

  private enum Tekst {
    FØDSELSDATO,
    PERSONIDENT,
    FOEDESTED,
    FORNAVN,
    HAR_SAMTYKKET,
    OPPLYSNINGER_OM_BARNET,
    TERMINDATO;
  }

  private enum Elementnavn {
    BARN,
    BARN_1,
    BESKRIVELSE,
    DETALJER_BARN,
    NAVN,
    MOTPART,
    FØDSELSDATO,
    PERSONIDENT,
    HOVEDPART,
    FORNAVN,
    SAMTYKKET,
    DATO_OPPRETTET
  }
  private static String dekryptere(String kryptertPersonident) {
    return Krypteringsverktøy.dekryptere(kryptertPersonident);
  }

}
