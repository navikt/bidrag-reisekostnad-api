package no.nav.bidrag.reisekostnad.aop

import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.reisekostnad.model.MDC_KORRELASJONSID
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.MDC
import org.springframework.stereotype.Component
import java.net.URLEncoder
import java.util.UUID

@Component
@Aspect
class KorrelasjonsIdAspect {

    @Before(value = "execution(* no.nav.bidrag.reisekostnad.skedulering.Databehandler.*(..))")
    fun schedulerCorrelationIdToThread(joinPoint: JoinPoint) {
        val tilfeldigVerdi = UUID.randomUUID().toString().subSequence(0, 8)
        val enkodedMetodenavn = URLEncoder.encode(joinPoint.signature.name, "utf-8")
        val korrelasjonsId = "${tilfeldigVerdi}_$enkodedMetodenavn"
        MDC.put(MDC_KORRELASJONSID, CorrelationId.existing(korrelasjonsId).get())
    }

    @After(value = "execution(* no.nav.bidrag.reisekostnad.skedulering.Databehandler.*(..))")
    fun clearCorrelationIdFromScheduler(joinPoint: JoinPoint) {
        MDC.clear()
    }
}
