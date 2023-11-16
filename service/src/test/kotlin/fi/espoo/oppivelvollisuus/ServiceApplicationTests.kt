package fi.espoo.oppivelvollisuus

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.assertEquals

class ServiceApplicationTests : FullApplicationTest() {
    @Autowired
    lateinit var controller: MainController

    @Test
    fun `get empty list of students`() {
        assertEquals(emptyList(), controller.getStudents())
    }
}
