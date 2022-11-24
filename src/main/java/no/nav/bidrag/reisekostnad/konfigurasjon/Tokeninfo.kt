package no.nav.bidrag.reisekostnad.konfigurasjon

import no.nav.bidrag.commons.security.service.OidcTokenManager
import no.nav.bidrag.commons.security.utils.TokenUtils
import no.nav.bidrag.commons.security.utils.TokenUtils.fetchSubject
import org.springframework.stereotype.Service

@Service
class Tokeninfo(
    private val oidcTokenManager: OidcTokenManager
) {
    fun erSystembruker(): Boolean {
        return try {
            TokenUtils.isSystemUser(oidcTokenManager.fetchTokenAsString())
        } catch (e: Exception) {
            false
        }
    }

    fun hentPaaloggetPerson(): String? {
        return try {
            fetchSubject(oidcTokenManager.fetchTokenAsString())
        } catch (e: Exception) {
            null
        }
    }
}