// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // These plugins are declared here but not applied.
    // The 'apply false' makes them available to sub-modules like :app
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}

// Add a 'clean' task to the root project to delete the build directory.
task("clean", Delete::class) {
    delete(layout.buildDirectory) // <-- Modern, configuration-cache-friendly way
}