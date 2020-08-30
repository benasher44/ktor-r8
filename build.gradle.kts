val ktlintVersion = "0.37.2"
val ktorVersion = "1.3.2"
val logbackVersion = "1.2.1"

plugins {
    kotlin("jvm") version "1.3.72"
    kotlin("plugin.serialization") version "1.3.72"
    application
}

application {
    application.mainClassName = "io.ktor.server.netty.EngineMain"
}

val ktlintConfiguration by configurations.creating
val r8Configuration by configurations.creating

val ktlint = tasks.register("ktlint", JavaExec::class) {
    group = JavaBasePlugin.VERIFICATION_GROUP
    val cliArgs = project.findProperty("ktlint_args") as? String
    val ktlintArgs = cliArgs?.split(" ") ?: listOf(
        "--reporter=plain?group_by_file",
        "--reporter=checkstyle,output=$buildDir/ktlint.xml",
        "src/**/*.kt",
        "buildSrc/**/*.kt"
    )
    description = "Check Kotlin code style."
    classpath = ktlintConfiguration
    main = "com.pinterest.ktlint.Main"
    args = ktlintArgs
}

val checkTask = tasks.named("check")
checkTask.configure {
    dependsOn(ktlint)
}

tasks.register("ktlintFormat", JavaExec::class) {
    group = "formatting"
    description = "Fix Kotlin code style deviations."
    classpath = ktlintConfiguration
    main = "com.pinterest.ktlint.Main"
    args = listOf("-F", "src/**/*.kt", "buildSrc/**/*.kt")
}

group = "com.example"
version = "0.0.1"

val fatJarProvider = tasks.register("fatJar", Jar::class) {
    dependsOn(configurations.runtimeClasspath)
    dependsOn(tasks.named("jar"))

    archiveClassifier.set("fat")

    manifest {
        attributes(
            "Main-Class" to application.mainClassName,
            "Implementation-Version" to project.version
        )
    }
    val sourceClasses = sourceSets.main.get().output.classesDirs
    val resources = sourceSets.main.get().output.resourcesDir
    inputs.files(sourceClasses)
    inputs.files(resources)

    doFirst {
        from(files(sourceClasses))
        from(files(resources))
        from(configurations.runtimeClasspath.get().asFileTree.files.map { zipTree(it) })

        exclude("**/*.kotlin_metadata")
        exclude("**/*.kotlin_module")
        exclude("**/module-info.class")
        exclude("META-INF/maven/**")
        exclude("META-INF/proguard/**")
        exclude("META-INF/*.version")
        exclude("**/*.proto")
        exclude("LICENSE")
        exclude("NOTICE")
        exclude("r8-version.properties")
    }
}

tasks.named("build").configure {
    dependsOn(fatJarProvider)
}

val r8File = File("$buildDir/libs/${rootProject.name}-r8.jar")
val r8Jar = tasks.register("r8Jar", JavaExec::class) {
    dependsOn(configurations.runtimeClasspath)
    dependsOn(fatJarProvider)

    val fatJarFile = fatJarProvider.get().archiveFile.get()
    inputs.file(fatJarFile)
    outputs.file(r8File)

    classpath(r8Configuration)
    main = "com.android.tools.r8.R8"
    args = listOf(
        "--release",
        "--classfile",
        "--output", r8File.toString(),
        "--pg-conf", "src/main/r8.txt",
        "--lib", System.getProperty("java.home"),
        fatJarFile.toString()
    )
}

tasks.register("r8Help", JavaExec::class) {
    classpath(r8Configuration)
    main = "com.android.tools.r8.R8"
    args = listOf("--help")
}

sourceSets {
    val main by getting {
        resources.srcDir("resources")
    }
    val test by getting {
        resources.srcDir("testresources")
    }
}

repositories {
    mavenCentral()
    google()
    jcenter()
}

dependencies {
    ktlintConfiguration("com.pinterest:ktlint:$ktlintVersion")
    r8Configuration("com.android.tools:r8:2.1.66")

    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
    implementation("io.ktor:ktor-serialization:$ktorVersion")
}
