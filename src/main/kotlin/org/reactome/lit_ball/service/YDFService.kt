package service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import util.Logger
import java.io.BufferedReader
import java.io.InputStreamReader

private const val CONSOLE_MAX_LIFE = 1000000L

object YDFService {
    private const val TAG = "YDFService"
    var path: String = ""
    fun doPredict(
        modelScope: kotlinx.coroutines.CoroutineScope,
        modelPath: String,
        datasetPath: String,
        resultPath: String,
        key: String,
    ): Job? {
        fun executeCommand(command: String): Process {
            val builder = ProcessBuilder()
            return builder.command(*command.split(" ").toTypedArray()).start()
        }

        val process =
            executeCommand("$path/predict --model=$modelPath --dataset=csv:$datasetPath --key=$key --output=csv:$resultPath")
        if (!process.isAlive)
            return null
        val stderrReader = BufferedReader(InputStreamReader(process.errorStream))
        val stderrChannel = Channel<String>()
        modelScope.launch(Dispatchers.IO) {
            do {
                val line = stderrReader.readLine() ?: break
                stderrChannel.send(line)
            } while (true)
        }
        modelScope.launch(Dispatchers.IO) {
            while (true) {
                val line = stderrChannel.receive()
                if (line.startsWith("[INFO "))
                    Logger.i(TAG, line)
                if (line.contains("[FATAL"))
                    throw Exception(line)
            }
        }
        val job = modelScope.launch(Dispatchers.IO) {
            process.onExit().get()
            stderrChannel.cancel()
        }
        modelScope.launch(Dispatchers.IO) {
            delay(CONSOLE_MAX_LIFE)
            process.destroy()
        }
        return job
    }
}