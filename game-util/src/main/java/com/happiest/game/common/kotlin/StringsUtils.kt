package com.happiest.game.common.kotlin

fun String.startsWithAny(strings: Collection<String>) = strings.any { this.startsWith(it) }
