package org.grr.bridgy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableJpaAuditing
@SpringBootApplication
class BridgyApplication

fun main(args: Array<String>) {
    runApplication<BridgyApplication>(*args)
}