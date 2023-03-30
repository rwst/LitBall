package org.reactome.lit_ball.common

import org.mapdb.DB
import org.mapdb.DBMaker

object App {
    private val db = DBMaker.fileDB("file.db").make()
    private lateinit var querySet: QuerySet
    fun beforeWindow() {
        querySet = DB.HashSetMaker<Query>(db, "querySet").createOrOpen()
        querySet += Query()
        db.commit()
    }

    fun getDBSize() : String { return querySet.size.toString() }

    fun afterWindow() {
        println(querySet.size)
        db.close()
    }
}