plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val testcontainersVersion: String by project

dependencies {
    implementation(project(":infrastructure"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:mariadb:$testcontainersVersion")
}
