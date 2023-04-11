package no.nav.bidrag.reisekostnad.feilhåndtering;

public enum Feilkode {

  ARKIVERINGSFEIL_OPPGITTE_DATA("Feil i data oppgitt til arkiveringstjenesten"),
  ARKIVERINGSFEIL("Feil ved arkivering av forespørsel"),
  KAN_IKKE_ARKIVERE_FORESPØRSEL("Feil ved arkivering av forespørsel"),
  BRUKERNOTIFIKASJON_OPPRETTE_OPPGAVE("Opprettelse av brukernotifikasjonsoppgave feilet!"),
  DATABASEFEIL("Feil ved lesing fra eller skriving til database"),
  DATAFEIL("Feil ved henting av lagrede data"),
  FANT_IKKE_FORESPØRSEL("Oppgitt forespørsel (id: {}) ble ikke funnet i databasen"),
  INTERNFEIL("Intern feil har oppstått."),
  VALIDERING_DEAKTIVERE_PERSON_IKKE_PART_I_FORESPØRSEL("Personen er ikke part i forespørselen som ble forsøkt deaktivert"),
  VALIDERING_DEAKTIVERE_FEIL_STATUS("Den oppgitte forespørselen er allerede deaktivert"),
  KRYPTERING("Kryptering av personident feilet."),
  VALIDERING_PÅLOGGET_PERSON_DISKRESJON("Pålogget person har diskresjon. Kan ikke benytte løsningen."),
  VALIDERING_SAMTYKKE_MOTPART("Fant ingen aktive forespørsler knyttet til oppgitt motpart. Samtykke ikke oppdatert."),
  PDF_OPPRETTELSE_FEILET("Opprettelse av PDF feilet."),
  PDL_PERSON_DØD("Pålogget person er død."),
  PDL_PERSON_IKKE_FUNNET("Fant ikke person i PDL"),
  PDL_FEIL("En feil oppstod ved henting av data fra PDL"),
  RESSURS_IKKE_FUNNET("Fant ikke ønsket ressurs"),
  VALIDERING_NY_FOREPØRSEL("Feil ved validering av ny forespørsel"),
  VALIDERING_NY_FOREPØRSEL_INGEN_FAMILIERELASJONER("Feil ved validering av ny forespørsel. Fant ingen familierelasjoner for person."),
  VALIDERING_NY_FOREPØRSEL_MANGLER_RELASJON_MOTPART("Person mangler relasjon til oppgitt motpart"),
  VALIDERING_NY_FOREPØRSEL_MANGLER_RELASJON_BARN("Person mangler relasjon til ett eller flere av de/ det oppgitte barna/ barnet"),
  VALIDERING_NY_FOREPØRSEL_BARN_I_AKTIV_FORESPØRSEL("Minst ett av det/de oppgitte barnet/ barna er tilknyttet en aktiv forepørsel.");

  private final String beskrivelse;

  Feilkode(String beskrivelse) {
    this.beskrivelse = beskrivelse;
  }

  public String getBeskrivelse() {
    return this.beskrivelse;
  }
}
