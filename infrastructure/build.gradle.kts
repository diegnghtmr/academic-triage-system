plugins {
    `java-library`
    id("io.spring.dependency-management")
}

val springBootVersion: String by project
val springAiVersion: String by project
val jjwtVersion: String by project
val mapstructVersion: String by project
val lombokVersion: String by project
val springdocVersion: String by project
val flywayVersion: String by project
val archunitVersion: String by project
val testcontainersVersion: String by project

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion")
        mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
    }
}

dependencies {
    implementation(project(":application"))
    implementation(project(":domain"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:$jjwtVersion")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:$jjwtVersion")

    // Spring AI (optional - enabled via profile)
    implementation("org.springframework.ai:spring-ai-starter-model-openai:$springAiVersion")

    // Database
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // Mapping
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    // Lombok (infrastructure ONLY)
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    // Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springdocVersion")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:mariadb:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("com.tngtech.archunit:archunit-junit5:$archunitVersion")
    testImplementation("org.wiremock:wiremock-standalone:3.10.0")
}
