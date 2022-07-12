package dev.crashteam.heavengate

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@EnableCaching
@SpringBootApplication
class HeavenGateApplication

fun main(args: Array<String>) {
    runApplication<HeavenGateApplication>(*args)
}
