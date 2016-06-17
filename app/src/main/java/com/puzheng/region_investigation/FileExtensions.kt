package com.puzheng.region_investigation

import android.os.Environment
import java.io.File

/**
 * Created by xc on 16-6-13.
 */
val File.humanizePath: String
        get() = File("存储卡", this.relativeTo(Environment.getExternalStoragePublicDirectory("")).path).path

