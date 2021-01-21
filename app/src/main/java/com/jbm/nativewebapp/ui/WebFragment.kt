package com.jbm.nativewebapp.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.jbm.nativewebapp.R
import com.jbm.nativewebapp.service.BleService
import java.net.URLEncoder

class WebFragment : Fragment() {
    lateinit var mWebView: WebView

    val MY_PERMISSIONS_REQUEST_FINE_LOCATION = 3

    companion object {
        val TAG = WebFragment::class.java.simpleName
    }

    var bleService: BleService? = null
    private var mHandler: MyHandler? = null

    private val bleConnection = object : ServiceConnection {
        override fun onServiceConnected(arg0: ComponentName, arg1: IBinder) {
            bleService = (arg1 as BleService.BleBinder).service
            bleService!!.setHandler(mHandler!!)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bleService = null
        }
    }

    /**
     * Start of Android Fragment LifeCycle function
     */
    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        mHandler = MyHandler(this)
    }

    @Override
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val view = inflater.inflate(R.layout.web_fragment, container, false)

        val jsInterface = JSInterface(this)

        mWebView = view.findViewById(R.id.webView) as WebView

        // linking the interface to the WebView and activating JavaScript for the web view
        val webSettings = mWebView.settings
        webSettings.javaScriptEnabled = true
        mWebView.addJavascriptInterface(jsInterface, "AndroidInterface")
        mWebView.loadUrl("file:///android_asset/web_app.html")

        return view
    }

    @Override
    override fun onResume() {
        super.onResume()

        checkLocationPermission()
        checkBluetoothEnable()
        startBleService(BleService::class.java, bleConnection, null)
    }

    /*
    @Override
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == BleService().REQUEST_ENABLE_BT) {
            startBleService(BleService::class.java, bleConnection, null)
        }
    }

    */
    @Override
    override fun onStop() {
        activity!!.unbindService(bleConnection)
        activity!!.stopService(bleService?.getIntent(requireContext()))
        super.onStop()
    }

    @Override
    override fun onDestroy() {
        mWebView.removeJavascriptInterface("AndroidInterface")
        super.onDestroy()
    }

    /*************************************/

    //Start the Service that will handle all BLE task
    private fun startBleService(service: Class<*>, serviceConnection: ServiceConnection, extras: Bundle?) {
        if (bleService?.SERVICE_CONNECTED == false) {

            val startService = Intent(activity, service)
            if (extras != null && !extras.isEmpty) {
                val keys = extras.keySet()
                for (key in keys) {
                    val extra = extras.getString(key)
                    startService.putExtra(key, extra)
                }
            }
            activity!!.startService(startService)
        }
        val bindingIntent = BleService().getIntent(requireContext())
        activity!!.bindService(bindingIntent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    //Check for permission. If not granted yet, ask for the proper permission
    //Location Permission. Mandatory for working with BLE
    fun checkLocationPermission() {
        // check if location permission has been granted.
        if (ActivityCompat.checkSelfPermission(activity!!,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Location permission has not been granted. So ask for it.
            ActivityCompat.requestPermissions(activity!!,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_FINE_LOCATION)
        }
    }

    // Bluetooth permission
    fun checkBluetoothEnable(): Boolean {
        val mBluetoothAdapter: BluetoothAdapter?

        // Initializes Bluetooth adapter.
        val bluetoothManager = activity!!.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter = bluetoothManager.adapter

        // Ensures Bluetooth is available on the device and it is enabled. If not,
        // displays a dialog requesting user permission to enable Bluetooth.
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, BleService().REQUEST_ENABLE_BT)

            return false
        } else
            return true
    }


    /*
     * This handler will be passed BleService.
     * Data received from BLE is displayed through this handler
     */
    private class MyHandler(fragment: WebFragment) : Handler() {
        // so we don't leak the Activity as Service holding this handler stays around after activity leaves foreground
        private var data: String? = null
        private var frag: WebFragment = fragment

        @Override
        override fun handleMessage(msg: Message) {
            data = msg.obj?.toString()

            Log.d(TAG, "Handler Message from Built in BLE = " + data)

            if (frag != null && data != null) {
                when (msg.what) {
                    BleService().MESSAGE_FROM_BLE -> frag.receiveBLE (data as String)
                }
            }
        }
    }

    fun receiveBLE(data: String) {
        mWebView.evaluateJavascript("javascript: androidToJs(\""
                + URLEncoder.encode(data, "UTF-8") + "\")", null)
    }

    fun starRx() {
        bleService!!.startLeScan()
    }

    fun stopRx () {
        bleService!!.stopLeScan()
    }
}

internal class JSInterface (var mFrag: WebFragment) {
    val TAG = JSInterface::class.simpleName

    /**command sent from the web page  */
    @JavascriptInterface
    fun jsToAndroid(command: String) {
        when(command) {
            "start-rx" -> mFrag.starRx()
            "stop-rx" -> mFrag.stopRx()
        }
    }
}