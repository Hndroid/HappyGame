package com.happy.game.core

import android.os.ParcelFileDescriptor

data class VirtualFile(val virtualPath: String, val fileDescriptor: ParcelFileDescriptor)
