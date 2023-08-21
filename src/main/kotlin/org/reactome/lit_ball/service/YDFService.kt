package org.reactome.lit_ball.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.AnnotatingRootStore
import java.io.BufferedReader
import java.io.InputStreamReader

const val CONSOLE_MAX_LIFE = 1000000L
object YDFService {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun doPredict(
        modelPath: String,
        datasetPath: String,
        resultPath: String,
        key: String,
    ): Boolean {
        fun executeCommand(command: String): Process {
            val builder = ProcessBuilder()
            return builder.command(*command.split(" ").toTypedArray()).start()
        }
        val process = executeCommand("predict --model=$modelPath --dataset=csv:$datasetPath --key=$key --output=csv:$resultPath")
        if (!process.isAlive)
            return false
        val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
        val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

        val stdoutChannel = AnnotatingRootStore.scope.produce(Dispatchers.IO) {
            var line: String
            while (stdoutReader.readLine().also { line = it } != null) { send(line) }
        }
        val stderrChannel = AnnotatingRootStore.scope.produce(Dispatchers.IO) {
            var line: String
            while (stderrReader.readLine().also { line = it } != null) { send(line) }
        }
        AnnotatingRootStore.scope.launch(Dispatchers.IO) {
            while (true) {
                val line = stdoutChannel.receive()
                if (line.contains("xyz"))
                    AnnotatingRootStore.setProgressIndication(Pair(.5f, "5/10"))
            }
        }
        AnnotatingRootStore.scope.launch(Dispatchers.IO) {
            while (true) {
                val line = stderrChannel.receive()
                if (line.contains("abc"))
                    throw Exception()
            }
        }
        AnnotatingRootStore.scope.launch (Dispatchers.IO) {
            process.onExit().get()
            stdoutChannel.cancel()
            stderrChannel.cancel()
        }
        AnnotatingRootStore.scope.launch (Dispatchers.IO) {
            delay(CONSOLE_MAX_LIFE)
            process.destroy()
        }
        return true
    }
}