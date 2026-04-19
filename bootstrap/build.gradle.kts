plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val testcontainersVersion: String by project

dependencies {
    implementation(project(":infrastructure"))
    testImplementation(project(":application"))
    testImplementation(project(":domain"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.jupiter")
        exclude(group = "org.junit.platform")
    }
    testImplementation("org.testcontainers:mariadb:$testcontainersVersion")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.2")
    
    val jjwtVersion: String by project
    testImplementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    testRuntimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    testRuntimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")
}
