package be.bluexin.ktstests.host

import be.bluexin.ktstests.scriptdef.BlueScriptCtorArgs
import io.mockk.mockk
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import kotlin.script.experimental.api.EvaluationResult
import kotlin.script.experimental.api.ResultValue
import kotlin.script.experimental.api.ResultWithDiagnostics
import kotlin.script.experimental.host.toScriptSource
import kotlin.test.assertIs

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HostTest {

    @MethodSource("scripts using banned packages or classes")
    @ParameterizedTest
    fun `access to banned packages or classes is prevented`(snippet: String) {
        val result = Host.evalAs(
            snippet.toScriptSource("test snippet"),
            BlueScriptCtorArgs(mockk(), 0),
        )
        assertIs<ResultWithDiagnostics.Success<EvaluationResult>>(result)
        val rv = result.value.returnValue
        assertIs<ResultValue.Error>(rv)
        assertIs<NoClassDefFoundError>(rv.error)
    }

    @Language("kts")
    private fun `scripts using banned packages or classes`() = listOf(
        "kotlin.system.exitProcess(0)", // => java.lang.System
        """kotlin.io.path.Path("")""", // => java.nio.Paths
        """java.io.File("")"""
    )

    @MethodSource("legal script code")
    @ParameterizedTest
    fun `access to banned packages via helpers is ok`(snippet: String) {
        val result = Host.evalAs(
            snippet.toScriptSource("test snippet"),
            BlueScriptCtorArgs(mockk(), 0),
        )
        assertIs<ResultWithDiagnostics.Success<EvaluationResult>>(result)
        val rv = result.value.returnValue
        assertIs<ResultValue.Value>(rv)
    }

    @Language("kts")
    private fun `legal script code`() = listOf(
        """readResourceSafely("build.gradle.kts")""", // => java.io.*
    )

    @MethodSource("scripts using non whitelisted libraries")
    @ParameterizedTest
    fun `access to non whitelisted libraries is prevented`(snippet: String) {
        val result = Host.evalAs(
            snippet.toScriptSource("test snippet"),
            BlueScriptCtorArgs(mockk(), 0),
        )
        assertIs<ResultWithDiagnostics.Failure>(result)
    }

    @Language("kts")
    private fun `scripts using non whitelisted libraries`() = listOf(
        "be.bluexin.ktstests.host.Host"
    )
}
