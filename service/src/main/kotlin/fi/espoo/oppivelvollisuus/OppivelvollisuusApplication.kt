package fi.espoo.oppivelvollisuus

import AppEnv
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppEnv::class)
class OppivelvollisuusApplication

fun main(args: Array<String>) {
    runApplication<OppivelvollisuusApplication>(*args)
}
