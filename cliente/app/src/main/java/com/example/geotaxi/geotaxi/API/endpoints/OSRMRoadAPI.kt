package com.example.geotaxi.geotaxi.API.endpoints

import android.content.Context
import android.os.AsyncTask
import com.example.geotaxi.geotaxi.API.OSRMAPI
import com.example.geotaxi.geotaxi.API.OsrmAPI
import org.osmdroid.util.GeoPoint
import retrofit2.Response

/**
 * Created by dieropal on 17/01/18.
 */
class OSRMRoadAPI {
    private val osrmretrofit = OSRMAPI.retrofit
    private val osrmAPI = osrmretrofit.create(OsrmAPI::class.java)

    fun getOsrmRoutes(start: GeoPoint, end: GeoPoint): Response<String>? {
        val startLong = start.longitude
        val startLat = start.latitude
        val endLong = end.longitude
        val endLat = end.latitude
        val roadTask = RoadTask(startLong.toString(), startLat.toString(),
                endLong.toString(), endLat.toString())
        return roadTask.execute().get()
    }

    inner class RoadTask(val startLong: String, val startLat: String,
                         val endLong: String, val endLat: String) : AsyncTask<Context, Void, Response<String>?>() {

        override fun doInBackground(vararg params: Context?): Response<String>? {
            val serverCall = osrmAPI?.getOsrmRoutes(startLong, startLat, endLong, endLat)
            if (serverCall != null ) {
                return serverCall.execute()
            }
            return null
        }
    }
}