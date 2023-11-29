package fi.espoo.oppivelvollisuus

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.bindKotlin
import org.jdbi.v3.core.kotlin.mapTo
import org.jdbi.v3.core.mapper.PropagateNull
import kotlin.jvm.optionals.getOrNull

data class EmployeeUser(
    val externalId: String,
    val firstName: String,
    val lastName: String,
    val email: String?
)

typealias EmployeeLoginRequest = EmployeeUser

data class EmployeeBasics(
    @PropagateNull val id: String,
    val name: String
)

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

fun Handle.getEmployees(): List<EmployeeUser> = createQuery(
    """
    SELECT external_id, first_name, last_name, email
    FROM employees
"""
).mapTo<EmployeeUser>().list()

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
