package be.bluexin.ktstests.host

import be.bluexin.ktstests.scriptdef.BlueScript
import be.bluexin.ktstests.scriptdef.OnSizeChange
import be.bluexin.ktstests.scriptdef.Tree
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.script.experimental.api.*
import kotlin.script.experimental.host.toScriptSource
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost
import kotlin.time.measureTime
import kotlin.time.measureTimedValue

fun main(vararg args: String) {
    if (args.size != 1) {
        println("usage: <app> <script file>")
    } else {
        val scriptFile = File(args[0])
        if (scriptFile.isFile) repeat(5) {
            runEvalPrintLoop(scriptFile, "without", Host::evalFile)
            runEvalPrintLoop(scriptFile, "with", Host::evalFileCaching)
        } else {
            println("Not a file : $scriptFile")
        }
    }
}

private inline fun runEvalPrintLoop(
    sf: File,
    caching: String,
    crossinline evalFn: (SourceCode, Tree, Int) -> ResultWithDiagnostics<EvaluationResult>
) {
    val sc = sf.toScriptSource()
    val tree = TreeImpl()
    println("\n---- Executing script $sf $caching caching ----")
    repeat(10) { idx ->
        Host.evalPrint(sc) { evalFn(it, tree, idx) }
    }
    repeat(2) {
        println("Growing from host")
        val eventHandlersTime = measureTime {
            tree.grow()
        }
        println("Result : $tree (handlers from code duration : $eventHandlersTime)")
    }
}

private object Host {
    fun evalPrint(sourceCode: SourceCode, fn: (SourceCode) -> ResultWithDiagnostics<EvaluationResult>) {
        val (res, time) = measureTimedValue {
            fn(sourceCode)
        }
        res.reports.forEach {
            if (it.severity > ScriptDiagnostic.Severity.WARNING) {
                println("${it.severity} : ${it.message}" + if (it.exception == null) "" else ": ${it.exception}")
            }
        }
        res.onSuccess {
            println("Evaluation result value : ${it.returnValue} from ${it.returnValue.scriptInstance}")
            ResultWithDiagnostics.Success(Unit)
        }.onFailure {
            println("Failed to run !")
        }
        println("Ran in $time")
    }

    private val basicHost = BasicJvmScriptingHost()

    fun evalFile(scriptFile: SourceCode, tree: Tree, id: Int): ResultWithDiagnostics<EvaluationResult> =
        basicHost.evalWithTemplate<BlueScript>(
            scriptFile,
            evaluation = {
                constructorArgs(tree, id)
            }
        )

    private val cachingHost by lazy { BasicCachingJvmScriptingHost() }

    fun evalFileCaching(scriptFile: SourceCode, tree: Tree, id: Int): ResultWithDiagnostics<EvaluationResult> =
        cachingHost.evalWithTemplate<BlueScript>(
            scriptFile,
            evaluation = {
                constructorArgs(tree, id)
            }
        )
}

private data class TreeImpl(var size: Int = 0) : Tree {
    private val listeners = mutableListOf<OnSizeChange>()

    override fun grow() = notify {
        println("The tree grows in size")
        ++size
    }

    override fun harvest() {
        if (size > 0) notify {
            println("The tree is being harvested")
            --size
        } else println("The tree can't be harvested...")
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
