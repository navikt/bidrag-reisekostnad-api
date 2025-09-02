package no.nav.bidrag.reisekostnad.konfigurasjon

import no.nav.bidrag.reisekostnad.konfigurasjon.Brukernotifikasjonskonfig.NAMESPACE_BIDRAG
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties
class Egenskaperkonfig {
    @Bean
    fun egenskaperBrukernotifikasjon(): Brukernotifikasjon {
        return Brukernotifikasjon()
    }

    @Bean
    fun egenskaper(): Egenskaper {
        return Egenskaper()
    }
}

@ConfigurationProperties(prefix = "egenskaper")
data class Egenskaper(
    var appnavnReisekostnad: String = "bidrag-reisekostnad",
    var urlReisekostnad: String = "",
    var brukernotifikasjon: Brukernotifikasjon = Brukernotifikasjon()
) {
    val cluster: String
        get() = System.getenv()["NAIS_CLUSTER_NAME"] ?: "dev-gcp"

    val namespace: String
        get() = System.getenv()["NAIS_NAMESPACE"] ?: NAMESPACE_BIDRAG

    val appnavn: String
        get() = System.getenv()["NAIS_APP_NAME"] ?: appnavnReisekostnad
}

@ConfigurationProperties(prefix = "brukernotifikasjon")
data class Brukernotifikasjon(
    var emneBrukernotifikasjon: String = "min-side.aapen-brukervarsel-v1",
    var grupperingsidReisekostnad: String = "reisekostnad",
    var synlighetBeskjedAntallMaaneder: Int = 1,
    var levetidOppgaveAntallDager: Int = 30,
    var sikkerhetsnivaaBeskjed: String = "Substantial",
    var sikkerhetsnivaaOppgave: String = "Substantial",
    var skruddPaa: Boolean = true
)
