package fi.espoo.oppivelvollisuus

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.inTransactionUnchecked
import org.jdbi.v3.core.kotlin.mapTo
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("local")
class DevDataInserter(jdbi: Jdbi) {
    @Autowired
    lateinit var jdbi: Jdbi

    init {
        jdbi.inTransactionUnchecked { tx ->
            val emptyDb = tx.createQuery("SELECT count(*) FROM employees").mapTo<Int>().first() == 0
            if (emptyDb) {
                tx.createUpdate(
                    """
INSERT INTO employees (external_id, first_name, last_name)
VALUES ('12345678-0000-0000-0000-000000000000', 'Sanna', 'Suunnittelija');
INSERT INTO employees (external_id, first_name, last_name)
VALUES ('12345678-0000-0000-0000-000000000001', 'Olli', 'Ohjaaja');
                """
                ).execute()
            }
        }
    }
}
