package fi.espoo.oppivelvollisuus

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.inTransactionUnchecked
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.jvm.optionals.getOrNull

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

data class EmployeeUser(
    val externalId: String,
    val firstName: String,
    val lastName: String,
    val email: String?
)

typealias EmployeeLoginRequest = EmployeeUser

fun Handle.upsertEmployee(employee: EmployeeLoginRequest) =
    createQuery(
        // language=SQL
        """
INSERT INTO employees (external_id, first_name, last_name, email)
VALUES (:externalId, :firstName, :lastName, :email)
ON CONFLICT (external_id) DO UPDATE
SET updated = now(), first_name = :firstName, last_name = :lastName, email = :email
RETURNING external_id, first_name, last_name, email
    """
            .trimIndent()
    )
        .bindKotlin(employee)
        .mapTo<EmployeeUser>()
        .findOne()
        .get()

fun Handle.getEmployee(id: String) =
    createQuery(
        // language=SQL
        """
SELECT external_id, first_name, last_name, email
FROM employees 
WHERE external_id = :id
    """
            .trimIndent()
    )
        .bind("id", id)
        .mapTo<EmployeeUser>()
        .findOne()
        .getOrNull()
