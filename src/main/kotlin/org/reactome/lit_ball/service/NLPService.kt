package service

object NLPService {
    private var initialized = false
    private lateinit var notWordRegex: Regex
    private lateinit var wsRegex: Regex
    fun init() {
        if (initialized) return
        notWordRegex = Regex("[^\\w\\s]")
        wsRegex = "\\s+".toRegex()
        initialized = true
    }

    fun preprocess(s: String?): String {
        if (!initialized) init()
        if (s == null) return ""
        var str = ""
        s.replace(notWordRegex, "").split(wsRegex).forEach {
            str += "$it "
        }
        return str
    }
}
