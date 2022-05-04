@file:Suppress("PropertyName")

package com.github.synth

import net.minecraftforge.gradle.userdev.DependencyManagementExtension
import org.gradle.api.*
import org.gradle.api.model.*
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import javax.inject.Inject


abstract class SynthExtension @Inject constructor(objects: ObjectFactory) {
    internal val versions: Versions = objects.newInstance(Versions::class.java)
    internal val mappings: Mappings = objects.newInstance(Mappings::class.java)
    internal val sources: Sources = objects.newInstance(Sources::class.java)
    internal val modInfo: ModInfo = objects.newInstance(ModInfo::class.java)
    internal val dependencies: Dependencies = objects.newInstance(Dependencies::class.java)

    fun info(action: Action<ModInfo>) = action.execute(modInfo)

    /**
     * Adds a configuration
     */
    fun versions(action: Action<Versions>) = action.execute(versions)

    /**
     * Gets the mappings versions
     */
    fun mappings(action: Action<Mappings>) = action.execute(mappings)

    /**
     * Add your dependencies here
     */
    fun dependencies(action: Action<Dependencies>) = action.execute(dependencies)

    /**
     * The sources used for the mod's sourcesets
     */
    fun sources(action: Action<Sources>) = action.execute(sources)
}

/**
 * Stores the dependencies
 */
open class Dependencies @Inject constructor(val project: Project) {
    fun compile(notation: Any) =
        add("compileOnly", notation, false)

    fun implement(notation: Any) =
        add("implementation", notation, false)

    fun runtime(notation: Any) =
        add("compileOnly", notation, false)

    fun modCompile(notation: Any) =
        add("compileOnly", notation, true)

    fun modImplement(notation: Any) =
        add("implementation", notation, true)

    fun modRuntime(notation: Any) =
        add("runtimeOnly", notation, true)

    fun curseCompile(name: String, projectId: String, fileId: String) =
        add("runtimeOnly", "curse.maven:$name-$projectId:$fileId", true)

    fun curseRuntime(name: String, projectId: Int, fileId: Int) =
        add("runtimeOnly", "curse.maven:$name-$projectId:$fileId", true)


    private fun add(name: String, notation: Any, deobf: Boolean) {
        val depends = project.dependencies
//        println(project.name)
//        println(project.configurations.names)
        val fg = project.extensions.getByType(DependencyManagementExtension::class.java)
        depends.add(name, if (deobf) fg.deobf(notation) else notation)
    }

    fun jei(version: String) {
        val mcVersion = project.extensions.getByType(SynthExtension::class.java).versions.minecraft
        modCompile("mezz.jei:jei-${mcVersion}-forge-api:$version")
        modCompile("mezz.jei:jei-${mcVersion}-common-api:$version")
        modRuntime("mezz.jei:jei-${mcVersion}-forge:$version")
    }

    fun shaded(notation: Any) = add("library", notation, false)
}

open class ModInfo @Inject constructor(objects: ObjectFactory, project: Project) {
    var modid: String = project.name.toString()
    var version: String = project.version.toString()
    var group: String = project.group.toString()
    var displayName: String = modid.capitalize()
    var author: String = ""
    var description: String = ""
    var credits: List<String> = listOf(author)
        private set

    fun credits(vararg credits: String) {
        this.credits = credits.toList()
    }

    fun credit(credit: String) {
        this.credits = listOf(credit)
    }

}

open class Mappings @Inject constructor(objects: ObjectFactory) {
    //Used to auto set versions
    private val versions = objects.property(Versions::class.java).getOrElse(Versions())

    var channel = "parchment"
    var version = "1.18.2-2022.03.13-1.18.2"

    /**
     * Creates a parchment
     */
    fun parchment(version: String, mc_version: String = versions.minecraft) {
        this.channel = "parchment"
        this.version = "${version}-${mc_version}"
    }

    /**
     * uses the offical mappings version
     */
    fun official(version: String = versions.minecraft) {
        this.channel = "official"
        this.version = version
    }
}

open class Versions {
    var minecraft = "1.18.2"
    var forge = "40.1.0"
}

open class Sources @Inject constructor(project: Project) {
    internal val sources: MutableList<SourceSet> =
        arrayListOf(project.extensions.getByType(SourceSetContainer::class.java).getByName("main"))

    /**
     * sets the sourceSets to a single element array of the source
     */
    fun source(source: SourceSet) = with(sources) { clear(); add(source) }

    /**
     * Sets the source sets to the sourceset array
     */
    fun sources(vararg sources: SourceSet) = sources(sources.toList())


    /**
     * Sets the sources to the array passed
     */
    fun sources(sources: List<SourceSet>) = this.sources.addAll(sources)


}