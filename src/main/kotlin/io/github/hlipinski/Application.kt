package io.github.hlipinski

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import io.github.resilience4j.retry.RetryRegistry
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
    fun restTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
        return restTemplateBuilder.build()
    }
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}