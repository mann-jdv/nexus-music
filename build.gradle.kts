import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.aboutlibraries) apply false
    alias(libs.plugins.aboutlibraries.multiplatform) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.sentry.gradle) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.build.config) apply false
    alias(libs.plugins.osdetector) apply false
    alias(libs.plugins.conveyor) apply false
    alias(libs.plugins.compose.hotReload) apply false
}

// Nexus Music version (see nexus.properties). Read once here and handed to every module through
// extra, so androidApp, vivoAndroidApp and composeApp's BuildKonfig can never disagree.
run {
    val props = java.util.Properties()
    rootProject.file("nexus.properties").inputStream().use { props.load(it) }
    val version = props.getProperty("nexus.version")?.trim()
        ?: error("nexus.properties: nexus.version is missing")
    val parts = version.split('.').map { it.toIntOrNull() }
    require(parts.size == 3 && parts.all { it != null && it in 0..99 }) {
        "nexus.properties: nexus.version must be MAJOR.MINOR.PATCH with each part 0..99, was '$version'"
    }
    val (major, minor, patch) = parts.map { it!! }
    extra["nexusVersionName"] = "$version-nexus"
    extra["nexusVersionCode"] = 1_000_000 + major * 10_000 + minor * 100 + patch
}

tasks.register<Delete>("Clean") {
    delete(rootProject.layout.buildDirectory)
}

subprojects {
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            if (project.findProperty("enableComposeCompilerReports") == "true") {
                arrayOf("reports", "metrics").forEach {
                    freeCompilerArgs.addAll(
                        listOf(
                            "-P",
                            "plugin:androidx.compose.compiler.plugins.kotlin:${it}Destination=${layout.buildDirectory.asFile.get().absolutePath}/compose_metrics",
                        ),
                    )
                }
            }
        }
    }

    // PipePipe and Brave both depend on com.github.TeamNewPipe:nanojson with different commit
    // hashes. Gradle's default resolver picks PipePipe's older 1d9e1aea... commit which lacks
    // JsonArray.streamAsJsonObjects(), causing NoSuchMethodError when Brave's fallback runs at
    // runtime. Force the latest upstream commit (newer than both libs ship) across every module
    // so the merged APK/JAR carries a nanojson with the API both extractors expect.
    configurations.all {
        resolutionStrategy {
            force("com.github.TeamNewPipe:nanojson:c7a6c1c08d16b6d5ecded34758e6415e07be2166")
        }
    }
}