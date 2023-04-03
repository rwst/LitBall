package org.reactome.lit_ball.common


object App {
    private fun dbTest(i: Int) {
        SerialDB.open()
        val map: DBType = SerialDB.get()
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
