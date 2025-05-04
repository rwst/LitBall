package model

interface ProgressHandler {
    fun setProgressIndication(title: String = "", value: Float = -1f, text: String = ""): Boolean
    fun setInformationalDialog(text: String?)
}