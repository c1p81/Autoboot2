package com.luca.innocenti.autoboot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.RetryPolicy
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), SensorEventListener {
    private var azimuth_mean: Float = 0.0f
    private var pitch_mean: Float = 0.0f
    private var roll_mean: Float = 0.0f
    private var max_itera: Int = 20000
    private var batteryperc: Int = 0
    private var azimuth: Float= 0.0f
    private var pitch: Float= 0.0f
    private var roll: Float = 0.0f
    private var conteggio: Int = 0

    lateinit var mainHandler: Handler

    private var mSensorManager : SensorManager ?= null
    private var mAccelerometer : Sensor ?= null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        this.registerReceiver(batteria, intentFilter)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)


        val wakeLock: PowerManager.WakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::MyWakelockTag").apply {
                    acquire()
                }
            }

    


    }

    private val batteria = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
             batteryperc = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL,0)!!
            Log.d("batteria",batteryperc.toString())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            conteggio = conteggio + 1
            azimuth = event.values[0]
            pitch = event.values[1]
            roll = event.values[2]

            azimuth_mean = azimuth_mean + azimuth
            pitch_mean = pitch_mean + pitch
            roll_mean = roll_mean + roll

            if (conteggio > max_itera) {

                conteggio = 0
                //val queue = Volley.newRequestQueue(this)

                val currentDate = SimpleDateFormat("yyyy/MM/dd_HH:mm:ss", Locale.getDefault()).format(Date())

                pitch_mean = pitch_mean/max_itera
                roll_mean = roll_mean/max_itera
                azimuth_mean = azimuth_mean/max_itera

                val url = "http://192.168.1.102/index.php?tempo="+currentDate+"&pitch="+pitch_mean.toString()+"&roll="+roll_mean.toString()+"&azimuth="+azimuth_mean.toString()+"&batteria="+batteryperc.toString()

                pitch_mean = 0.0f
                roll_mean = 0.0f
                azimuth_mean = 0.0f

                val httpAsync = url
                    .httpGet()
                    .responseString { request, response, result ->
                        when (result) {
                            is Result.Failure -> {
                                val ex = result.getException()
                                println(ex)
                            }
                            is Result.Success -> {
                                val data = result.get()
                                println(data)
                            }
                        }
                    }

                httpAsync.join()

                Log.d("Volley",url)
                //val stringRequest = StringRequest(Request.Method.GET, url,
                //    Response.Listener { response ->
                //        Log.d("Volley", "Response is: ${response}")
                //    },
                //    Response.ErrorListener { Log.d("Volley", "No")})

                //val retryPolicy: RetryPolicy = DefaultRetryPolicy(
                //    DefaultRetryPolicy.DEFAULT_TIMEOUT_MS, //30000
                //    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                //    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
                //)
                //stringRequest.setRetryPolicy(retryPolicy);

                //queue.add(stringRequest)
            }
            //Log.d("acc", conteggio.toString()+" "+azimuth.toString())
        }
    }

    override fun onPause() {
        super.onPause()

    }

    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(this,mAccelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

}


class BootReceiver : BroadcastReceiver() {
    private lateinit var mSensorManager: SensorManager
    private var mSensors: Sensor? = null

    override fun onReceive(c: Context, intent: Intent?) {
        val action = intent?.action
        Log.d("ASD123123", "RECEIVED BOOT")
        val intent = Intent(c, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        c.startActivity(intent)
        val b = intent?.toUri(Intent.URI_INTENT_SCHEME)
        when (action) {

            ACTION_BOOT_COMPLETED -> startWork(c)
        }
    }


    private fun startWork(context: Context) {
        Log.d("Test", "Test")

    }


}