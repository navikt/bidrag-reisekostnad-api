package no.nav.bidrag.reisekostnad.model

import no.nav.bidrag.reisekostnad.database.datamodell.Forespørsel

val Forespørsel.erArkivert get() = journalført != null
val Forespørsel.kanArkiveres get() = !erArkivert && deaktivert == null && (!isKreverSamtykke || samtykket != null)