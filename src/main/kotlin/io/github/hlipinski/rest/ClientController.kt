package io.github.hlipinski.rest

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.decorators.Decorators
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.function.Supplier


@RestController
@RequestMapping(produces = ["application/json"])
class ClientController(
    private val simpleAppClient: SimpleAppClient,
    private val circuitBreaker: CircuitBreaker
) {

    var logger: Logger = LoggerFactory.getLogger(ClientController::class.java)

    @GetMapping("/breaker")
    fun getBreaker(): ResponseEntity<String> {
        val decoratedSupplier: Supplier<String?> =
            Decorators.ofSupplier { simpleAppClient.get503() }
                .withCircuitBreaker(circuitBreaker)
                .withFallback(listOf(CallNotPermittedException::class.java)) { "Hello from fallback!" }
                .decorate()

        kotlin.runCatching { decoratedSupplier.get() }
            .onSuccess { return ResponseEntity.ok(it) }
            .onFailure { logger.error("${it.javaClass} ${it.message}") }

        logger.info(">>>>>>>>>>>> CircuitBreaker state: ${circuitBreaker.state}")

        return ResponseEntity.noContent().build()
    }
}