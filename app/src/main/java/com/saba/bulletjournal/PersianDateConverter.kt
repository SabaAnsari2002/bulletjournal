package com.saba.bulletjournal

import java.util.Calendar

class PersianDateConverter {

    private val g_days_in_month = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    private val j_days_in_month = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

    private fun gregorianToJalali(gregorianDate: Calendar): Array<Int> {
        val gy = gregorianDate.get(Calendar.YEAR) - 1600
        val gm = gregorianDate.get(Calendar.MONTH) + 1
        val gd = gregorianDate.get(Calendar.DAY_OF_MONTH) - 1

        var j_day_no: Int = 365 * gy + (gy + 3) / 4 - (gy + 99) / 100 + (gy + 399) / 400
        for (i in 0 until gm - 1) {
            j_day_no += g_days_in_month[i]
        }
        if (gm > 2 && (gy + 1600) % 4 == 0 && ((gy + 1600) % 100 != 0 || (gy + 1600) % 400 == 0)) {
            j_day_no++
        }
        j_day_no += gd

        j_day_no -= 79

        var j_np = j_day_no / 12053
        j_day_no %= 12053

        var jy = 979 + 33 * j_np + 4 * (j_day_no / 1461)
        j_day_no %= 1461

        if (j_day_no >= 366) {
            jy += (j_day_no - 1) / 365
            j_day_no = (j_day_no - 1) % 365
        }

        var jm: Int = 0
        var jd: Int = 0
        for (i in 0..11) {
            if (j_day_no < j_days_in_month[i]) {
                jm = i + 1
                jd = j_day_no + 1
                break
            }
            j_day_no -= j_days_in_month[i]
        }

        return arrayOf(jy, jm, jd)
    }

    fun getJalaliDate(gregorianDate: Calendar): String {
        val jalaliDate = gregorianToJalali(gregorianDate)
        val year = jalaliDate[0]
        val month = jalaliDate[1]
        val day = jalaliDate[2]
        return "$year-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"
    }
}
