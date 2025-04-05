plugins {
    alias(libs.plugins.org.jetbrains.kotlin.jvm)
    alias(libs.plugins.org.jetbrains.kotlinx.kover)
    `maven-publish`
    signing
}

group = "com.github.asm0dey"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.stax.api)

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)

    explicitApi()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
        vendor = JvmVendorSpec.BELLSOFT
    }
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("staks")
                description.set("A lightweight, idiomatic Kotlin library for XML parsing with a fluent DSL")
                url.set("https://github.com/asm0dey/staks")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("asm0dey")
                        name.set("Pasha Finkelshteyn")
                        email.set("pavel.finkelshtein@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/asm0dey/staks.git")
                    developerConnection.set("scm:git:ssh://github.com:asm0dey/staks.git")
                    url.set("https://github.com/asm0dey/staks")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = System.getenv("OSSRH_USERNAME") ?: project.findProperty("ossrhUsername") as String?
                password = System.getenv("OSSRH_PASSWORD") ?: project.findProperty("ossrhPassword") as String?
            }
        }
    }
}

signing {
    val signingKey = System.getenv("SIGNING_KEY") ?: project.findProperty("signingKey") as String?
    val signingPassword = System.getenv("SIGNING_PASSWORD") ?: project.findProperty("signingPassword") as String?

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["maven"])
    }
}
