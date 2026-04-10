package com.tutu.myblbl.model.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

class FlexibleBooleanAdapter : TypeAdapter<Boolean>() {

    override fun write(out: JsonWriter, value: Boolean?) {
        out.value(value ?: false)
    }

    override fun read(reader: JsonReader): Boolean {
        return when (reader.peek()) {
            JsonToken.BOOLEAN -> reader.nextBoolean()
            JsonToken.NUMBER -> reader.nextInt() != 0
            JsonToken.STRING -> {
                when (reader.nextString().trim().lowercase()) {
                    "1", "true", "yes", "y" -> true
                    else -> false
                }
            }
            JsonToken.NULL -> {
                reader.nextNull()
                false
            }
            else -> {
                reader.skipValue()
                false
            }
        }
    }
}
