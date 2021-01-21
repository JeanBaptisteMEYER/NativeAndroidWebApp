package com.jbm.nativewebapp.service

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast

open class BleService : Service() {
    private val TAG = BleService::class.java.simpleName

    var SERVICE_CONNECTED = false
    val MESSAGE_FROM_BLE = 1
    val REQUEST_ENABLE_BT = 3

    val mBinder = BleBinder()

    private var mBluetoothLeScanner: BluetoothLeScanner? = null
    private var mBluetoothAdapter: BluetoothAdapter? = null
    var mHandler: Handler? = null

    private val mScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)

            if (mHandler != null
                    && result != null
                    && result.device != null
                    && result.device.name != null) {


                val rssi = Math.abs(result.rssi)
                val mac = result.device.address.replace(":", "")
                val data = toHexString(result.scanRecord!!.bytes)
                val localName = result.device.name

                val mJSONData = ("{\"t\":\"000\",\"RSSI\":\"" + rssi + "\",\"mac\":\"" + mac + "\",\"data\":\"" + data
                        + "\",\"local_name\":\"" + localName + "\"}")

                mHandler!!.obtainMessage(MESSAGE_FROM_BLE, mJSONData).sendToTarget()

            }
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)

            Log.d(TAG, "onBatchScanResults:")
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Discovery onScanFailed: $errorCode")
            super.onScanFailed(errorCode)
        }
    }

    fun startLeScan() {
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner!!.startScan(mScanCallback)
            Toast.makeText(applicationContext, "Bluetooth Sniffer started", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopLeScan() {
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner!!.stopScan(mScanCallback)
            Toast.makeText(applicationContext, "Bluetooth Sniffer stopped", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate() {
        SERVICE_CONNECTED = true

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        mBluetoothLeScanner = mBluetoothAdapter!!.bluetoothLeScanner
    }

    override fun onBind(intent: Intent): IBinder {
        return mBinder
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLeScan()
        SERVICE_CONNECTED = false
    }

    fun toHexString(data: ByteArray): String {
        val sb = StringBuilder()

        for (b in data) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }

    fun setHandler(mHandler: Handler) {
        this.mHandler = mHandler
    }

    /**
     * Intent to be used to start this service
     *
     * @param context
     * @return
     */
    fun getIntent(context: Context): Intent {
        return Intent(context, BleService::class.java)
    }

    inner class BleBinder : Binder() {
        val service: BleService
            get() = this@BleService
    }
}
