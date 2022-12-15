package no.nav.bidrag.reisekostnad;

import java.time.LocalDate;
import lombok.Value;

@Value
public class Testperson {

  public static Testperson testpersonGråtass = new Testperson ("12345678910", "Gråtass", 40);
  public static Testperson testpersonStreng = new Testperson ("11111122222", "Streng", 38);
  public static Testperson testpersonSirup = new Testperson ("33333344444", "Sirup", 35);
  public static Testperson testpersonBarn16 = new Testperson ("77777700000", "Grus", 16);
  public static Testperson testpersonBarn10 = new Testperson ("33333355555", "Småstein", 10);
  public static Testperson testpersonIkkeFunnet = new Testperson ("00000001231", "Utenfor", 29);
  public static Testperson testpersonHarDiskresjon = new Testperson ("23451644512", "Diskos", 29);
  public static Testperson testpersonHarMotpartMedDiskresjon = new Testperson ("56472134561", "Tordivel", 44);
  public static Testperson testpersonHarBarnMedDiskresjon = new Testperson ("32456849111", "Kaktus", 48);
  public static Testperson testpersonErDød = new Testperson ("77765415234", "Steindød", 35);
  public static Testperson testpersonHarDødtBarn = new Testperson ("05784456310", "Albueskjell", 53);
  public static Testperson testpersonDødMotpart = new Testperson ("445132456487", "Bunkers", 41);
  public static Testperson testpersonServerfeil = new Testperson ("12000001231", "Feil", 78);

  String ident;
  String fornavn;
  int alder;
  LocalDate fødselsdato;

  public Testperson(String ident, String fornavn, int alder) {
    this.ident = ident;
    this.fornavn = fornavn;
    this.alder = alder;
    this.fødselsdato = LocalDate.now().minusYears(alder);
  }

}
