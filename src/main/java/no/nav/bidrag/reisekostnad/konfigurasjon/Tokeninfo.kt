package no.nav.bidrag.reisekostnad.konfigurasjon

import no.nav.bidrag.commons.security.service.OidcTokenManager
import no.nav.bidrag.commons.security.utils.TokenUtils

class Tokeninfo {
    companion object {
        private val oidcTokenManager: OidcTokenManager = OidcTokenManager()
        fun erSystembruker(): Boolean {
            return try {
                TokenUtils.erApplikasjonsbruker()
            } catch (e: Exception) {
                false
            }
        }
        fun hentPaaloggetPerson(): String? {
            return try {
                TokenUtils.hentBruker()
            } catch (e: Exception) {
                null
            }
        }
    }

}