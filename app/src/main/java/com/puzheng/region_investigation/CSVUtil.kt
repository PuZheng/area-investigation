package com.puzheng.region_investigation

/**
 * Created by xc on 16-7-15.
 */
class CSVUtil {
    companion object {
        const val fieldSep = ","
        const val lineSep = "\n"
        const val textSep = "\""
        fun quote(s: String) = textSep + s.replace(textSep, textSep + textSep) + textSep
    }
}