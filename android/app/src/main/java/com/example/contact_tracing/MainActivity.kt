package com.example.contact_tracing

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import org.altbeacon.beacon.*
import org.altbeacon.beacon.powersave.BackgroundPowerSaver
import android.Manifest
import android.app.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.graphics.Color
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout

const val APP_UUID: String = "31049370-03e0-459f-b832-da0666df02f8"

class MainActivity : AppCompatActivity(), BeaconConsumer {
    var beaconManager: BeaconManager? = null
    var backgroundPowerSaver: BackgroundPowerSaver? = null
    var transmitter: BeaconTransmitter? = null

    private fun startTransmitter() {
        Log.i("bluetooth", "starting transmitter")
        var beaconBuilder = Beacon.Builder()
        beaconBuilder.setId1(APP_UUID)
        beaconBuilder.setId2("0")
        beaconBuilder.setId3("0")
        beaconBuilder.setManufacturer(0x004c)
        beaconBuilder.setTxPower(-59)
        beaconBuilder.setDataFields(listOf(0L))
        var beacon = beaconBuilder.build()
        var parser = BeaconParser()
        parser.setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        transmitter = BeaconTransmitter(applicationContext, parser)
        transmitter?.startAdvertising(beacon, object : AdvertiseCallback() {
            override fun onStartFailure(errorCode: Int) {
                Log.w("bluetooth", "transmitter failed with error code $errorCode")
            }

            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.i("bluetooth", "transmitter started")
            }
        })
    }

    private fun createNotificationChannel(channelId: String, channelName: String): String {
        val chan = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_NONE)
        chan.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(chan)
        return channelId
    }

    private fun startInitFlow() {
        // prompt user for location, this is necessary in order for permissions to work
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 1)
            } else {
                initBeacons()
            }
        } else {
            initBeacons()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // setup toggle button
        var btnToggle = findViewById<Switch>(R.id.btnToggle)
        var lblTrackingTitle = findViewById<TextView>(R.id.lblTrackingTitle)
        var lblTrackingSubtitle = findViewById<TextView>(R.id.lblTrackingSub)
        var layoutTracking = findViewById<ConstraintLayout>(R.id.layoutTracking)
        btnToggle.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                beaconManager?.removeAllMonitorNotifiers()
                transmitter?.stopAdvertising()
                Toast.makeText(this, "Service disabled!", Toast.LENGTH_SHORT).show()
            } else {
                if (beaconManager == null) {
                    startInitFlow()
                }
                else {
                    onBeaconServiceConnect()
                    Toast.makeText(this, "Service enabled!", Toast.LENGTH_SHORT).show()
                }
            }

            if (isChecked) {
                lblTrackingTitle.text = "Tracking: ON"
                lblTrackingSubtitle.text = "Back home? Turn me off to save battery."
                layoutTracking.setBackgroundColor(Color.parseColor("#F3755E"))

            }
            else {
                lblTrackingTitle.text = "Tracking: OFF"
                lblTrackingSubtitle.text = "Going outside? Turn me on!"
                layoutTracking.setBackgroundColor(Color.parseColor("#E5E5E5"))
            }
        }

        btnToggle.isChecked = true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initBeacons()
            }
        }
    }

    private fun initBeacons() {
        // init beacon manager
        beaconManager = BeaconManager.getInstanceForApplication(this)

        // foreground service requires channel in newer android
        var channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel("contact-tracing", "Stop the Spread")
        } else {
            ""
        }

        // setup beacon manager to run as a foreground service
        var builder = Notification.Builder(this.applicationContext, channelId)
        beaconManager?.enableForegroundServiceScanning(builder.build(), 456)
        beaconManager?.setEnableScheduledScanJobs(false)
        beaconManager?.isRegionStatePersistenceEnabled = false

        // enable ibeacons
        var parser = BeaconParser()
        parser.setHardwareAssistManufacturerCodes(intArrayOf(0x004c))
        parser.setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        beaconManager?.beaconParsers?.add(parser)
        beaconManager?.bind(this)

        backgroundPowerSaver = BackgroundPowerSaver(this)

        // check transmitter service status
        var transmitterResult = BeaconTransmitter.checkTransmissionSupported(this)
        if (transmitterResult != BeaconTransmitter.SUPPORTED) {
            Log.w("bluetooth", "does not support transmission $transmitterResult")
        }
    }

    override fun onBeaconServiceConnect() {
        beaconManager?.removeAllMonitorNotifiers()
        beaconManager?.addRangeNotifier { beacons, _ ->
            Log.i(
                "bluetooth",
                "ranged $beacons beacons"
            )
        }
        beaconManager?.startRangingBeaconsInRegion(Region("contact-tracing", null, null, null))
        Log.i("bluetooth", "listener started")

        startTransmitter()
    }
}