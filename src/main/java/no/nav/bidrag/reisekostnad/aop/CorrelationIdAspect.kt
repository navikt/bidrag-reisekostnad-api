package no.nav.bidrag.reisekostnad.aop

import no.nav.bidrag.commons.CorrelationId
import no.nav.bidrag.reisekostnad.model.KORRELINGSID
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.slf4j.MDC
import org.springframework.stereotype.Component

@Component
@Aspect
class CorrelationIdAspect {

    @Before(value = "execution(* no.nav.bidrag.reisekostnad.skedulering.Databehandler.*(..))")
    fun schedulerCorrelationIdToThread(joinPoint: JoinPoint) {
        val correlationId = CorrelationId.generateTimestamped(joinPoint.signature.name).get()
        MDC.put(KORRELINGSID, correlationId)
    }

    @After(value = "execution(* no.nav.bidrag.reisekostnad.skedulering.Databehandler.*(..))")
    fun clearCorrelationIdFromScheduler(joinPoint: JoinPoint) {
        MDC.clear()
    }
}
