plugins {
    java
}

group = "lt.mredgariux"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")

    testImplementation(platform("org.junit:junit-bom:5.11.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 17
}

val compatibilityApiVersions = mapOf(
    "Paper1206" to "1.20.6-R0.1-SNAPSHOT",
    "Paper1211" to "1.21.1-R0.1-SNAPSHOT",
    "Paper12111" to "1.21.11-R0.1-SNAPSHOT"
)

val compatibilityChecks = compatibilityApiVersions.map { (taskSuffix, apiVersion) ->
    val apiClasspath = configurations.create("${taskSuffix.replaceFirstChar { it.lowercase() }}Api")
    dependencies.add(apiClasspath.name, "io.papermc.paper:paper-api:$apiVersion")

    tasks.register<JavaCompile>("compileAgainst$taskSuffix") {
        description = "Compiles the plugin against Paper API $apiVersion."
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        source = sourceSets.main.get().java
        classpath = apiClasspath
        destinationDirectory = layout.buildDirectory.dir("compatibility/$taskSuffix")
        options.encoding = "UTF-8"
        options.release = 17
    }
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.check {
    dependsOn(compatibilityChecks)
}

tasks.jar {
    archiveBaseName = "Saugykla"
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}
