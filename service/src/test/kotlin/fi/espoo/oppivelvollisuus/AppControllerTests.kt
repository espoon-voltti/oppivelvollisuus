package fi.espoo.oppivelvollisuus

import fi.espoo.oppivelvollisuus.domain.AppController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class AppControllerTests : FullApplicationTest() {
    @Autowired
    lateinit var controller: AppController

    @Test
    fun `get empty list of students`() {
        assertEquals(emptyList(), controller.getStudents())
    }
}
