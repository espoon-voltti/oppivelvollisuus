package fi.espoo.oppivelvollisuus

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.inTransactionUnchecked
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller for "system" endpoints intended to be only called from api-gateway
 * as the system internal user
 */
@RestController
@RequestMapping("/system")
class SystemController {
    @Autowired
    lateinit var jdbi: Jdbi

    @PostMapping("/employee-login")
    fun employeeLogin(@RequestBody employee: EmployeeLoginRequest): EmployeeUser {
        return jdbi.inTransactionUnchecked { it.upsertEmployee(employee) }
    }

    @GetMapping("/employee/{id}")
    fun getEmployeeUser(@PathVariable id: String): EmployeeUser? {
        return jdbi.inTransactionUnchecked { it.getEmployee(id) }
    }
}
