package com.tutu.myblbl.model.series

object SeriesType {
    const val ANIME = 1
    const val MOVIE = 2
    const val DOCUMENTARY = 3
    const val CHINA_ANIME = 4
    const val DRAMA = 5
    const val VARIETY = 7

    fun titleOf(type: Int): String {
        return when (type) {
            ANIME -> "番剧"
            CHINA_ANIME -> "国创"
            MOVIE -> "电影"
            VARIETY -> "综艺"
            DRAMA -> "电视剧"
            DOCUMENTARY -> "纪录片"
            else -> "番剧"
        }
    }
}
