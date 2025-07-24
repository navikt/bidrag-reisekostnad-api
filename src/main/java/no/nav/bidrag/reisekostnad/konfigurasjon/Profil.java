package no.nav.bidrag.reisekostnad.konfigurasjon;

public interface Profil {
    String NAIS = "nais";
    String LOKAL_SKY = "lokal-sky";
    String LOKAL_H2 = "lokal-h2";
    String LOKAL_POSTGRES = "lokal-postgres";
    String TEST = "test";
    String HENDELSE = "hendelse";

    String DATABASES_AND_NOT_LOKAL_SKY = "(lokal-h2 | lokal-postgres) & !lokal-sky";
}
