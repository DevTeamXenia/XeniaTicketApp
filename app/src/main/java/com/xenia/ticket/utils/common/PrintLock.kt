package com.xenia.ticket.utils.common

import java.util.concurrent.atomic.AtomicBoolean

object PrintLock {
    val isPrinting = AtomicBoolean(false)
}
