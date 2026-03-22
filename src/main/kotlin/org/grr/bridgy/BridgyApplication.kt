package org.grr.bridgy

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BridgyApplication

fun main(args: Array<String>) {
    runApplication<BridgyApplication>(*args)
}
