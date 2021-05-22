package io.github.hlipinski

import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
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
	fun restTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate {
		return restTemplateBuilder.build()
	}
}

fun main(args: Array<String>) {
	runApplication<Application>(*args)
}