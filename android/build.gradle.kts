tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}

// Workaround for cross-drive path issues on Windows
// Disables unit test compilation tasks that reference plugin sources on different drives
subprojects {
    afterEvaluate {
        // Disable unit test compilation
        tasks.matching { 
            it.name.contains("UnitTest", ignoreCase = true) && 
            it.name.contains("compile", ignoreCase = true) 
        }.configureEach {
            enabled = false
        }
        
        // Disable Kotlin incremental compilation for plugins (fixes cross-drive path errors)
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            incremental = false
        }
    }
}
