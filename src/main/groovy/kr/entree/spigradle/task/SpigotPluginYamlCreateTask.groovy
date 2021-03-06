package kr.entree.spigradle.task

import kr.entree.spigradle.asm.ByteInspector
import kr.entree.spigradle.extension.PluginAttributes
import kr.entree.spigradle.mapper.Mapper
import kr.entree.spigradle.util.Version
import kr.entree.spigradle.yaml.SpigradleRepresenter
import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

/**
 * Created by JunHyung Lim on 2019-12-12
 */
class SpigotPluginYamlCreateTask extends DefaultTask {
    @Input
    PluginAttributes attributes
    @Input
    String encoding = 'UTF-8'
    @Input
    Yaml yaml = createYaml()
    @Input
    Map<CopySpec, Boolean> includeTasks = new HashMap<>()

    @TaskAction
    def createPluginYaml() {
        def file = new File(temporaryDir, 'plugin.yml')
        file.newWriter(encoding).withCloseable {
            yaml.dump(createMap(), it)
        }
        (getJarTasks() + getFlattenIncludeTasks()).each {
            it.from file
        }
    }

    private Collection<CopySpec> getJarTasks() {
        return project.tasks.withType(Jar).findAll {
            includeTasks.getOrDefault(it, true)
        }
    }

    private Collection<CopySpec> getFlattenIncludeTasks() {
        return includeTasks.findAll { it.value }
                .collect { it.key }
    }

    def include(copySpec, Boolean whether = true) {
        if (copySpec instanceof CopySpec) {
            includeTasks.put(copySpec, whether)
        }
    }

    def exclude(copySpec, Boolean whether = true) {
        include(copySpec, !whether)
    }

    def createMap() {
        attributes.with {
            name = name ?: project.name
            version = version ?: project.version
        }
        if (attributes.main == null) {
            def inspected = new ByteInspector(project).doInspect()
            attributes.main = inspected.mainClass
        }
        def yamlMap = Mapper.mapping(attributes, true) as Map<String, Object>
        validateYamlMap(yamlMap)
        return yamlMap
    }

    static def validateYamlMap(Map<String, Object> yamlMap) {
        if (yamlMap.main == null) {
            throw new IllegalArgumentException("""\
                Spigradle couldn\'t find a main class automatically.
                Please manually present your main class using @kr.entree.spigradle.Plugin annotation
                or set the 'main' property in spigot {} block in build.gradle
                or just disable the spigotPluginYaml task like below.
                
                "tasks.spigotPluginYaml.enabled = false"\
            """.stripIndent())
        }
        if (yamlMap.'api-version' != null) {
            def rawApiVersion = yamlMap.'api-version'.toString()
            Version.parse(rawApiVersion).with {
                if (major < 1 || minor < 13) {
                    throw new IllegalArgumentException("""\
                        Invalid api-version configured:'$rawApiVersion'
                        It should be 1.13 or higher or null!\
                    """.stripIndent())
                }
                if (major == 1 && (13..15).contains(minor) && patch != null) {
                    throw new IllegalArgumentException("""\
                        Invalid api-version configured:'$rawApiVersion'
                        Valid format: $major.$minor
                    """.stripIndent())
                }
            }
        }
    }

    static Yaml createYaml() {
        def options = new DumperOptions()
        options.with {
            defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
            indicatorIndent = indent - 1
        }
        return new Yaml(new SpigradleRepresenter(), options)
    }
}
