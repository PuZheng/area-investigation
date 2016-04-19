package com.puzheng.region_investigation

import java.io.File
import java.io.FileOutputStream
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

fun InputStream.copyTo(file: File) {
    val outputStream = FileOutputStream(file)
    transferTo(outputStream)
    outputStream.close()
    close()
}