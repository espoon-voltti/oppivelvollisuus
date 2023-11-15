rootProject.name = "oppivelvollisuus-service"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://build.shibboleth.net/maven/releases") {
            content {
                includeGroup("net.shibboleth")
                includeGroup("net.shibboleth.utilities")
                includeGroup("org.opensaml")
            }
        }
    }
}
