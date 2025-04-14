package common

import org.reactome.lit_ball.BuildConfig

object About {
    val version: String = BuildConfig.APP_VERSION
    val text = """
        LitBall v. $version
        (c) 2024 Ralf Stephan <gtrwst9@gmail.com>
        Repo: https://github.com/rwst/LitBall
        
        Changes history:""".trimIndent()
}