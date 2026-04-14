package com.tutu.myblbl.core.model.id

@JvmInline
value class Aid(val value: Long) {
    fun isValid(): Boolean = value > 0L
    fun orNull(): Long? = value.takeIf { it > 0L }
}

@JvmInline
value class Cid(val value: Long) {
    fun isValid(): Boolean = value > 0L
    fun orNull(): Long? = value.takeIf { it > 0L }
}

@JvmInline
value class EpId(val value: Long) {
    fun isValid(): Boolean = value > 0L
    fun orNull(): Long? = value.takeIf { it > 0L }
}

@JvmInline
value class Mid(val value: Long) {
    fun isValid(): Boolean = value > 0L
    fun orNull(): Long? = value.takeIf { it > 0L }
}

@JvmInline
value class Bvid(val value: String) {
    fun isValid(): Boolean = value.isNotBlank()
    fun orNull(): String? = value.takeIf { it.isNotBlank() }
}
