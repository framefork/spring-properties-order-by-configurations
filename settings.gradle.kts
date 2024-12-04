import kotlin.io.path.listDirectoryEntries

pluginManagement {
}

dependencyResolutionManagement {
    versionCatalogs {
    }
}

rootProject.name = "spring-properties-order-by-configurations"

file("${rootProject.projectDir}/modules").toPath().listDirectoryEntries().forEach { moduleDir ->
    include("${moduleDir.fileName}")
    project(":${moduleDir.fileName}").projectDir = moduleDir.toFile()
}

file("${rootProject.projectDir}/testing").toPath().listDirectoryEntries().forEach { moduleDir ->
    include("${moduleDir.fileName}")
    project(":${moduleDir.fileName}").projectDir = moduleDir.toFile()
}
