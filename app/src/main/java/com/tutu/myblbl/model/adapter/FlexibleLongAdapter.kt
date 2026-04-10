package com.tutu.myblbl.model.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter

class FlexibleLongAdapter : TypeAdapter<Long>() {

    override fun write(out: JsonWriter, value: Long?) {
        out.value(value ?: 0L)
    }

    override fun read(reader: JsonReader): Long {
        return when (reader.peek()) {
            JsonToken.NUMBER -> reader.nextLong()
            JsonToken.STRING -> {
                val str = reader.nextString().trim()
                str.toLongOrNull() ?: 0L
            }
            JsonToken.NULL -> {
                reader.nextNull()
                0L
            }
            else -> {
                reader.skipValue()
                0L
            }
        }
    }
}
