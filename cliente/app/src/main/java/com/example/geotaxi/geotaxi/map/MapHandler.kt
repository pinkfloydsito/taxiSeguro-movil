package com.example.geotaxi.geotaxi.map

import android.graphics.drawable.Drawable
import android.support.v4.content.res.ResourcesCompat
import android.view.View
import com.example.geotaxi.geotaxi.R
import com.example.geotaxi.geotaxi.data.Route
import com.example.geotaxi.geotaxi.data.User
import com.example.geotaxi.geotaxi.ui.MainActivity
import org.osmdroid.api.IMapController
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.infowindow.InfoWindow

/**
 * Created by dieropal on 17/01/18.
 */
class MapHandler {
    private var activity: MainActivity? = null
    private var map : MapView? = null
    private var driverMarker: Marker? = null
    private var userMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var mapController: IMapController? = null
    private var roadOverlays = mutableListOf<Polyline>()
    private var roadChosen : Road? = null
    private var roadChosenIndex: Int = 0
    private var ROAD_COLORS: HashMap<String, Int> = hashMapOf()

    constructor(activity: MainActivity,
                mapView: MapView?,
                driverIcon: Drawable?,
                userIcon: Drawable?,
                destinationIcon: Drawable?
    ) {
        val mRotationGestureOverlay =  RotationGestureOverlay(mapView)
        val userMarker = Marker(mapView)
        val driverMarker = Marker(mapView)
        val destinationMarker = Marker(mapView)
        val mapController = mapView?.controller

        mRotationGestureOverlay.isEnabled = true
        mapView?.setTileSource(TileSourceFactory.MAPNIK)
        mapView?.setMultiTouchControls(true)
        mapView?.overlays?.add(mRotationGestureOverlay)
        mapController?.setCenter(User.instance.position)
        mapController?.setZoom(17)
        userMarker?.setIcon(userIcon)
        driverMarker?.setIcon(driverIcon)
        destinationMarker?.setIcon(destinationIcon)
        this.activity = activity
        this.map = mapView
        this.mapController = mapController
        this.driverMarker = driverMarker
        this.userMarker = userMarker
        this.destinationMarker = destinationMarker
        this.ROAD_COLORS = hashMapOf(
                "chosen" to ResourcesCompat.getColor(activity.resources, R.color.chosenRoute, null),
                "alternative" to ResourcesCompat.getColor(activity.resources, R.color.alternativeRoute, null)
                )
    }

    fun updateUserIconOnMap(location: GeoPoint) {
        map?.overlays?.remove(userMarker)
        userMarker?.position = location
        userMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map?.overlays?.add(userMarker)
        map?.invalidate()
    }

    fun updateDriverIconOnMap(location: GeoPoint) {
        map?.overlays?.remove(driverMarker)
        driverMarker?.position = location
        driverMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map?.overlays?.add(driverMarker)
        map?.invalidate()
    }

    fun animateToLocation(location: GeoPoint?, zoomLevel: Int) {
        if (location != null) {
            mapController?.animateTo(location)
            mapController?.zoomTo(17)
        }
    }
    fun drawRoad(road: Road, userPos: GeoPoint, destinationPos: GeoPoint) {
        if (!road.mNodes.isEmpty()) {
            val roadOverlay = RoadManager.buildRoadOverlay(road)
            roadOverlay.color = ROAD_COLORS["chosen"]!!
            val midIndex = if (road.mNodes.size%2 == 0) {
                (road.mNodes.size/2) - 1
            } else {
                ((road.mNodes.size + 1)/2) - 1
            }
            val infoPos = road.mNodes[midIndex].mLocation
            val duration = ("%.2f".format(road.mDuration/60)) + " min"
            val distance = ("%.2f".format(road.mLength)) + " km"

            roadOverlay.infoWindow = MyInfoWindow(R.layout.info_window, map!!,
                    title = duration, description = distance)
            roadOverlay.infoWindow.view.setOnLongClickListener { v: View ->
                roadOverlay.infoWindow.close()
                true
            }
            roadOverlay.setOnClickListener{ polyline, mapView, eventPos ->
                roadOverlay.showInfoWindow(eventPos)
                true
            }
            updateUserIconOnMap(userPos)
            addDestMarker(destinationPos)
            map?.overlays?.add(roadOverlay)
            roadOverlay.showInfoWindow(infoPos)
            map?.zoomToBoundingBox(road.mBoundingBox, true)
            map?.invalidate()
        }
    }

    fun drawRoads(roads: kotlin.Array<out Road>) {
        var roadIndex = 0
        var roadColor = ROAD_COLORS["chosen"]
        if (roadOverlays.isNotEmpty()) {
            roadOverlays.clear()
        }
        roads.forEach { road ->

            if (road.mNodes.isNotEmpty()) {

                val roadOverlay = RoadManager.buildRoadOverlay(road)
                roadOverlay.width = 6F

                val midIndex = if (road.mNodes.size%2 == 0) {
                    (road.mNodes.size/2) - 1
                } else {
                    ((road.mNodes.size + 1)/2) - 1
                }
                val infoPos = road.mNodes[midIndex].mLocation
                val duration = ("%.2f".format(road.mDuration/60)) + " min"
                val distance = ("%.2f".format(road.mLength)) + " km"

                val mInfoWin = MyInfoWindow(R.layout.info_window, map!!,
                        title = duration, description = distance)

                if (roadIndex != Route.instance.currentRoadIndex) {
                    roadColor = ROAD_COLORS["alternative"]
                    mInfoWin.setTittle("Alterna")
                    mInfoWin.showTittle()
                    map?.overlays?.add(roadOverlay)
                } else {
                    roadColor = ROAD_COLORS["chosen"]
                    mInfoWin.hideTittle()
                }
                roadOverlay.infoWindow = mInfoWin
                roadOverlay.infoWindow.view.setOnClickListener{ v: View  ->
                    onRoadChosen(roadOverlay, road)
                }
                roadOverlay.infoWindow.view.setOnLongClickListener { v: View ->
                    roadOverlay.infoWindow.close()
                    true
                }
                roadOverlay.setOnClickListener{ polyline, mapView, eventPos ->
                    onRoadChosen(roadOverlay, road)
                    roadOverlay.showInfoWindow(eventPos)
                    true
                }
                roadOverlays.add(roadOverlay)

                roadOverlay.color = roadColor!!
                roadOverlay.showInfoWindow(infoPos)//open(roadOverlay, infoPos, 2,2)
                map?.zoomToBoundingBox(road.mBoundingBox, true)
                map?.invalidate()
            }
            roadIndex += 1
        }
        map?.overlays?.add(roadOverlays[roadChosenIndex])
    }

    fun drawDriverRequestRoad(road: Road) {
        if (!road.mNodes.isEmpty()) {
            val roadOverlay = RoadManager.buildRoadOverlay(road)
            val midIndex = if (road.mNodes.size%2 == 0) {
                (road.mNodes.size/2) - 1
            } else {
                ((road.mNodes.size + 1)/2) - 1
            }
            val infoPos = road.mNodes[midIndex].mLocation
            val duration = ("%.2f".format(road.mDuration/60)) + " min"
            val distance = ("%.2f".format(road.mLength)) + " km"

            roadOverlay.infoWindow = MyInfoWindow(R.layout.info_window, map!!,
                    title = duration, description = distance)
            roadOverlay.color = ROAD_COLORS["alternative"]!!
            map?.overlays?.add(roadOverlay)
            roadOverlay.showInfoWindow(infoPos)
            map?.zoomToBoundingBox(road.mBoundingBox, true)
            map?.invalidate()
        }
    }

    private fun onRoadChosen(roadOverlay: Polyline, road: Road) {
        var indx = 0
        activity?.choose_route?.isEnabled = true
        roadOverlays.forEach {
            if (it != roadOverlay) {
                it.color = ROAD_COLORS["alternative"]!!
            } else {
                roadChosenIndex = indx
            }
            indx+= 1
        }
        map!!.overlays.remove(roadOverlay)
        roadOverlay.color = ROAD_COLORS["chosen"]!!
        map!!.overlays.add(roadOverlay)
        roadChosen = road
    }

    fun getRoadChosen(): Road? {
        return roadChosen
    }

    fun getRoadIndexChosen(): Int {
        return roadChosenIndex
    }
    fun addDestMarker(destPos: GeoPoint) {
        destinationMarker?.position = destPos
        destinationMarker?.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        //infoMarker.snippet = ("%.2f".format(road.mDuration/60)) + " min"
        //infoMarker.subDescription = ("%.2f".format(road.mLength)) + " km"
        //infoMarker.title = "."
        //infoMarker.setIcon(null)
        //infoMarker.infoWindow = infoWindow
        map?.overlays?.add(destinationMarker)
        //infoMarker.infoWindow.open(infoMarker, infoPos, 2,2)
    }

    fun clearMapOverlays() {
        map?.overlays?.clear()
        InfoWindow.closeAllInfoWindowsOn(map)
    }

    fun closeDestinationWindowInfo() {
        if (destinationMarker?.isInfoWindowShown!!)
            destinationMarker?.closeInfoWindow()
    }
}