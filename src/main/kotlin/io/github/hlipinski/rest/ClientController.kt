package io.github.hlipinski.rest

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.decorators.Decorators
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.timelimiter.TimeLimiter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier


@RestController
@RequestMapping(produces = ["application/json"])
class ClientController(
    private val simpleAppClient: SimpleAppClient,
    private val circuitBreaker: CircuitBreaker,
    private val retry: Retry,
    private val rateLimiter: RateLimiter,
    private val bulkhead: Bulkhead,
    private val timeLimiter: TimeLimiter
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

        return ResponseEntity.ok().build()
    }

    @GetMapping("/retry")
    fun getWithRetry(): ResponseEntity<String> {
        val decoratedSupplier = Retry.decorateSupplier(retry) { simpleAppClient.get503() }

        kotlin.runCatching { decoratedSupplier.get() }
            .onSuccess { return ResponseEntity.ok(it) }
            .onFailure {
                logger.error("${it.javaClass} ${it.message}")
                return ResponseEntity.ok(it.message)
            }

        return ResponseEntity.ok().build()
    }

    @GetMapping("/rateLimiter")
    fun getWithRateLimit(): ResponseEntity<String> {
        val decoratedSupplier = RateLimiter.decorateSupplier(rateLimiter) { simpleAppClient.getRegular() }

        return ResponseEntity.ok(decoratedSupplier.get())
    }

    @GetMapping("/bulkhead")
    fun getWithBulkhead(): ResponseEntity<String> {
        val decoratedSupplier = Bulkhead.decorateSupplier(bulkhead) { simpleAppClient.getSleep() }

        return ResponseEntity.ok(decoratedSupplier.get())
    }

    @GetMapping("/timeLimiter")
    fun getWithTimeLimiter(): ResponseEntity<String> {
        val decoratedSupplier = TimeLimiter.decorateFutureSupplier(timeLimiter) {
            CompletableFuture.supplyAsync() { simpleAppClient.getSleep() }
        }

        return ResponseEntity.ok(decoratedSupplier.call())
    }
}