package com.marozzi.tracker.outdoor.sdk.features.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.fragment.app.FragmentActivity
import com.marozzi.tracker.outdoor.sdk.data.local.model.GpsActivityPoint
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Created by amarozzi on 2020-07-19
 */
object GpsTrackerUtils {

    fun haveToAdd(point0: GpsActivityPoint, point1: GpsActivityPoint, point2: GpsActivityPoint): Boolean {

        val delta = 0.00001 // 10 cm delta

        val hd01 = haversineDistance(point0, point1)
        val hd12 = haversineDistance(point1, point2)
        val hd02 = haversineDistance(point0, point2)

        return hd01 + hd12 > hd02 + delta
    }

    private fun haversineDistance(origin: GpsActivityPoint, destination: GpsActivityPoint): Double {
        //http://en.wikipedia.org/wiki/Haversine_formula
        //Wayne Dyck's implementation

        val lat1 = origin.latitude
        val lon1 = origin.longitude

        val lat2 = destination.latitude
        val lon2 = destination.longitude

        val radius = 6371.0 //earth radius in km

        val dlat = Math.toRadians(lat2 - lat1)
        val dlon = Math.toRadians(lon2 - lon1)

        val a = sin(dlat / 2) * sin(dlat / 2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dlon / 2) * sin(dlon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return radius * c
    }

    /**
     * @return true if ignoring, false otherwise
     */
    fun isIgnoringBatteryOptimizations(context: Context, packageName: String = context.packageName): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (context.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(packageName)
        } else {
            true
        }
    }

    fun requestIgnoringBatteryOptimizations(activity: FragmentActivity, packageName: String = activity.packageName, requestCode: Int) : Boolean {
        val intent = Intent().setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:$packageName"))
        return if (intent.resolveActivity(activity.packageManager) != null) {
            activity.startActivityForResult(intent, requestCode)
            true
        } else {
            false
        }
    }
}
