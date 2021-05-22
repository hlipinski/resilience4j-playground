package io.github.hlipinski.rest

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class SimpleAppClient(private val restTemplate: RestTemplate) {

    private val PATH = "http://localhost:8080"
    var logger: Logger = LoggerFactory.getLogger(SimpleAppClient::class.java)

    fun getRegular(): String? {
        logger.info("getRegular() method called")
        return restTemplate.getForObject("$PATH/regular", String::class.java)
    }

    fun getSleep(): String? {
        logger.info("getSleep() method called")
        return restTemplate.getForObject("$PATH/sleep", String::class.java)
    }

    fun get503(): String? {
        logger.info("get503() method called")
        return restTemplate.getForObject("$PATH/503", String::class.java)
    }
}