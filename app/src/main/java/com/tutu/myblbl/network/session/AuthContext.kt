package com.tutu.myblbl.network.session

enum class AuthContext(val label: String) {
    USER_ACTION("user_action"),
    FOREGROUND("foreground"),
    BACKGROUND("background");

    val shouldClearSession: Boolean get() = this != BACKGROUND
}
