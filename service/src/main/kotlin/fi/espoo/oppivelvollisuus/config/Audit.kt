package fi.espoo.oppivelvollisuus.config

import mu.KLogger
import net.logstash.logback.argument.StructuredArguments
import org.slf4j.Marker
import org.slf4j.MarkerFactory

val AUDIT_MARKER: Marker = MarkerFactory.getMarker("AUDIT_EVENT")

fun KLogger.audit(
    user: AuthenticatedUser,
    eventCode: String,
    meta: Map<String, String> = emptyMap()
) {
    val data =
        mapOf<String, Any?>(
            "userId" to user.id,
            "meta" to meta
        )
    warn(AUDIT_MARKER, eventCode, StructuredArguments.entries(data))
}
