package com.happiest.game.app.utils.image

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.IntBuffer
import javax.microedition.khronos.opengles.GL10


class ImageUtils {

    companion object {

        private fun genFileFromBitmap(bitmap: Bitmap, c: Context): File {
            val cw = ContextWrapper(c)
            val shareImgDir = cw.cacheDir
            val shareImgFile = File(shareImgDir, "share.jpg")
            Timber.d("saveBitmap: " + shareImgFile.absolutePath)

            val bos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bos)
            val bitmapData = bos.toByteArray()
            val fis = ByteArrayInputStream(bitmapData)

            val fos = FileOutputStream(shareImgFile)
            val buf = ByteArray(1024)
            var len: Int
            while (fis.read(buf).also { len = it } > 0) {
                fos.write(buf, 0, len)
            }
            fos.flush()
            fos.close()
            fis.close()
            return shareImgFile
        }

        fun genFileFromView(context: Context, v: View): File {
            val width = v.width
            val height = v.height
            val createBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val c = Canvas(createBitmap)

            c.drawColor(Color.WHITE)
            v.draw(c)
            return genFileFromBitmap(createBitmap, context)
        }

        fun createBitmapFromGLSurface(x: Int, y: Int, w: Int, h: Int, gl: GL10): Bitmap {
            val bitmapBuffer = IntArray(w * h)
            val bitmapSource = IntArray(w * h)
            val intBuffer: IntBuffer = IntBuffer.wrap(bitmapBuffer)
            intBuffer.position(0)
            gl.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer)
            var offset1: Int
            var offset2: Int
            for (i in 0 until h) {
                offset1 = i * w
                offset2 = (h - i - 1) * w
                for (j in 0 until w) {
                    val texturePixel = bitmapBuffer[offset1 + j]
                    val blue = texturePixel shr 16 and 0xff
                    val red = texturePixel shl 16 and 0x00ff0000
                    val pixel = texturePixel and -0xff0100 or red or blue
                    bitmapSource[offset2 + j] = pixel
                }
            }
            return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888)
        }
    }
}
