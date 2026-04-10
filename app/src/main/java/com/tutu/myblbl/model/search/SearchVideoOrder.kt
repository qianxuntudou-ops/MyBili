package com.tutu.myblbl.model.search

enum class SearchVideoOrder(
    val orderValue: String,
    val showName: String
) {
    TotalRank("totalrank", "综合排序"),
    Click("click", "最多点击"),
    PubDate("pubdate", "最新发布"),
    Dm("dm", "最多弹幕"),
    MostCollection("stow", "最多收藏"),
    MostComment("scores", "最多评论")
}
