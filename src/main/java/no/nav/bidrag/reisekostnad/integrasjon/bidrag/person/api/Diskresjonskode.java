package no.nav.bidrag.reisekostnad.integrasjon.bidrag.person.api;

import java.util.List;
import org.apache.commons.lang3.StringUtils;

public enum Diskresjonskode {
  SPSF("Strengt fortrolig"), SPFO("Fortrolig"), P19("Strengt fortrolig utland");

  String beskrivelse;

  Diskresjonskode(String beskrivelse) {
    this.beskrivelse = beskrivelse;
  }

  public String getBeskrivelse() {
    return this.beskrivelse;
  }

  public static boolean harMinstEttFamiliemedlemHarDiskresjon(List<Familiemedlem> familiemedlemmer) {

    for (Familiemedlem familiemedlem : familiemedlemmer) {
      if (!StringUtils.isEmpty(familiemedlem.getDiskresjonskode())) {
        for (Diskresjonskode kode : Diskresjonskode.values()) {
          if (kode.name().equalsIgnoreCase(familiemedlem.getDiskresjonskode())) {
            return true;
          }
        }
      }
    }
    return false;
  }
}