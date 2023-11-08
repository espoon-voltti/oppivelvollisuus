package fi.espoo.oppivelvollisuus

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.inTransactionUnchecked
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class ServiceApplication

fun main(args: Array<String>) {
    runApplication<ServiceApplication>(*args)
}

@RestController
class HelloWorldController {
    @Autowired
    lateinit var jdbi: Jdbi

    @GetMapping("/hello")
    fun helloWorld(): Hello {
        return jdbi.inTransactionUnchecked { tx ->
            tx.createQuery("SELECT count(*) as rows FROM hello").mapTo<Hello>().one()
        }
    }

    data class Hello(
        val rows: Int
    )
}
