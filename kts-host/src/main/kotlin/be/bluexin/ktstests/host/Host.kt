package be.bluexin.ktstests.host

import be.bluexin.ktstests.scriptdef.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.ScriptingHostConfiguration
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvm.CompiledJvmScriptsCache
import kotlin.script.experimental.jvm.baseClassLoader
import kotlin.script.experimental.jvm.compilationCache
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

fun main(vararg args: String) {
    if (args.size != 1) {
        logger.info { "usage: <app> <script file>" }
    } else {
        val scriptFile = File(args[0])
        if (scriptFile.isFile) repeat(5) {
            runEvalPrintLoop(scriptFile)
        } else {
            logger.info { "Not a file : $scriptFile" }
        }
    }
}

private val logger = KotlinLogging.logger { }

private fun runEvalPrintLoop(
    sf: File
) {
    val sc = sf.toScriptSource()
    val tree = TreeImpl()
    logger.info { "\n---- Executing script $sf ----" }
    repeat(20) { idx ->
        evalPrint(sc) { Host.evalFile(it, tree, idx) }
    }
    repeat(3) {
        logger.info { "Growing from host" }
        val eventHandlersTime = measureTime {
            tree.grow()
        }
        logger.info { "Result : $tree (handlers from code duration : $eventHandlersTime)" }
    }
}

private fun evalPrint(sourceCode: SourceCode, fn: (SourceCode) -> ResultWithDiagnostics<EvaluationResult>) {
    val (res, time) = measureTimedValue {
        fn(sourceCode)
    }
    res.reports.forEach {
        if (it.severity > ScriptDiagnostic.Severity.WARNING) {
            logger.error { "${it.severity} : ${it.message}" + if (it.exception == null) "" else ": ${it.exception}" }
        }
    }
    res.onSuccess {
        when (val rv = it.returnValue) {
            is ResultValue.Error -> logger.error(rv.error) { "Failed evaluation of ${sourceCode.name}:${sourceCode.locationId} !" }
            else -> logger.info { "Evaluation result value : $rv from ${rv.scriptInstance}" }
        }

        Unit.asSuccess()
    }.onFailure {
        logger.error { "Failed to run !" }
    }
    logger.info { "Ran in $time" }
}

object Host {

    val scriptingHost = BasicJvmScriptingHost(ScriptingHostConfiguration {
        jvm {
            compilationCache(object : CompiledJvmScriptsCache {
                private val cache = mutableMapOf<SourceCode, CompiledScript>()

                override fun get(
                    script: SourceCode,
                    scriptCompilationConfiguration: ScriptCompilationConfiguration
                ) = cache[script]

                override fun store(
                    compiledScript: CompiledScript,
                    script: SourceCode,
                    scriptCompilationConfiguration: ScriptCompilationConfiguration
                ) {
                    cache[script] = compiledScript
                }
            })
            baseClassLoader(object : ClassLoader("Classloader for addons", BlueScript::class.java.classLoader) {
                private fun error(name: String): Nothing = throw NoClassDefFoundError("Forbidden class : $name")

                override fun loadClass(name: String, resolve: Boolean) = when (name) {
                    "java.lang.System" -> error(name)
                    else -> when (name.split(".").take(2).joinToString(".")) {
                        "java.io", "java.nio" -> {
                            // We don't actually need to walk the stack ??!
                            /*val caller2 = StackWalker.getInstance(
                                setOf(
                                    StackWalker.Option.SHOW_HIDDEN_FRAMES,
                                    StackWalker.Option.SHOW_REFLECT_FRAMES,
                                    StackWalker.Option.RETAIN_CLASS_REFERENCE
                                ), 5
                            ).walk {
                                it.skip(5)*//*.takeWhile {  }*//*.toList()
                            }.toList()*/
//                                super.loadClass(name, resolve)
                            error(name)
                        }

                        else -> super.loadClass(name, resolve)
                    }
                }
            })
        }
    })

    fun evalFile(scriptFile: SourceCode, tree: Tree, id: Int): ResultWithDiagnostics<EvaluationResult> =
        scriptingHost.evalWithTemplate<BlueScript>(
            scriptFile,
            evaluation = {
                constructorArgs(BlueScriptCtorArgs(tree, id))
            }
        )

    inline fun <reified ScriptType : ScriptDefinition<ConstructorArgs>, ConstructorArgs : ScriptArguments<ScriptType>> evalAs(
        scriptFile: SourceCode,
        args: ConstructorArgs
    ) = scriptingHost.evalWithTemplate<ScriptType>(
        scriptFile,
        evaluation = {
            constructorArgs(args)
        }
    )
}

private data class TreeImpl(var size: Int = 0) : Tree {
    private val listeners = mutableListOf<OnSizeChange>()

    override fun grow() = notify {
        logger.info { "The tree grows in size" }
        ++size
    }

    override fun harvest() {
        if (size > 0) notify {
            logger.info { "The tree is being harvested" }
            --size
        } else logger.info { "The tree can't be harvested..." }
    }

    private inline fun notify(action: () -> Unit) {
        val oldSize = size
        action()
        listeners.forEach {
            runBlocking {
                it(oldSize, size)
            }
        }
    }

    override fun onSizeChange(callback: OnSizeChange) {
        listeners += callback
    }
}
