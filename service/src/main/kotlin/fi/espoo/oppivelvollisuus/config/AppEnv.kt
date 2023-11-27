import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

@ConfigurationProperties(prefix = "app")
data class AppEnv(
    val jwt: JwtEnv
) {
    data class JwtEnv(
        val publicKeysUrl: URI
    )
}
