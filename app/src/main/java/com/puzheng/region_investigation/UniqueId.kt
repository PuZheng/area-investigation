package com.puzheng.region_investigation

import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by xc on 16-5-6.
 */

private val atomicInteger = AtomicInteger()

fun uniqueId() = atomicInteger.andIncrement