rootProject.name = "backend-challenge-120"
file("exercises").listFiles()?.filter { it.isDirectory && File(it, "build.gradle.kts").isFile }?.sortedBy { it.name }?.forEach {
    include(it.name)
    project(":" + it.name).projectDir = it
}
