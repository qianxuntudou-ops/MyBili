package com.tutu.myblbl.network

import java.net.URLEncoder
import java.security.MessageDigest
import java.util.TreeMap

object WbiGenerator {
    
    private val mixinKeyEncTab = intArrayOf(
        46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
        27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
        37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
        22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52
    )
    
    fun generateWbiParams(
        params: Map<String, String>,
        imgKey: String,
        subKey: String
    ): Map<String, String> {
        if (imgKey.isBlank() || subKey.isBlank()) {
            return params
        }

        val mixinKey = getMixinKey(imgKey + subKey)
        val wts = System.currentTimeMillis() / 1000

        val cleanParams = params.filterKeys { it.isNotBlank() }
        val sortedParams = TreeMap(cleanParams)
        sortedParams["wts"] = wts.toString()

        val filteredParams = mutableMapOf<String, String>()
        sortedParams.forEach { (key, value) ->
            if (key.isNotBlank()) {
                val filteredValue = filterSpecialChars(value)
                filteredParams[key] = filteredValue
            }
        }

        val query = filteredParams.entries
            .sortedBy { it.key }
            .joinToString("&") { (key, value) ->
                "${URLEncoder.encode(key, "UTF-8").replace("+", "%20")}=${URLEncoder.encode(value, "UTF-8").replace("+", "%20")}"
            }

        return filteredParams + ("w_rid" to md5(query + mixinKey))
    }
    
    private fun getMixinKey(originKey: String): String {
        val sb = StringBuilder()
        for (i in mixinKeyEncTab.indices) {
            val index = mixinKeyEncTab[i]
            if (index < originKey.length) {
                sb.append(originKey[index])
            }
        }
        return sb.toString().take(32)
    }
    
    private fun filterSpecialChars(str: String): String {
        val specialChars = setOf('!', '\'', '(', ')', '*')
        return str.filter { char -> char !in specialChars }
    }
    
    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
    
    fun extractKeyFromUrl(url: String): String {
        val wbiIndex = url.indexOf("wbi/")
        if (wbiIndex == -1) return ""
        val startIndex = wbiIndex + 4
        val endIndex = url.indexOf(".", startIndex)
        return if (endIndex > startIndex) {
            url.substring(startIndex, endIndex)
        } else {
            ""
        }
    }
}
