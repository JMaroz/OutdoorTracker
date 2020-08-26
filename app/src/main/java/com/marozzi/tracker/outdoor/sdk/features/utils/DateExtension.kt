package com.marozzi.tracker.outdoor.sdk.features.utils

import android.content.Context
import android.os.Build
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.abs

const val DATE_PATTERN_IT = "dd/MM/yyyy"
const val DATE_PATTERN_GG_MM_YYYY = "dd-MM-yyyy"
const val DATETIME_PATTERN_IT = "dd/MM/yyyy HH:mm:ss"
const val DATE_PATTERN_SERVER = "yyyy-MM-dd"
const val DATETIME_PATTERN_SERVER = "yyyy-MM-dd HH:mm:ss"
const val DATETIME_PATTERN_LOCAL = "yyyyMMdd'_'HHmmss"

const val DAY_IN_MILLIS = 1000 * 60 * 60 * 24.toLong()

private fun getLocale(): Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
    Locale.getDefault(Locale.Category.FORMAT)
} else {
    Locale.getDefault()
}

fun Date.formatDate(pattern: String): String {
    val sdf = SimpleDateFormat(pattern, getLocale())
    return sdf.format(this)
}

fun Date.formatDate(pattern: String, timeZone: String): String {
    val sdf = SimpleDateFormat(pattern, getLocale())
    sdf.timeZone = TimeZone.getTimeZone(timeZone)
    return sdf.format(this)
}

fun String.getDate(datePattern: String): Date? = try {
    SimpleDateFormat(datePattern, getLocale()).parse(this)
} catch (e: Exception) {
    null
}

fun String.getDate(datePattern: String, timeZone: String): Date? = try {
    SimpleDateFormat(datePattern, getLocale()).apply {
        this.timeZone = TimeZone.getTimeZone(timeZone)
    }.parse(this)
} catch (e: Exception) {
    null
}

/**
 * Pattern: yyyy-MM-dd HH:mm:ss
 */
fun Date.formatToServerDateTimeDefaults(): String {
    return formatDate(DATETIME_PATTERN_SERVER)
}

fun Date.formatToTruncatedDateTime(): String {
    return formatDate("yyyyMMddHHmmss")
}

/**
 * Pattern: yyyy-MM-dd
 */
fun Date.formatToServerDateDefaults(): String {
    return formatDate(DATE_PATTERN_SERVER)
}

/**
 * Pattern: HH:mm:ss
 */
fun Date.formatToServerTimeDefaults(): String {
    return formatDate("HH:mm:s")
}

/**
 * Pattern: dd/MM/yyyy HH:mm:ss
 */
fun Date.formatToViewDateTimeDefaults(): String {
    return formatDate(DATETIME_PATTERN_IT)
}

/**
 * Pattern: dd/MM/yyyy
 */
fun Date.formatToViewDateDefaults(): String {
    return formatDate(DATE_PATTERN_IT)
}

/**
 * Pattern: HH:mm:ss
 */
fun Date.formatToViewTimeDefaults(): String {
    return formatDate("HH:mm:ss")
}

/**
 * Add field date to current date
 */
fun Date.add(field: Int, amount: Int) {
    val cal = Calendar.getInstance()
    cal.timeInMillis = this.time
    cal.add(field, amount)

    this.time = cal.time.time
}

fun Date.addYears(years: Int) {
    add(Calendar.YEAR, years)
}

fun Date.addMonths(months: Int) {
    add(Calendar.MONTH, months)
}

fun Date.addDays(days: Int) {
    add(Calendar.DAY_OF_MONTH, days)
}

fun Date.addHours(hours: Int) {
    add(Calendar.HOUR_OF_DAY, hours)
}

fun Date.addMinutes(minutes: Int) {
    add(Calendar.MINUTE, minutes)
}

fun Date.addSeconds(seconds: Int) {
    add(Calendar.SECOND, seconds)
}

fun Date.addMilliseconds(milliseconds: Long) {
    this.time = time + milliseconds
}

/**
 * @return a new date with the time setted to the start of the date 00:00:00:
 */
fun Date.dateAtStartOfDay(): Date {
    val cal = Calendar.getInstance()
    cal.timeInMillis = this.time
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.time
}

/**
 * @return a new date with the time setted to the end of the date 23:59:59
 */
fun Date.dateAtEndOfDay(): Date {
    val cal = Calendar.getInstance()
    cal.timeInMillis = this.time
    cal.set(Calendar.HOUR_OF_DAY, 23)
    cal.set(Calendar.MINUTE, 59)
    cal.set(Calendar.SECOND, 59)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.time
}

fun Date.getPartitionDate(): Int {
    val simpleDateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return simpleDateFormat.format(this).toInt()
}

fun Int.getDateFromPartitionDate(): Date {
    val simpleDateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return simpleDateFormat.parse(toString())
}

fun Date.get(field: Int): Int {
    val cal = Calendar.getInstance()
    cal.timeInMillis = this.time
    return cal.get(field)
}

/**
 * Return the number of days from this to the dateStart in input
 */
fun Date.getNumberOfDaysFrom(dateStart: Date): Int {
    return TimeUnit.DAYS.convert(dateAtEndOfDay().time - dateStart.dateAtStartOfDay().time, TimeUnit.MILLISECONDS).toInt()
}

/**
 * Return the number of days from the dateEnd in input to this
 */
fun Date.getNumberOfDaysTo(dateEnd: Date): Int {
    return TimeUnit.DAYS.convert(dateEnd.dateAtEndOfDay().time - dateAtStartOfDay().time, TimeUnit.MILLISECONDS).toInt()
}

/**
 * Return partition date
 */
@Deprecated("Use getPartitionDate instead", ReplaceWith("getPartitionDate()"))
fun Date.toPartitionDate(): Int {
    val simpleDateFormat = SimpleDateFormat("yyyyMMdd")
    val s = simpleDateFormat.format(this)
    return Integer.parseInt(s)
}


fun Date.format(context: Context, pattern: String) = SimpleDateFormat(pattern, Locale.getDefault()).format(this)

fun Date.format(context: Context) = android.text.format.DateFormat.getDateFormat(context).format(this)

fun Date.mediumFormat(context: Context) = android.text.format.DateFormat.getMediumDateFormat(context).format(this)

fun Date.longFormat(context: Context) = android.text.format.DateFormat.getLongDateFormat(context).format(this)

fun Date.dateFormat(context: Context) = android.text.format.DateFormat.getDateFormat(context).format(this)

fun Date.timeFormat(context: Context) = android.text.format.DateFormat.getTimeFormat(context).format(this)

fun Calendar.resetTime(): Calendar {
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
    return this
}

fun Calendar.atStartOfDay(): Calendar {
    resetTime()
    return this
}

val Calendar.timeAtStartOfDay: Date
    get() = apply { resetTime() }.time

fun Calendar.atStartOfMonth(): Calendar {
    resetTime()
    set(Calendar.DAY_OF_MONTH, 1)
    return this
}

val Calendar.timeAtStartOfMonth: Date
    get() = apply {
        resetTime()
        set(Calendar.DAY_OF_MONTH, 1)
    }.time

fun Date.dateAtStartOfMonth() = Calendar.getInstance().apply { time = this@dateAtStartOfMonth }.timeAtStartOfMonth

fun Calendar.atStartOfYear(): Calendar {
    resetTime()
    set(Calendar.MONTH, 0)
    set(Calendar.DAY_OF_MONTH, 1)
    return this
}

val Calendar.timeAtStartOfYear: Date
    get() = apply {
        resetTime()
        set(Calendar.MONTH, 0)
        set(Calendar.DAY_OF_MONTH, 1)
    }.time

fun Date.dateAtStartOfYear(): Date = Calendar.getInstance().apply { time = this@dateAtStartOfYear }.timeAtStartOfYear

fun Date.findNearestDate(dates: Collection<Date>) = Collections.min(dates) { o1, o2 ->
    val diff1 = abs(o1.time - time)
    val diff2 = abs(o2.time - time)
    diff1.compareTo(diff2)
}

fun Date.findNearestDateSameYear(dates: Collection<Date>): Date = Collections.min(dates.filter { it.get(Calendar.YEAR) == get(Calendar.YEAR) }) { o1, o2 ->
    val diff1 = Math.abs(o1.time - time)
    val diff2 = Math.abs(o2.time - time)
    java.lang.Long.compare(diff1, diff2)
}

val Date.calendar: Calendar
    get() = Calendar.getInstance().also { it.time = this }


/**
 * @return true if this date is today else false
 */
fun Date.isToday(): Boolean {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = time
    val thenYear = calendar[Calendar.YEAR]
    val thenMonth = calendar[Calendar.MONTH]
    val thenMonthDay = calendar[Calendar.DAY_OF_MONTH]
    calendar.timeInMillis = System.currentTimeMillis()
    return (thenYear == calendar[Calendar.YEAR]
            && thenMonth == calendar[Calendar.MONTH]
            && thenMonthDay == calendar[Calendar.DAY_OF_MONTH])
}

/**
 * @return true if this date is tomorrow else false
 */
fun Date.isTomorrow(): Boolean {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = time
    val thenYear = calendar[Calendar.YEAR]
    val thenMonth = calendar[Calendar.MONTH]
    val thenMonthDay = calendar[Calendar.DAY_OF_MONTH]
    calendar.timeInMillis = System.currentTimeMillis()
    return (thenYear == calendar[Calendar.YEAR]
            && thenMonth == calendar[Calendar.MONTH]
            && thenMonthDay == calendar[Calendar.DAY_OF_MONTH] + 1)
}

/**
 * @return true if this date is yesterday else false
 */
fun Date.isYesterday(): Boolean {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = time
    val thenYear = calendar[Calendar.YEAR]
    val thenMonth = calendar[Calendar.MONTH]
    val thenMonthDay = calendar[Calendar.DAY_OF_MONTH]
    calendar.timeInMillis = System.currentTimeMillis()
    return (thenYear == calendar[Calendar.YEAR]
            && thenMonth == calendar[Calendar.MONTH]
            && thenMonthDay == calendar[Calendar.DAY_OF_MONTH] - 1)
}