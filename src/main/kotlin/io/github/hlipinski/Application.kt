package io.github.hlipinski

import io.github.resilience4j.bulkhead.Bulkhead
import io.github.resilience4j.bulkhead.BulkheadConfig
import io.github.resilience4j.bulkhead.BulkheadRegistry
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RateLimiterRegistry
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
import io.github.resilience4j.timelimiter.TimeLimiter
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import io.github.resilience4j.timelimiter.TimeLimiterRegistry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.RestTemplate
import java.time.Duration

@SpringBootApplication
class Application {
    @Bean
    fun circuitBreaker(): CircuitBreaker {
        val circuitBreakerRegistry = CircuitBreakerRegistry.of(
            CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .slowCallRateThreshold(50f)
                .failureRateThreshold(50f)
                .waitDurationInOpenState(Duration.ofSeconds(10))
                .permittedNumberOfCallsInHalfOpenState(4)
                .writableStackTraceEnabled(false)
                .recordExceptions(Throwable::class.java)
                .ignoreExceptions(NullPointerException::class.java)
                .build()
        )

        return circuitBreakerRegistry.circuitBreaker("clientController")
    }

    @Bean
    fun retry(): Retry {
        val retryRegistry = RetryRegistry.of(
            RetryConfig.custom<ResponseEntity<Any>>()
                .maxAttempts(5)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofSeconds(1), 2.toDouble()))
                .retryOnResult { it.statusCode == HttpStatus.SERVICE_UNAVAILABLE }
//                .retryExceptions(IOException::class.java, TimeoutException::class.java)
                .ignoreExceptions(NullPointerException::class.java)
                .failAfterMaxAttempts(true)
                .build()
        )

        return retryRegistry.retry("clientController")
    }

    @Bean
    fun rateLimiter(): RateLimiter {
        val registry = RateLimiterRegistry.of(
            RateLimiterConfig.custom()
                .limitForPeriod(2)
                .limitRefreshPeriod(Duration.ofSeconds(2))
                .timeoutDuration(Duration.ofSeconds(2))
                .build()
        )

        return registry.rateLimiter("clientController")
    }

    @Bean
    fun bulkhead(): Bulkhead {
        val registry = BulkheadRegistry.of(
            BulkheadConfig.custom()
                .maxConcurrentCalls(2)
                .maxWaitDuration(Duration.ofSeconds(10))
                .writableStackTraceEnabled(false)
                .build()
        )

        return registry.bulkhead("clientController")
    }

    @Bean
    fun timeLimiter(): TimeLimiter {
        val registry = TimeLimiterRegistry.of(
            TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(2))
                .build()
        )

        return registry.timeLimiter("clientController")
    }

    @Bean
    fun restTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
        return restTemplateBuilder.build()
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}