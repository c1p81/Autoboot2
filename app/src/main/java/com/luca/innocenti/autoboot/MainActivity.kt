package com.luca.innocenti.autoboot
/*
Script in php per il lato server
<?php
$tempo = $_GET['tempo'];
$pitch = $_GET['pitch'];
$roll = $_GET['roll'];
$azimuth = $_GET['azimuth'];
$batteria = $_GET['batteria'];
$id_staz = $_GET['id_staz'];
$allarme = $_GET['allarme'];

//print $pitch + ";" + $roll + ";" + $azimuth;

$fp = fopen('sensore.txt', 'a');
fwrite($fp, $tempo.";".$pitch.";".$roll.";".$azimuth.";".$batteria.";".$id_staz.";".$allarme.PHP_EOL);
fclose($fp);

?>
*/


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.content.IntentFilter
import android.graphics.Typeface
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.PowerManager
import android.util.Log
import android.util.TypedValue
import android.widget.TextView
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
    private var pitch_mean_old: Float = 0.0f
    private var roll_mean_old: Float = 0.0f

    private var max_itera: Int = 20000 // il dato viene inviato al server quando e' cumulato
                                       // questo numero di misure
    private var batteryperc: Int = 0
    private var x: Float= 0.0f
    private var y: Float= 0.0f
    private var z: Float = 0.0f
    private var conteggio: Int = 0
    private var allarme: Int = 0
    private var controllo: Int = 0
    private var id_staz: String = "2"  // identificativo della stazione di misura
    private var base_url: String = "http://150.217.73.108/autoboot/"  //url del webserver

    lateinit var mainHandler: Handler

    private var mSensorManager : SensorManager ?= null
    private var mAccelerometer : Sensor ?= null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val testo: TextView = findViewById(R.id.testo) as TextView
        testo.setText(id_staz)
        testo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
        testo.setTypeface(null, Typeface.BOLD);


        // Gestore dello stato della batteria
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        this.registerReceiver(batteria, intentFilter)
        //-------------------------

        // Gestore accelerometro
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        //--------------------------

        // Wake Lock
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

    fun crea_allarme(tipo: Int){
        val currentDate = SimpleDateFormat("yyyy/MM/dd_HH:mm:ss", Locale.getDefault()).format(Date())
        val url = base_url+"index.php?allarme="+allarme.toString()+"&tempo="+currentDate
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
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            conteggio = conteggio + 1
            x = event.values[0]
            y = event.values[1]
            z = event.values[2]

            // valori istantanei di pitch e roll
            var pitch_ista = (Math.atan2(y.toDouble(), z.toDouble())) * (180.0 / Math.PI)
            var roll_ista = (Math.atan2(
                (-x).toDouble(),
                Math.sqrt(((y * y) + (z * z)).toDouble())
            )) * (180.0 / Math.PI)

            // ALLARME
            if ((Math.abs(pitch_ista-pitch_mean_old) > 1) && (controllo == 0))
            {
                allarme = 1
                controllo = 1
                crea_allarme(1)
            }
            if ((Math.abs(roll_ista-roll_mean_old) > 1) && (controllo == 0))
            {
                controllo = 1
                allarme = 2
                crea_allarme(2)
            }

            // ********************* FINE ALLARME ***************

            // calcola il valore medio
            //azimuth_mean = azimuth_mean + azimuth
            pitch_mean = (pitch_mean + pitch_ista).toFloat()
            roll_mean = (roll_mean + roll_ista).toFloat()

            Log.d("Posizione",(pitch_mean/conteggio).toString()+";"+(roll_mean/conteggio).toString())

            if (conteggio == max_itera) {
                conteggio = 0
                controllo = 0

                val currentDate = SimpleDateFormat("yyyy/MM/dd_HH:mm:ss", Locale.getDefault()).format(Date())

                pitch_mean = pitch_mean/max_itera
                roll_mean = roll_mean/max_itera
                //azimuth_mean = azimuth_mean/max_itera

                val url = base_url+"index.php?tempo="+currentDate+"&pitch="+"%.2f".format(pitch_mean).toString()+"&roll="+"%.2f".format(roll_mean).toString()+"&batteria="+batteryperc.toString()+"&id_staz="+id_staz+"&allarme="+allarme.toString()
                //val url = base_url+"/index.php?tempo="+currentDate+"&pitch="+pitch_mean.toString()+"&roll="+roll_mean.toString()+"&azimuth="+azimuth_mean.toString()+"&batteria="+batteryperc.toString()+"&id_staz="+id_staz


                pitch_mean_old = pitch_mean
                roll_mean_old = pitch_mean
                //azimuth_mean_old = pitch_mean
                pitch_mean = 0.0f
                roll_mean = 0.0f
                azimuth_mean = 0.0f
                allarme = 0

                // Invio al server in modalita' GET con la libreria FUEL
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
                //-------------------------------------

                Log.d("Volley",url)
            }
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

// Gestite l'avvio della app al boot del device
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