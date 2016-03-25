package com.puzheng.area_investigation.store

import java.io.InputStream
import java.io.OutputStream

fun InputStream.transferTo(outputStream: OutputStream) {
    var len = 1024
    val buf = ByteArray(len)
    while (true) {
        len = read(buf)
        if (len <= 0) {
            break
        }
        outputStream.write(buf, 0, len)
    }
}