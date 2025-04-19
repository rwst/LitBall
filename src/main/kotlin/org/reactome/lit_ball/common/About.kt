package common

import org.reactome.lit_ball.BuildConfig

object About {
    const val VERSION: String = BuildConfig.APP_VERSION
    val text = """
        LitBall v. $VERSION
        (c) 2024 Ralf Stephan <gtrwst9@gmail.com>
        Repo: https://github.com/rwst/LitBall
        
        Changes history:""".trimIndent()
}