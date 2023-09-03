package be.bluexin.ktstests.scriptdef

import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm

/**
 * Superclass for script instances
 */
@Suppress("unused", "MemberVisibilityCanBePrivate") // used in scripts
@KotlinScript(
    fileExtension = "bluescript.kts",
    compilationConfiguration = BlueScriptCompilationConfiguration::class
)
abstract class BlueScript(private val tree: Tree, val id: Int) : Tree by tree {
    protected val localRandom = Random(Random.nextLong())

    fun doSomething() = println("Doing something in id:$id")

    abstract fun testAbstract()

    override fun toString() = "BlueScript(id=$id, tree=$tree)"
}

object BlueScriptCompilationConfiguration : ScriptCompilationConfiguration({
    defaultImports(EventHandler::class)
    jvm {
        // Probably something to tweak, sounds unsafe
        dependenciesFromClassContext(
            BlueScript::class,
            "kts-scriptdef",
            "kotlin-stdlib",
            "kotlinx-coroutines-core-jvm"
        )
    }

    ide {
        this.acceptedLocations(ScriptAcceptedLocation.Everywhere)
    }

    refineConfiguration {
        onAnnotations<EventHandler> { context ->
            val annotations = context.collectedData?.get(ScriptCollectedData.collectedAnnotations)?.takeIf { it.isNotEmpty() }
                ?: return@onAnnotations context.compilationConfiguration.asSuccess()
            return@onAnnotations runBlocking {
                // Do some stuff maybe for actual dependencies ? Can't let it resolve from interwebz but could check provided libraries
                // See https://kotlinlang.org/docs/custom-script-deps-tutorial.html#create-a-script-definition
                // Uses to resolve dependencies for the script
                context.compilationConfiguration.asSuccess()
            }
        }
    }
})

annotation class EventHandler
