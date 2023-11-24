package fi.espoo.oppivelvollisuus

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OppivelvollisuusApplication

fun main(args: Array<String>) {
    runApplication<OppivelvollisuusApplication>(*args)
}
