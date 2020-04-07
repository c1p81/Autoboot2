package com.luca.innocenti.autoboot
/*
Script in php per il lato server


*/


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_BOOT_COMPLETED
import android.content.IntentFilter
import android.graphics.Color
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
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity(), SensorEventListener {

    private var azimuth_mean: Float = 0.0f
    private var pitch_mean: Float = 0.0f
    private var roll_mean: Float = 0.0f
    private var pitch_mean_old: Float = 0.0f
    private var roll_mean_old: Float = 0.0f
    private var soglia: Float = 2.0f

    private var max_itera: Int = 10000 // il dato viene inviato al server quando e' cumulato
                                       // questo numero di misure
    private var batteryperc: Int = 0
    private var batterytemp: Int = 0
    private var pressione: Int = 0
    private var batterytempf: Double = 0.0
    private var temp_ambiente: Float = 0.0f
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
    private var mBarometer: Sensor ?=null
    private var mAmbient: Sensor ?=null



    class imposta(
        var cicli:Int = 0,
        var soglia:Float = 0.0f) {
                fun Get_cicli():Int {
                    return cicli
                }

                fun Get_soglia():Float {
                    return soglia
                }
                override fun toString(): String {
                    return "${this.cicli}, ${this.soglia}"
                }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val nome = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9","10")
        val spinner = findViewById<Spinner>(R.id.spinner)
        if (spinner != null) {
            val spinnerArrayAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, nome)

            spinner.setAdapter(spinnerArrayAdapter);



            spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                    (parent.getChildAt(0) as TextView).setTextColor(Color.BLACK)
                    (parent.getChildAt(0) as TextView).textSize = 45f
                    (parent.getChildAt(0) as TextView).gravity = Gravity.CENTER
                    (parent.getChildAt(0) as TextView).textAlignment= View.TEXT_ALIGNMENT_GRAVITY
                    id_staz = nome[position]
                    Log.d("nome", id_staz)
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Code to perform some action when nothing is selected
                }
            }
        }



        val testo: TextView = findViewById(R.id.testo) as TextView
        testo.setText(id_staz)
        testo.setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
        testo.setTypeface(null, Typeface.BOLD);


        // Gestore dello stato della batteria
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        this.registerReceiver(batteria, intentFilter)
        //-------------------------

        // Istanzia i sensori
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mBarometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_PRESSURE)
        mAmbient = mSensorManager!!.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)
        if (mAmbient == null)
        {
            Log.d("ambiente","Nessun sensore di temperatura")
        }
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
             batterytemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE,-1)!!
             batterytempf = batterytemp/10.0



            Log.d("batteria",batteryperc.toString()+" "+ batterytempf.toString())
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
        Log.d("test","Allarmato "+currentDate)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER)
                    {
                    conteggio = conteggio + 1
                    x = event.values[0]
                    y = event.values[1]
                    z = event.values[2]

                    // valori istantanei di pitch e roll
                    var pitch_ista = ((Math.atan2(y.toDouble(), z.toDouble())) * (180.0 / Math.PI)).toFloat()
                    var roll_ista = ((Math.atan2((-x).toDouble(), Math.sqrt(((y * y) + (z * z)).toDouble()))) * (180.0 / Math.PI)).toFloat()



                    // calcola il valore medio degli angoli di posizione
                    //azimuth_mean = azimuth_mean + azimuth
                    pitch_mean = pitch_mean + pitch_ista
                    roll_mean = roll_mean + roll_ista
                    //Log.d("Posizione",(pitch_mean/conteggio).toString()+";"+(roll_mean/conteggio).toString())
                    var tr: Float = 0.0f
                    var tp: Float = 0.0f
                    tr = Math.abs(roll_mean_old-roll_ista)
                    tp = Math.abs(pitch_mean_old-pitch_ista)

                    // ALLARME
                    if ((tp > soglia) && (controllo==0))
                    {
                        allarme = 1
                        controllo = 1
                        crea_allarme(1)
                    }
                    if ((tr > soglia) && (controllo==0))
                    {
                        controllo = 1
                        allarme = 2
                        crea_allarme(2)
                    }

                    // ********************* FINE ALLARME ***************


                    if (conteggio == max_itera) {
                        conteggio = 0
                        controllo = 0

                        val currentDate = SimpleDateFormat(
                            "yyyy/MM/dd_HH:mm:ss",
                            Locale.getDefault()
                        ).format(Date())

                        pitch_mean = pitch_mean / max_itera
                        roll_mean = roll_mean / max_itera
                        //azimuth_mean = azimuth_mean/max_itera

                        val url =
                            base_url + "index.php?tempo=" + currentDate + "&pitch=" + "%.2f".format(
                                pitch_mean
                            ).toString() + "&roll=" + "%.2f".format(roll_mean)
                                .toString() + "&batteria=" + batteryperc.toString() + "&id_staz=" + id_staz + "&allarme=" + allarme.toString() + "&temp=" + temp_ambiente.toString() + "&cicli=" + max_itera.toString()+"&pressione="+pressione.toString()+"&soglia="+soglia.toString()


                        pitch_mean_old = pitch_mean
                        roll_mean_old = roll_mean
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
                                        // legge il file di configurazione JSON
                                        // che arriva dal server
                                        val data = result.get()
                                        Log.d("risposta", data)
                                        //val json = "{'cicli':26,'soglia': 1.5}"
                                        var impostazioni = Gson().fromJson(data, imposta::class.java)
                                        max_itera = impostazioni.Get_cicli()
                                        soglia = impostazioni.Get_soglia()

                                    }
                                }
                            }

                        httpAsync.join()
                        //-------------------------------------

                        Log.d("Volley", url)
                    }
            }
            if (event.sensor.type == Sensor.TYPE_PRESSURE)
            {
                pressione = event.values[0].toInt()
                Log.d("pressione",pressione.toString())
            }
            if (event.sensor.type == Sensor.TYPE_AMBIENT_TEMPERATURE)
            {
                temp_ambiente = event.values[0]
                Log.d("ambiente",temp_ambiente.toString())
            }

        }
    }

    override fun onPause() {
        super.onPause()

    }

    override fun onResume() {
        super.onResume()
        mSensorManager!!.registerListener(this,mAccelerometer, SensorManager.SENSOR_DELAY_GAME)
        mSensorManager!!.registerListener(this,mBarometer, SensorManager.SENSOR_DELAY_NORMAL)
        mSensorManager!!.registerListener(this,mAmbient, SensorManager.SENSOR_DELAY_FASTEST)

    }

}

// Gestite l'avvio della app al boot del device
class BootReceiver : BroadcastReceiver() {
    private lateinit var mSensorManager: SensorManager
    private var mSensors: Sensor? = null

    override fun onReceive(c: Context, intent: Intent?) {
        val action = intent?.action
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