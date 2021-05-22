package io.github.hlipinski.rest

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(produces = ["application/json"])
class SimpleAppController {

    @GetMapping("/regular")
    fun getRegular(): ResponseEntity<String> {
        return ResponseEntity.ok("Hello!")
    }

    @GetMapping("/sleep")
    fun getSleep(): ResponseEntity<String> {
        Thread.sleep(3000)
        return ResponseEntity.ok("Not sleeping!")
    }

    @GetMapping("/503")
    fun get503(): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build()
    }
}