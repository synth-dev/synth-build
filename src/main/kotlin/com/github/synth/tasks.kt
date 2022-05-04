package com.github.synth

import com.sun.swing.internal.plaf.synth.resources.*
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.*
import java.nio.file.Files
import java.nio.file.Path

open class GenerateAssetsFolder : DefaultTask() {
    @TaskAction
    fun generate() {
        val ext = project.extensions.getByType(SynthExtension::class.java)
        ext.sources.sources.forEach {
            it.resources {
                it.srcDirs.forEach {
                    val assets = File(it, "assets${File.separator}${ext.modInfo.modid}")
                    if (!assets.exists()) assets.mkdirs()
                    val data = File(it, "data${File.separator}${ext.modInfo.modid}")
                    if (!data.exists()) data.mkdirs()
                }
            }
        }
    }

}

open class GenerateModObject : DefaultTask() {
    @TaskAction
    fun generate() {
        val ext = project.extensions.getByType(SynthExtension::class.java)
        val name = generateModNameFrom(ext.modInfo.modid)
        val modClass = generateModClass(ext, name)
        val files = getOutputFiles(ext, name)
        for (file in files) {
            Files.writeString(file, modClass)
        }
    }

    private fun getOutputFiles(synth: SynthExtension, name: String): List<Path> {
        val files = ArrayList<Path>()
        val group = synth.modInfo.group.replace(".", File.separator)

        synth.sources.sources.forEach {
            it.allJava.srcDirs.forEach {
                if (it.exists() && it.name == "kotlin") {
                    val dir = File(it, group)
                    if (!dir.exists()) dir.mkdirs()
                    files.add(File(dir, "${name}.kt").toPath())
                }
            }
        }
        return files
    }

    private fun generateModNameFrom(string: String): String {
        val split = string.split("-")
        if (split.isEmpty()) return string.capitalize()
        val sb = StringBuilder()
        for (str in split) sb.append(str.capitalize())
        return sb.toString()
    }

    private fun generateModClass(synth: SynthExtension, name: String): String {
        val info = synth.modInfo
        return """
          package ${info.group}
          
          import net.minecraftforge.fml.common.*
          
          @Mod($name.ModId)
          object $name {
              const val ModId: String = "${info.modid}"

              init {
                  //...initialize mod here 
              }
          }
        """.trimIndent()
    }
}

open class GenerateTomlTask : DefaultTask() {

    @TaskAction
    fun generate() {
        val ext = project.extensions.getByType(SynthExtension::class.java)
        val toml = generateToml(ext)
        val files = getOutputFiles(ext)
        for (file in files) {
            Files.writeString(file, toml)
        }
    }

    private fun getOutputFiles(synth: SynthExtension): List<Path> {
        val files = ArrayList<Path>()
        synth.sources.sources.forEach {
            it.resources {
                it.srcDirs.forEach {
                    val meta = File(it, "META-INF")
                    if (!meta.exists())
                        meta.mkdirs()
                    println(meta.path)
                    files.add(File(meta, "mods.toml").toPath())
                }
            }
        }
        return files
    }

    private fun generateToml(synth: SynthExtension): String {
        val info = synth.modInfo
        return """
            modLoader = "kotlinforforge"
            loaderVersion = "[3,)" # Require at least 3.x
            license = 'LGPL'
            [[mods]]
            modId = "${info.modid}"
            version = "${info.version}"
            displayName = "${info.displayName}"
            credits = "${info.author}"
            authors = "${info.author}"
            description = '''
            ${info.description.trimIndent()}
            '''
            [[dependencies.${info.modid}]]
            modId = "forge"
            mandatory = true
            versionRange = "[${synth.versions.forge}]"
            ordering = "NONE"
            side = "BOTH"
            [[dependencies.${info.modid}]]
            modId = "minecraft"
            mandatory = true
            versionRange = "[${synth.versions.minecraft}]"
            ordering = "NONE"
            side = "BOTH"
        """.trimIndent()
    }


}

open class GeneratePackMeta : DefaultTask() {

    @TaskAction
    fun generate() {
        val ext = project.extensions.getByType(SynthExtension::class.java)
        val toml = generateToml(ext)
        val files = getOutputFiles(ext)
        for (file in files) {
            Files.writeString(file, toml)
        }
    }

    private fun getOutputFiles(synth: SynthExtension): List<Path> {
        val files = ArrayList<Path>()
        synth.sources.sources.forEach {
            it.resources {
                it.srcDirs.forEach {
                    val meta = File(it, "pack.mcmeta")
                    files.add(meta.toPath())
                }
            }
        }
        return files
    }

    private fun generateToml(synth: SynthExtension): String {
        val info = synth.modInfo
        return """
            {
                "pack": {
                    "description": "${synth.modInfo.displayName} resources",
                    "pack_format": 6
                }
            }
        """.trimIndent()
    }
}