plugins { `java-library` }

dependencies {
    implementation(project(":domain"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
}
