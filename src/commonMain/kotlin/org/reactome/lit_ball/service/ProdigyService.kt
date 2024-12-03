package org.reactome.lit_ball.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.reactome.lit_ball.common.Settings
import model.AnnotatingRootStore
import org.reactome.lit_ball.util.Logger
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

private const val CONSOLE_MAX_LIFE = 1000000L

object ProdigyService {
    private const val TAG = "ProdigyService"
    private var pythonPath: String? = Settings.map["PYTHONPATH"]

    private fun executeCommand(command: String, directory: String): Process {
        val builder = ProcessBuilder()
        return builder.directory(File(directory)).command(*command.split(" ").toTypedArray()).start()
    }

    fun call(cmd: String, args: String, directory: String): Job? {
        if (pythonPath == null) {
            return null
        }
        val process =
            executeCommand("${pythonPath}/bin/python3 -m $cmd $args", directory)

        if (!process.isAlive)
            return null
        val stderrReader = BufferedReader(InputStreamReader(process.errorStream))
        val stderrChannel = Channel<String>()
        AnnotatingRootStore.scope?.launch(Dispatchers.IO) {
            do {
                val line = stderrReader.readLine() ?: break
                stderrChannel.send(line)
            } while (true)
        }
        AnnotatingRootStore.scope?.launch(Dispatchers.IO) {
            while (true) {
                val line = stderrChannel.receive()
                if (line.startsWith("[INFO "))
                    Logger.i(TAG, line)
                if (line.contains("[FATAL"))
                    throw Exception(line)
            }
        }
        val job = AnnotatingRootStore.scope?.launch(Dispatchers.IO) {
            process.onExit().get()
            stderrChannel.cancel()
        }
        AnnotatingRootStore.scope?.launch(Dispatchers.IO) {
            delay(CONSOLE_MAX_LIFE)
            process.destroy()
        }
        return job
    }
}
