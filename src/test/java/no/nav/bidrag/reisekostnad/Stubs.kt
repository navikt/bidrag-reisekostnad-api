package no.nav.bidrag.reisekostnad

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.matching.ContainsPattern
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder

fun getBidragDokumentRequestPatternBuilder(forespørselId: Int?): RequestPatternBuilder? {
    val verify = WireMock.postRequestedFor(
        WireMock.urlEqualTo("/bidrag-dokument/journalpost/JOARK")
    )
    verify.withRequestBody(ContainsPattern(String.format("\"referanseId\":\"REISEKOSTNAD_%s\"", forespørselId)))
    return verify
}

fun verifiserDokumentArkivertForForespørsel(forespørselId: Int?) {
    WireMock.verify(getBidragDokumentRequestPatternBuilder(forespørselId))
}