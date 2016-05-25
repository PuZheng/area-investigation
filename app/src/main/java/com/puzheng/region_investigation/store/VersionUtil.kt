package com.puzheng.region_investigation.store

class VersionUtil {
    companion object {
        fun compare(version1: String, version2: String): Int {
            val vals1 = version1.split(".")
            val vals2 = version2.split(".")
            var i = 0
            // set index to first non-equal ordinal or length of shortest version string
            while (i < vals1.size && i < vals2.size && vals1[i] == vals2[i]) {
                i++
            }
            // compare first non-equal ordinal number
            return if (i < vals1.size && i < vals2.size) {
                val diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]))
                Integer.signum(diff)
            } else {
                // the strings are equal or one string is a substring of the other
                // e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
                Integer.signum(vals1.size - vals2.size)
            }
        }
    }
}
