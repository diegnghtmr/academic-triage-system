plugins {
    java
    id("org.springframework.boot") version "3.5.9" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.diffplug.spotless") version "7.0.2"
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    }

    repositories { mavenCentral() }

    tasks.withType<Test> { useJUnitPlatform() }
}
