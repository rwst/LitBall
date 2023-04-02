package org.reactome.lit_ball.common

import org.reactome.lit_ball.util.SerialDB


object App {
    private fun dbTest(i: Int) {
        val map: MutableMap<String, Query> = SerialDB.get()
        println(map.size)
        val q = Query(i)
        map["a$i"] = q
        SerialDB.commit()
        SerialDB.close()
    }
    fun beforeWindow() {
        dbTest(1)
        dbTest(2)
    }

//    fun getDBSize() : String { return querySet.size.toString() }

    fun afterWindow() {
    }
}
