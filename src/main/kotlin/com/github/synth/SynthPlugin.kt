package com.github.synth

import com.sun.swing.internal.plaf.synth.resources.*
import groovy.lang.*
import net.minecraftforge.gradle.common.util.*
import net.minecraftforge.gradle.userdev.*
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.*
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.internal.file.impl.DefaultFileMetadata.*
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.*
import java.time.*
import java.util.*

class SynthPlugin : Plugin<Project> {

    /**
     * Run some action involving the project
     */
    override fun apply(project: Project): Unit {
        project.beforeEvaluate {
            setupBuildscript.execute(project)
        }
        project.evaluationDependsOnChildren()

        applyPlugins.execute(project)

        setupExtensions.execute(project)
        configureRepositories.execute(project)
        configureMinecraftPlugin.execute(project)
        configureJar.execute(project)
        configureDependencies.execute(project)
        val ex = project.extensions.getByType(SynthExtension::class.java)
        project.tasks.register("${ex.modInfo.modid}Server", RunServerGame::class.java) {
            it.group = "synth"
        }
        project.tasks.register("${ex.modInfo.modid}Client", RunClientGame::class.java) {
            it.group = "synth"
        }
        project.tasks.register("generatePackMeta", GeneratePackMeta::class.java) {
            it.group = "synth"
        }

        project.tasks.register("generateToml", GenerateTomlTask::class.java) {
            it.group = "synth"
        }

        project.tasks.register("generateMain", GenerateModObject::class.java) {
            it.group = "synth"
        }

        project.tasks.register("generateFolders", GenerateAssetsFolder::class.java) {
            it.group = "synth"
        }

        project.tasks.register("generateAll") {
            it.finalizedBy(":generatePackMeta", ":generateToml", ":generateMain")
            it.group = "synth"
        }

    }


    /**
     * Setups up our custom extension
     */
    private val setupExtensions = ProjectRunnable { project ->
        project.extensions.create("synth", SynthExtension::class.java)
    }

    /**
     * Sets up the build script to include the correct dependencies
     */
    private val setupBuildscript = ProjectRunnable { project ->
        project.buildscript.repositories.add(project.repositories.maven {
            it.setUrl("https://maven.minecraftforge.net")
        })
        project.buildscript.repositories.add(project.repositories.maven {
            it.setUrl("https://maven.parchmentmc.org")
        })
        project.buildscript.repositories.add(project.repositories.maven {
            it.setUrl("https://repo.spongepowered.org/maven")
        })
        project.buildscript.repositories.add(project.repositories.mavenCentral())
        project.buildscript.dependencies.add("classpath", "org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.20")
        project.buildscript.dependencies.add("classpath", "org.jetbrains.kotlin:kotlin-serialization:1.6.20")
        project.buildscript.dependencies.add("classpath", "org.parchmentmc:librarian:1.2.0")
    }

    /**
     * Applies the plugins needed to run the project
     */
    private val applyPlugins = ProjectRunnable { project ->
        project.apply {
            it.plugin("net.minecraftforge.gradle")
            it.plugin("kotlin")
//            it.plugin("kotlinx-serialization")
            it.plugin("org.parchmentmc.librarian.forgegradle")
            it.from("https://raw.githubusercontent.com/thedarkcolour/KotlinForForge/site/thedarkcolour/kotlinforforge/gradle/kff-3.2.0.gradle")
        }
    }

    /**
     * Makes sure that we"re using jdk 17
     */
    private val correctJavaVersion = ProjectRunnable { project ->
        val java = project.extensions.getByType(JavaPluginExtension::class.java)
        java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    /**
     * Configure the minecraft plugin
     */
    private val configureMinecraftPlugin = ProjectRunnable { project ->
        val mc = project.extensions.getByType(MinecraftExtension::class.java)
        val synth = project.extensions.getByType(SynthExtension::class.java)
        val includes = synth.includes.includes
        val info = synth.modInfo
        val registerMod = { run: RunConfig ->
            run.mods.register(info.modid) {
                it.sources(synth.sources.sources)
            }
            println("HERE: $includes")

//            project.afterEvaluate {
            includes.forEach {
                println("HERE: ${it}")
                project.project(it) {

                    it.extensions.getByType(SourceSetContainer::class.java).getByName("main").resources {
                        it.srcDirs("src/generated/resources/")
                    }

                    println("HERE: ${it.path}")
                    val ex = it.extensions.getByType(SynthExtension::class.java)
                    run.mods.register(ex.modInfo.modid) {
                        it.sources(ex.sources.sources)
                    }
//                    }

                }
            }
        }

        mc.mappings(synth.mappings.channel, synth.mappings.version)
        val runs = mc.runs

        runs.create("client") {
            it.workingDirectory(project.file("run"))
            it.property("forge.logging.markers", "SCAN,LOADING,CORE")
            it.property("forge.logging.console.level", "debug")

            it.property("mixin.env.remapRefMap", "true")
            it.property("mixin.env.refMapRemappingFile", "${project.projectDir}/build/createSrgToMcp/output.srg")
            project.afterEvaluate { proj ->
                registerMod(it)
            }

        }
        mc.runs.create("server") {
            it.workingDirectory(project.file("run/server"))
            it.property("forge.logging.markers", "SCAN,LOADING,CORE")
            it.property("forge.logging.console.level", "debug")

            it.property("mixin.env.remapRefMap", "true")
            it.property("mixin.env.refMapRemappingFile", "${project.projectDir}/build/createSrgToMcp/output.srg")
            project.afterEvaluate { proj ->
                registerMod(it)
            }
        }

        mc.runs.create("data") {
            it.workingDirectory(project.file("run"))
            it.property("forge.logging.markers", "SCAN,LOADING,CORE")
            it.property("forge.logging.console.level", "debug")
            it.property("mixin.env.remapRefMap", "true")
            it.property("mixin.env.refMapRemappingFile", "${project.projectDir}/build/createSrgToMcp/output.srg")
            it.args(
                listOf(
                    "--mod",
                    synth.modInfo.modid,
                    "--all",
                    "--output",
                    project.file("src/generated/resources/"),
                    "--existing",
                    project.file("src/main/resources")
                )
            )
            project.afterEvaluate { proj ->
                registerMod(it)
            }
        }
    }

    /**
     * Configures the repos and dependencies for the actual project
     */
    private val configureRepositories = ProjectRunnable { project ->
        project.repositories.add(project.repositories.mavenCentral())

        project.repositories.add(project.repositories.maven { repo ->
            repo.setUrl("https://maven.blamejared.com")
        })
        project.repositories.add(project.repositories.maven { repo ->
            repo.setUrl("https://dvs1.progwml6.com/files/maven/")
        })
        project.repositories.add(project.repositories.maven { repo ->
            repo.setUrl("https://modmaven.dev/")
        })
        project.repositories.add(project.repositories.maven { repo ->
            repo.setUrl("https://cursemaven.com")
            repo.content {
                it.includeGroup("curse.maven")
            }
        })
        project.repositories.add(project.repositories.maven { repo ->
            repo.setUrl("https://maven.k-4u.nl")
        })

        project.repositories.add(project.repositories.maven { repo ->
            repo.setUrl("https://maven.blakesmods.com")
        })
        project.repositories.add(project.repositories.maven { repo ->
            repo.setUrl("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/")
        })
    }

    /**
     * Configures all the dependencies
     */
    private val configureDependencies = ProjectRunnable { project ->
        val ext = project.extensions.getByType(SynthExtension::class.java)
        project.dependencies.add(
            "minecraft",
            "net.minecraftforge:forge:${ext.versions.minecraft}-${ext.versions.forge}"
        )
    }

    /**
     * Configures the jar and adds the finalized output reofb
     */
    private val configureJar = ProjectRunnable { project ->
        val synth = project.extensions.getByType(SynthExtension::class.java)
        val info = synth.modInfo
        project.tasks.getByName("jar").configure(closureOf<Jar> {
            manifest {
                it.attributes(
                    mapOf(
                        "Specification-Title" to info.displayName,
                        "Specification-Vendor" to info.author,
                        "Specification-Version" to "1",
                        "Implementation-Title" to info.displayName,
                        "Implementation-Version" to info.version,
                        "Implementation-Vendor" to info.author,
                        "Implementation-Timestamp" to LocalDateTime.now()
                    )
                )
            }
            finalizedBy("${project.path}:reobfJar")
        })
    }


    /**
     * Apply some action to the project1
     */
    fun interface ProjectRunnable {
        /**
         * Run some action involving the project
         */
        fun execute(project: Project)
    }

    /**
     * Create and empty closure for us to use in kotlin
     */
    fun <T> Any.closureOf(action: T.() -> Unit) = object : Closure<Any>(this, this) {
        @Suppress("unused") // to be called dynamically by Groovy
        fun doCall() = org.gradle.internal.Cast.uncheckedCast<T>(delegate)?.action()
    }

}
