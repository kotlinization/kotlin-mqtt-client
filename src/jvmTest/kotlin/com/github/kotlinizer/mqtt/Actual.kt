package com.github.kotlinizer.mqtt

import java.lang.System.currentTimeMillis

actual val milliseconds: Long
    get() = currentTimeMillis()