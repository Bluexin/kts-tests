package be.bluexin.ktstests.scriptdef

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.random.Random
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm

/**
 * These two make it a bit verbose but enable cool shenanigans
 */
interface ScriptDefinition<out ConstructorArgs : ScriptArguments<ScriptDefinition<ConstructorArgs>>>
interface ScriptArguments<out ScriptType : ScriptDefinition<ScriptArguments<ScriptType>>>

/**
 * Superclass for script instances
 */
@Suppress("unused", "MemberVisibilityCanBePrivate") // used in scripts
@KotlinScript(
    displayName = "BlueScript",
    fileExtension = "bluescript.kts",
    compilationConfiguration = BlueScriptCompilationConfiguration::class,
    evaluationConfiguration = BlueScriptEvaluationConfiguration::class
)
abstract class BlueScript(ctorArgs: BlueScriptCtorArgs) : Tree by ctorArgs.tree, ScriptDefinition<BlueScriptCtorArgs> {
    private val tree = ctorArgs.tree
    protected val localRandom = Random(Random.nextLong())
    val id = ctorArgs.id

    fun doSomething() = logger.info { "Doing something in id:$id" }

    override fun toString() = "BlueScript(id=$id, tree=$tree)"

    protected val logger = KotlinLogging.logger("script-$id")

    /**
     * just imagine this has a safeguard
     */
    fun readResourceSafely(path: String) = File(path).readLines()
}

data class BlueScriptCtorArgs(val tree: Tree, val id: Int) : ScriptArguments<BlueScript>

object BlueScriptCompilationConfiguration : ScriptCompilationConfiguration({
    defaultImports(EventHandler::class)
    jvm {
        // Probably something to tweak, sounds unsafe
        dependenciesFromClassContext(
            BlueScript::class,
            "kts-scriptdef",
            "kotlin-stdlib",
            "kotlinx-coroutines-core-jvm",
            "kotlin-logging-jvm",
        )
        // below doesn't seem to work
        /*dependenciesFromClassloader(
            "kts-scriptdef",
            "kotlin-stdlib",
            "kotlinx-coroutines-core-jvm",
            "kotlin-logging-jvm",
            classLoader = object : ClassLoader("Classloader for addons", BlueScript::class.java.classLoader) {
                override fun loadClass(name: String, resolve: Boolean) = when {
                    name.startsWith("java.io") -> throw NoClassDefFoundError(name)
                    else -> super.loadClass(name, resolve)
                }
            }
        )*/
    }

    ide {
        this.acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }

    refineConfiguration {
        onAnnotations<EventHandler> { context ->
            val annotations =
                context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf { it.isNotEmpty() }
                    ?: return@onAnnotations context.compilationConfiguration.asSuccess()
            return@onAnnotations runBlocking {
                // Do some stuff maybe for actual dependencies ? Can't let it resolve from interwebz but could check provided libraries
                // See https://kotlinlang.org/docs/custom-script-deps-tutorial.html#create-a-script-definition
                // Uses to resolve dependencies for the script
                context.compilationConfiguration.asSuccess()
            }
        }

        beforeCompiling {
            val data = it.collectedData
            it.compilationConfiguration.asSuccess()
        }
    }
})

object BlueScriptEvaluationConfiguration : ScriptEvaluationConfiguration({
    refineConfigurationBeforeEvaluate {
        val cs = it.compiledScript
        it.evaluationConfiguration.asSuccess()
    }
})

annotation class EventHandler
