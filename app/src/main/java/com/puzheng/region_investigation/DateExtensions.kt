package com.puzheng.region_investigation

import java.util.*


fun Date.ofSameDay(other: Date): Boolean {
    val cal1 = Calendar.getInstance();
    val cal2 = Calendar.getInstance();
    cal1.time = this;
    cal2.time = other;
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
}