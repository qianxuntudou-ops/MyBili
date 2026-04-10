package com.tutu.myblbl.model.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

class FlexibleIntAdapter : TypeAdapter<Int>() {

    override fun write(out: JsonWriter, value: Int?) {
        out.value(value ?: 0)
    }

    override fun read(reader: JsonReader): Int {
        return when (reader.peek()) {
            JsonToken.NUMBER -> reader.nextInt()
            JsonToken.STRING -> {
                val str = reader.nextString().trim()
                str.toIntOrNull() ?: 0
            }
            JsonToken.NULL -> {
                reader.nextNull()
                0
            }
            else -> {
                reader.skipValue()
                0
            }
        }
    }
}
