package org.reactome.lit_ball.common


object App {
    fun beforeWindow() {
        SerialDB.open()
    }

//    fun getDBSize() : String { return querySet.size.toString() }

    fun afterWindow() {
    }
}
