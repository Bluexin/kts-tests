package be.bluexin.ktstests.host

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlin.script.experimental.api.*
import kotlin.script.experimental.jvmhost.BasicJvmScriptingHost

/**
 * Very simplistic, no concurrent calls to eval supported
 * Only caches based on the script itself, not on compilation configuration
 */
class BasicCachingJvmScriptingHost : BasicJvmScriptingHost() {
    private val cache = mutableMapOf<SourceCode, Deferred<ResultWithDiagnostics<CompiledScript>>>()

    override fun eval(
        script: SourceCode,
        compilationConfiguration: ScriptCompilationConfiguration,
        evaluationConfiguration: ScriptEvaluationConfiguration?
    ): ResultWithDiagnostics<EvaluationResult> = runInCoroutineContext {
        cache.getOrPut(script) {
            supervisorScope {
                async {
                    compiler(script, compilationConfiguration)
                }
            }
        }.await().onSuccess {
            evaluator(it, evaluationConfiguration ?: ScriptEvaluationConfiguration.Default.with {
                // TODO : investigate use of this option
//                enableScriptsInstancesSharing()
            })
        }
    }
}