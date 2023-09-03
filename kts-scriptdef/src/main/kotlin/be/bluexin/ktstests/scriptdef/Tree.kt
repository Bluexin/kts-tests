package be.bluexin.ktstests.scriptdef

interface Tree {
    fun grow()
    fun harvest()
    fun onSizeChange(callback: OnSizeChange)
}

typealias OnSizeChange = (from: Int, to: Int) -> Unit
