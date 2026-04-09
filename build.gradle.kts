plugins {
    java
    jacoco
    id("org.springframework.boot") version "3.5.9" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    id("com.diffplug.spotless") version "7.0.2"
}

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "jacoco")

    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    }

    repositories { mavenCentral() }

    tasks.withType<Test> {
        useJUnitPlatform()
        finalizedBy(tasks.withType<JacocoReport>())
    }

    tasks.withType<JacocoReport> {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}

tasks.register<JacocoReport>("jacocoRootReport") {
    group = "Verification"
    description = "Generates an aggregate JaCoCo coverage report for all subprojects."

    val subprojects = subprojects.filter { it.plugins.hasPlugin("java") }
    dependsOn(subprojects.map { it.tasks.withType<Test>() })

    additionalSourceDirs.setFrom(subprojects.map { it.sourceSets.main.get().allSource.srcDirs })
    sourceDirectories.setFrom(subprojects.map { it.sourceSets.main.get().allSource.srcDirs })
    classDirectories.setFrom(subprojects.map { it.sourceSets.main.get().output })

    executionData.setFrom(files(subprojects.map {
        File(it.layout.buildDirectory.asFile.get(), "jacoco/test.exec")
    }).filter { it.exists() })

    reports {
        xml.required.set(true)
        html.required.set(true)
        xml.outputLocation.set(file(layout.buildDirectory.file("reports/jacoco/root/jacocoRootReport.xml")))
        html.outputLocation.set(file(layout.buildDirectory.dir("reports/jacoco/root/html")))
    }
}
