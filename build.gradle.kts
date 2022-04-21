
import org.gradle.jvm.tasks.Jar

plugins {
    id("java")
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
}

group = "org.example"
version = "1.0-SNAPSHOT"

extra["springCloudVersion"] = "2021.0.0"
extra["springCloudGcpVersion"] = "2.0.4"

repositories {
    mavenCentral()
}

dependencies {

    implementation("com.google.cloud:spring-cloud-gcp-starter")

    //Datastore
    implementation("com.google.cloud:spring-cloud-gcp-data-datastore")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
        mavenBom("com.google.cloud:spring-cloud-gcp-dependencies:${property("springCloudGcpVersion")}")
    }
}


val fatJar = task("fatJar", type = Jar::class) {

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    baseName = "${project.name}-fat"
    manifest {
        attributes["Implementation-Title"] = "Google Datastore entity browser by Alejandro Covarrubias"
        attributes["Implementation-Version"] = "1.0"
        attributes["Main-Class"] = "com.sicar.Workbench"
    }
    from(configurations.runtimeClasspath.get().map({ if (it.isDirectory) it else zipTree(it) }))
    with(tasks.jar.get() as CopySpec)
}

tasks {
    "build" {
        dependsOn(fatJar)
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
