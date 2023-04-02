package com.happiest.game.app.utils

import com.happiest.game.R
import com.happiest.game.app.HappyGame
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateUtil {

    private const val MINUTE = (60 * 1000).toLong()

    private const val HOUR = 60 * MINUTE

    private const val DAY = 24 * HOUR

    private const val WEEK = 7 * DAY

    private const val MONTH = 4 * WEEK

    private const val YEAR = 365 * DAY

    /**
     * 根据传入的Unix时间戳，获取转换过后更加易读的时间格式。
     * @param dateMillis
     * Unix时间戳
     * @return 转换过后的时间格式，如2分钟前，1小时前。
     */
    fun getConvertedDate(dateMillis: Long): String {
        val currentMillis = System.currentTimeMillis()
        val timePast = currentMillis - dateMillis
        if (timePast > -MINUTE) { // 采用误差一分钟以内的算法，防止客户端和服务器时间不同步导致的显示问题
            when {
                timePast < HOUR -> {
                    var pastMinutes = timePast / MINUTE
                    if (pastMinutes <= 0) {
                        pastMinutes = 1
                    }
                    return pastMinutes.toString() + HappyGame.getString(R.string.minutes_ago)
                }
                timePast < DAY -> {
                    var pastHours = timePast / HOUR
                    if (pastHours <= 0) {
                        pastHours = 1
                    }
                    return pastHours.toString() + HappyGame.getString(R.string.hours_ago)
                }
                timePast < WEEK -> {
                    var pastDays = timePast / DAY
                    if (pastDays <= 0) {
                        pastDays = 1
                    }
                    return pastDays.toString() + HappyGame.getString(R.string.days_ago)
                }
                timePast < MONTH -> {
                    var pastDays = timePast / WEEK
                    if (pastDays <= 0) {
                        pastDays = 1
                    }
                    return pastDays.toString() + HappyGame.getString(R.string.weeks_ago)
                }
                else -> return getDate(dateMillis)
            }
        } else {
            return getDateAndTime(dateMillis)
        }
    }

    fun getTimeLeftTip(timeLeft: Long) = when {
        timeLeft > YEAR -> {
            val year = (timeLeft / YEAR) + 1
            year.toString() + HappyGame.getString(R.string.year)
        }
        timeLeft > MONTH -> {
            val month = (timeLeft / MONTH) + 1
            month.toString() + HappyGame.getString(R.string.month)
        }
        timeLeft > DAY -> {
            val day = (timeLeft / DAY) + 1
            day.toString() + HappyGame.getString(R.string.day)
        }
        timeLeft > HOUR -> {
            val hour = (timeLeft / HOUR) + 1
            hour.toString() + HappyGame.getString(R.string.hour)
        }
        timeLeft > MINUTE -> {
            val minute = (timeLeft / MINUTE) + 1
            minute.toString() + HappyGame.getString(R.string.minute)
        }
        else -> {
            "1" + HappyGame.getString(R.string.minute)
        }
    }

    fun isBlockedForever(timeLeft: Long) = timeLeft > 5 * YEAR

    private fun getDate(dateMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(dateMillis))
    }

    private fun getDateAndTime(dateMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(dateMillis))
    }

}
