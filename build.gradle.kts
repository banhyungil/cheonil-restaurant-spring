plugins {
    java
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.asciidoctor.jvm.convert") version "4.0.5"
    id("com.diffplug.spotless") version "7.0.2"
}

group = "ban.gil"

version = "0.0.1-SNAPSHOT"

description = "cheonil-restaurant-spring"

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }

repositories { mavenCentral() }

extra["snippetsDir"] = file("build/generated-snippets")

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.2")
    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.postgresql:postgresql")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-restdocs")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-websocket-test")
    // Spring Boot 4 — test slice 어노테이션이 도메인별 모듈로 분리됨.
    testImplementation("org.springframework.boot:spring-boot-data-jpa-test") // @DataJpaTest
    testImplementation("org.springframework.boot:spring-boot-jdbc-test") // @AutoConfigureTestDatabase
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    // Testcontainers 2.x — artifact 이름이 testcontainers-* prefix 로 변경됨
    // (1.x: postgresql / junit-jupiter → 2.x: testcontainers-postgresql / testcontainers-junit-jupiter)
    testImplementation(platform("org.testcontainers:testcontainers-bom:2.0.4"))
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testCompileOnly("org.projectlombok:lombok")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testAnnotationProcessor("org.projectlombok:lombok")
}

tasks.withType<Test> { useJUnitPlatform() }

tasks.test { outputs.dir(project.extra["snippetsDir"]!!) }

tasks.asciidoctor {
    inputs.dir(project.extra["snippetsDir"]!!)
    dependsOn(tasks.test)
}

spotless {
    java {
        target("src/**/*.java")
        targetExclude("**/build/**", "**/generated/**")

        googleJavaFormat("1.34.0")
        removeUnusedImports()
        importOrder("java", "javax", "jakarta", "org", "com", "")
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktfmt().kotlinlangStyle()
    }
}

tasks.named("check") { dependsOn("spotlessCheck") }
