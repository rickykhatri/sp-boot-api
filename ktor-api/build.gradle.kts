import org.gradle.api.file.DuplicatesStrategy

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	kotlin("plugin.jpa") version "1.9.25"
	id("org.springframework.boot") version "3.5.0"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	// NLP dependencies
	implementation("edu.stanford.nlp:stanford-corenlp:4.5.8")
	implementation("edu.stanford.nlp:stanford-corenlp:4.5.8:models")
	// Spring Boot and Kotlin dependencies
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-scripting-jsr223")
	implementation("org.flywaydb:flyway-core")
	runtimeOnly("com.h2database:h2")
	runtimeOnly("org.postgresql:postgresql")
	// Spring AI dependencies
	implementation(platform("org.springframework.ai:spring-ai-bom:1.1.8"))
	implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")
	// Spring Boot Actuator for monitoring and management
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar> {
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

