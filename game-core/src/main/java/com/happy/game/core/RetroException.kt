package com.happy.game.core

import java.lang.RuntimeException

class RetroException(val errorCode: Int) : RuntimeException("Retro exception. Error code $errorCode")
