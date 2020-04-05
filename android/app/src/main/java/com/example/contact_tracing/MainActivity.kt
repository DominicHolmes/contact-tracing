package com.example.contact_tracing

import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import java.nio.ByteBuffer
import java.util.*

class BeaconReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        var callbackType = intent?.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1)
        if (callbackType != -1) {
            Log.i("bluetooth", "background scan callback: $callbackType")
        }
    }
}

const val APPLE_MANUFACTURER_ID : Int = 76

class MainActivity : AppCompatActivity() {
    var adapter : BluetoothAdapter? = null
    var scanIntent : PendingIntent? = null
    var advertiseCallback : AdvertiseCallback? = null

    private fun getEncodedUUID() : ByteBuffer {
        var uuid = UUID.fromString("9eb121f4-1bc2-4da6-862f-60dcedabcf38");
        var encodedUUID = ByteBuffer.allocate(16)
        encodedUUID.putLong(uuid.mostSignificantBits)
        encodedUUID.putLong(uuid.leastSignificantBits)
        return encodedUUID
    }

    private fun getScanFilter() : List<ScanFilter> {
        var builder = ScanFilter.Builder()
        var manufacturerData = ByteBuffer.allocate(23)
        var manufacturerDataMask = ByteBuffer.allocate(24)
        manufacturerData.put(0,(0x02).toByte())
        manufacturerData.put(1, (0x15).toByte())
        var encodedUUID = getEncodedUUID()
        for (i in 2..16) {
            manufacturerData.put(i, encodedUUID[i - 2])
        }
        for (i in 2..16) {
            manufacturerData.put((0x01).toByte())
        }
        builder.setManufacturerData(APPLE_MANUFACTURER_ID, manufacturerData.array(), manufacturerDataMask.array())
        return listOf(builder.build())
    }

    private fun startScanner() {
        var settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build()
        var intent = Intent(applicationContext, BeaconReceiver::class.java)
        scanIntent = PendingIntent.getBroadcast(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        adapter?.bluetoothLeScanner?.startScan(getScanFilter(), settings, scanIntent!!)
    }

    private fun stopScanner() {
        scanIntent.let {
            adapter?.bluetoothLeScanner?.stopScan(it)
        }
        scanIntent = null
    }

    private fun startAdvertiser() {
        var advSettings = AdvertiseSettings.Builder()
        advSettings.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
        advSettings.setConnectable(false)
        advSettings.setTimeout(0)
        advSettings.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)

        var dataBuilder = AdvertiseData.Builder()
        var manufacturerData = ByteBuffer.allocate(24)
        manufacturerData.put(0, (0x02).toByte())
        manufacturerData.put(1, (0x15).toByte())
        var encodedUUID = getEncodedUUID()
        for (i in 2..16) {
            manufacturerData.put(i, encodedUUID[i - 2])
        }
        // major
        manufacturerData.put(18, (0x00).toByte())
        manufacturerData.put(19, (0x09).toByte())
        // minor
        manufacturerData.put(20, (0x00).toByte())
        manufacturerData.put(21, (0x06).toByte())
        // tx power
        manufacturerData.put(22, (0xb5).toByte())
        dataBuilder.addManufacturerData(APPLE_MANUFACTURER_ID, manufacturerData.array())
        var advData = dataBuilder.build()

        advertiseCallback = object: AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                Log.i("bluetooth", "advertisement started")
            }

            override fun onStartFailure(errorCode: Int) {
                Log.w("bluetooth", "advertisement failed $errorCode")
            }
        }

        adapter?.bluetoothLeAdvertiser?.startAdvertising(advSettings.build(), advData, advertiseCallback!!)
    }

    private fun stopAdvertiser() {
        advertiseCallback.let {
            adapter?.bluetoothLeAdvertiser?.stopAdvertising(it)
        }
        advertiseCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        adapter = BluetoothAdapter.getDefaultAdapter()

        if (adapter?.isEnabled != true) {
            Log.i("bluetooth", "bluetooth is not enabled");
        }
        else {
            Log.i("bluetooth", "bluetooth is enabled");
        }

        // register receiver
        startScanner()

        // register sender
        startAdvertiser()
    }
}