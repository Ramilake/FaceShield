package com.example.fossil_sensor_app_testing

//import androidx.lifecycle.DefaultLifecycleObserver
//import androidx.lifecycle.LifecycleOwner
//import androidx.lifecycle.ProcessLifecycleOwner
import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Vibrator
import android.support.wearable.activity.WearableActivity
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import java.io.FileWriter
import java.io.IOException
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.sqrt


class MainActivity : WearableActivity(), SensorEventListener, View.OnClickListener {

    // Declaring some sensor information here.
    private lateinit var sensorManager: SensorManager
    private var acc: Sensor? = null
    private var gyro: Sensor? = null
    private var mag: Sensor? = null

    var large_wind: Array<DoubleArray> = Array(600) { DoubleArray(6) }
    var small_wind: Array<DoubleArray> = Array(10) { DoubleArray(6) }


    // Declaring two arrays to store the XYZ and the RPY
    private var RPY = arrayOf(0.0f, 0.0f, 0.0f)
    private var XYZ = arrayOf(0.0f, 0.0f, 0.0f)

    var csvBoolToggle: Boolean = false
    var csvLock: Boolean = false
    var ringtoneLock: Boolean = false
    var traintestLock: Boolean = false

    // Declaring the vibration and audio tone tool. Also, adjusting the vibration length.
    private lateinit var vibrator: Vibrator
    private lateinit var toneGen: ToneGenerator
    private val tone = ToneGenerator.TONE_PROP_BEEP
    private val vibrationLength = 70
    private var lastVibTime: Long = 0
    private var lastNotificationTime: Long = 0

    // Here are the holding variables for the data coming from the sensors.
    private var ax = 0f
    private var ay = 0f
    private var az = 0f
    private var gx = 0f
    private var gy = 0f
    private var gz = 0f
    private var orien_x = 0f
    private var orien_y = 0f
    private var orien_z = 0f
    private var mark = 0f
    private var traintest = 0

    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    private var topCounter = 0
    private var bottomCounter = 0


    // Declaring file information tools. Also, the header for the CSV file is here.
    var fileWriter: FileWriter? = null

    //    private val CSV_HEADER = "x,y,z,roll,pitch,yaw,prediction,mark,time (milli),time"
    private val CSV_HEADER = "acc_x, acc_y, acc_z, gyro_x, gyro_y, gyro_z, prediction, mark, time (milli), time, traintest"

    // Background stuff

    private val INTERVAL: Long = 100 // 3000 == 3 seconds
    private val YOUR_APP_PACKAGE_NAME: String = "com.example.fossil_sensor_app_testing"

    private var stopTask = false

//    private val lifecycleListener: SampleLifecycleListener by lazy {
//        SampleLifecycleListener()
//    }
//    private fun setupLifecycleListener() {
//        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleListener)
//    }


    /*
    onCreate() function:

    This is the onCreate() function. It sets the view and calls the initialize function for the sensors.
    I also assign the vibrator and toneGeneration variables here.

    Note: Temporarily I have assigned the "Keep_Screen_On" to force the screen on... This is the way
    of keeping the application running forever right now.

     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super<WearableActivity>.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Keeping the screen on might keep the application open until they close it.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Enables Always-on
        setAmbientEnabled()
        initSensors()
//        setupLifecycleListener()

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

        val date = Date()
        val sdf = SimpleDateFormat("MM-dd-yyyy h:mm:ss a")
        val formattedDate: String = sdf.format(date)
        fileWriter = FileWriter("/storage/emulated/0/Download/session_" + formattedDate + ".csv")
        fileWriter?.append(CSV_HEADER)


        // Doing some work to make it background here.

//        Working foreground notification here.
//        intent = Intent(this, MyJobService::class.java)
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
//        {
//            Log.d("Foreground Service","Starting Foreground")
//            startForegroundService(intent)
//            startService(intent)
//        }
//        else
//        {
//            Log.d("Foreground Service","Starting Service")
//            startService(intent)
//        }

//
//        val jobScheduler = getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
//
//        val jobInfo = JobInfo.Builder(11, ComponentName(this@MainActivity, MyJobService::class.java))
//                .setMinimumLatency(1).build()
//        jobScheduler.schedule(jobInfo)

//        moveTaskToBack(true) // This moves it instantly to background

        // This solution actually works and it useful it just will relaunch the application if
        // someone exits it and the watch turns off...
        // Start your (polling) task
//        stopTask = false;
//        val task: TimerTask = object : TimerTask() {
//            @SuppressLint("ServiceCast")
//            override fun run() {
//                // If you wish to stop the task/polling
//                if (stopTask) {
//                    cancel()
//                }
//                val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
//                val runningAppProcessInfo = am.runningAppProcesses
//                val getRunningInfo = am.getRunningTasks(10)
//                // detect an app
//                for (i in runningAppProcessInfo.indices) {
//                    if (runningAppProcessInfo[i].processName == "com.example.fossil_sensor_app_testing") {
//                        Log.d("something",i.toString())
//                        am.moveTaskToFront(taskId,0)
//                    }
//                }
////                 Check foreground app: If it is not in the foreground... bring it!
////                if (foregroundTaskPackageName != YOUR_APP_PACKAGE_NAME) {
////                    val LaunchIntent = packageManager.getLaunchIntentForPackage(YOUR_APP_PACKAGE_NAME)
////                    startActivity(LaunchIntent)
////                }
//            }
//        }
//    var timer = Timer()
//    timer.scheduleAtFixedRate(task, 0, INTERVAL)

    }// Ending bracket for onCreate()


//    override fun onDestroy() {
//        super.onDestroy()
//        Log.d(ContentValues.TAG, "onDestroy called.")
//    }


    /*
    initSensors()

    This function is called on inside of the onCreate() function.
    I assign the sensorManager to its service and then initialize sensors to their respective one.
    From there I check to make sure that they were connected properly. Debug statements are made.
    These statements will appear at the top of the debugging.
    If connected properly then connect them to a listener, to be called when they update.

     */
    private fun initSensors() {

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        // Get Sensors
        acc = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        // Below is checking that the sensors are correctly connected and if not printing and if they are then putting a listener on them.
        if (acc != null) {
            Log.d("DEBUG", "acc found")
            acc?.also { m -> sensorManager.registerListener(this, m, SensorManager.SENSOR_DELAY_GAME) }
        } else {
            Log.d("DEBUG", "acc NOT found")
        }

        if (gyro != null) {
            Log.d("DEBUG", "gyro found")
            gyro?.also { m -> sensorManager.registerListener(this, m, SensorManager.SENSOR_DELAY_GAME) }
        } else {
            Log.d("DEBUG", "gyro NOT found")
        }
    } // Ending brace for initSensors().


    /*
    onAccuracyChanged()

    Not sure what to do with this function right now. Need to look into it some more.
    Google to see what I can use it for.

     */
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    /*
    onSensorChanged()

    This is the main function of the application...
    When the function detects that a sensor has changed, ie new value, it will pass it into here.
    From there I have if statements to see what sensor changed.
    Depending on sensor do some processing and assign the value to variables to be stored in CSV or
    used for the prediction.

    Currently the "acc" (Accelerometer) sensor does the actual writing to CSV and does the
    prediction based on the RandomForestClassifier model trained on Colab.

     */
    override fun onSensorChanged(event: SensorEvent?) {


        if (event?.sensor == gyro) {

            val v = event?.values ?: return

            gx = event.values[0]
            gy = event.values[1]
            gz = event.values[2]

        }

        if (event?.sensor == acc) {
            val v = event?.values ?: return


//            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
//
//            SensorManager.getRotationMatrix(
//                    rotationMatrix,
//                    null,
//                    accelerometerReading,
//                    magnetometerReading
//            )
//
//            // "rotationMatrix" now has up-to-date information.
//
//            SensorManager.getOrientation(rotationMatrix, orientationAngles)


//            val roll = atan2(v[1], v[2]) * 180 / kotlin.math.PI
//            val pitch = atan2(-v[0], sqrt(v[1] * v[1] + v[2] * v[2])) * 180 / kotlin.math.PI
//            val yaw = 180 * atan2(v[2], sqrt(v[0] * v[0] + v[2] * v[2])) / kotlin.math.PI
//
//            RPY[0] = roll.toFloat()
//            RPY[1] = pitch.toFloat()
//            RPY[2] = yaw.toFloat()

//            Log.d("acc", "Orientation:" + RPY.contentToString())

            // Default values for sensor.
            ax = event.values[0]
            ay = event.values[1]
            az = event.values[2]


//            gx = (0.9f * gx + 0.1f * ax) as Float
//            ax = ax - gx
//            gy = (0.9f * gy + 0.1f * ay) as Float
//            ay = ay - gy
//            gz = (0.9f * gz + 0.1f * az) as Float
//            az = az - gz
//
//            XYZ[0] = ax
//            XYZ[1] = ay
//            XYZ[2] = az

//            Log.d("acc", "acc:" + XYZ.contentToString())


//            This is the good working one:
            val features = DoubleArray(6)
            features[0] = ax.toString().toDouble()
            features[1] = ay.toString().toDouble()
            features[2] = az.toString().toDouble()
            features[3] = gx.toString().toDouble()
            features[4] = gy.toString().toDouble()
            features[5] = gz.toString().toDouble()

//            val features = DoubleArray(7)


//            features[3] = XYZ[0].toString().toDouble()
//            features[4] = XYZ[1].toString().toDouble()
//            features[5] = XYZ[2].toString().toDouble()
//            features[6] = hr.toString().toDouble()

            Log.d("acc", "acc:" + features.contentToString())


            // New prediction stuff
            // Loading up the data array and populating the lists with data.
//            for (i in 0..19)
//            {
//                data_arr[i][0] = ax.toString().toDouble()
//                data_arr[i][1] = ay.toString().toDouble()
//                data_arr[i][2] = az.toString().toDouble()
//                data_arr[i][3] = XYZ[0].toString().toDouble()
//                data_arr[i][4] = XYZ[1].toString().toDouble()
//                data_arr[i][5] = XYZ[2].toString().toDouble()
//
//            }
//
//             populating the lists
//            x_list.add(ax.toString().toDouble())
//            y_list.add(ay.toString().toDouble())
//            z_list.add(az.toString().toDouble())
//            roll_list.add(XYZ[0].toString().toDouble())
//            pitch_list.add(XYZ[1].toString().toDouble())
//            yaw_list.add(XYZ[2].toString().toDouble())
//
//
//
//
//
//
//
//            // If the data is done being loaded. (the 6x20 stuff)
////            if (data_arr[19][5] != 0.0) {
//            if (x_list.size == 20) {
//                for (i in 0..19)
////                {
////                    println("List length" + x_list[i])
////                }
//
//
//
//                pred_arr[0] = calculateSD(x_list)
//                pred_arr[1] = calculateSD(y_list)
//                pred_arr[2] = calculateSD(z_list)
//                pred_arr[3] = calculateSD(roll_list)
//                pred_arr[4] = calculateSD(pitch_list)
//                pred_arr[5] = calculateSD(yaw_list)
//
//                pred_arr[6] = x_list.average()
//                pred_arr[7] = y_list.average()
//                pred_arr[8] = z_list.average()
//                pred_arr[9] = roll_list.average()
//                pred_arr[10] = pitch_list.average()
//                pred_arr[11] = yaw_list.average()
//
//                pred_arr[12] = 0.toDouble()
//                pred_arr[13] = 0.toDouble()
//                pred_arr[14] = 0.toDouble()
//                pred_arr[15] = 0.toDouble()
//                pred_arr[16] = 0.toDouble()
//                pred_arr[17] = 0.toDouble()
//
//                var x_count = 0
//                var y_count = 0
//                var z_count = 0
//                var roll_count = 0
//                var pitch_count = 0
//                var yaw_count = 0
//
//                for (i in 1..18)
//                {
//                    if (x_list[i] > x_list[i - 1] && x_list[i] > x_list[i + 1])
//                    {
//                        x_count++
//                    }
//
//                    if (y_list[i] > y_list[i - 1] && y_list[i] > y_list[i + 1])
//                    {
//                        y_count++
//                    }
//
//                    if (z_list[i] > z_list[i - 1] && z_list[i] > z_list[i + 1])
//                    {
//                        z_count++
//                    }
//
//                    if (roll_list[i] > roll_list[i - 1] && roll_list[i] > roll_list[i + 1])
//                    {
//                        roll_count++
//                    }
//
//                    if (pitch_list[i] > pitch_list[i - 1] && pitch_list[i] > pitch_list[i + 1])
//                    {
//                        pitch_count++
//                    }
//
//                    if (yaw_list[i] > yaw_list[i - 1] && yaw_list[i] > yaw_list[i + 1])
//                    {
//                        yaw_count++
//                    }
//                }
//
//                pred_arr[18] = x_count.toDouble()
//                pred_arr[19] = y_count.toDouble()
//                pred_arr[20] = z_count.toDouble()
//                pred_arr[21] = roll_count.toDouble()
//                pred_arr[22] = pitch_count.toDouble()
//                pred_arr[23] = yaw_count.toDouble()
//
//
//                val features = DoubleArray(24)
//                for (i in 0..23)
//                {
//                    features[i] = pred_arr[i]
//                }
//
//                for (i in 0..23)
//                {
//                    println(features[i])
//                }
//                println("END OF FEATURES")
//
//
//                val prediction: Int = RandomForestClassifier.predict(features)
//                println("trying" + prediction)
//                if (prediction != 0) {
//
//                    println("prediction: " + prediction.toString())
//
//                    updateVibration()
//
//                }
//
//
//
//
//
//
//
//
//
//
//                // Clearing the lists for next calculation
//                x_list.clear()
//                y_list.clear()
//                z_list.clear()
//                roll_list.clear()
//                pitch_list.clear()
//                yaw_list.clear()
//
//                print("here")
//                println("Breaking up \n\n\n")
//
//            }


            // Window Prediction stuff



//            if (wind_counter <= 499) {
//                x_list[wind_counter] = ax
//                y_list[wind_counter] = ay
//                z_list[wind_counter] = az
//                roll_list[wind_counter] = XYZ[0]
//                pitch_list[wind_counter] = XYZ[1]
//                yaw_list[wind_counter] = XYZ[2]
//                wind_counter += 1
//            } else {
//                wind_counter = 0
//            }
//
//
//            if (wind_counter_small <= 19) {
////                println("length is " +  x_list_small.size+ ", updating the small window " + "at " + wind_counter_small)
//                x_list_small[wind_counter_small] = ax
//                y_list_small[wind_counter_small] = ay
//                z_list_small[wind_counter_small] = az
//                roll_list_small[wind_counter_small] = XYZ[0]
//                pitch_list_small[wind_counter_small] = XYZ[1]
//                yaw_list_small[wind_counter_small] = XYZ[2]
//                wind_counter_small += 1
//            } else {
//                wind_counter_small = 0
//            }



//            println("average of big window x " + x_list.average())
//            println("average of small window x " + x_list_small.average())
//            println("computed: " + x_list.average() / x_list_small.average())

//            println("%.3f".format(x_list.average()).toDouble())
//            println("%.3f".format(x_list_small.average()).toDouble())
//            println("")
//            println("%.3f".format(y_list.average()).toDouble())
//            println("%.3f".format(y_list_small.average()).toDouble())
//            println("")
//            println("%.3f".format(z_list.average()).toDouble())
//            println("%.3f".format(z_list_small.average()).toDouble())
//            println("")
//            println("%.3f".format(roll_list.average()).toDouble())
//            println("%.3f".format(roll_list_small.average()).toDouble())
//            println("")
//            println("%.3f".format(pitch_list.average()).toDouble())
//            println("%.3f".format(pitch_list_small.average()).toDouble())
//            println("")
//            println("%.3f".format(yaw_list.average()).toDouble())
//            println("%.3f".format(yaw_list_small.average()).toDouble())
//            println("")


//
//            var big_wind_whole_avg: Float = ((x_list.average() + y_list.average() + z_list.average() + roll_list.average() +
//                    pitch_list.average() + yaw_list.average()) / 6).toFloat()
//
//            var small_wind_whole_avg: Float = ((x_list_small.average() + y_list_small.average() + z_list_small.average() + roll_list_small.average() +
//                    pitch_list_small.average() + yaw_list_small.average()) / 6).toFloat()

//            var big_wind_whole_avg = (x_list.average()) / 1
//            var small_wind_whole_avg = (x_list_small.average()) / 1

//            var big_wind_whole_avg = ("%.3f".format(x_list.average()).toDouble() +
//                    ("%.3f".format(y_list.average()).toDouble()) +
//                    ("%.3f".format(z_list.average()).toDouble()) +
//                    ("%.3f".format(roll_list.average()).toDouble()) +
//                    ("%.3f".format(pitch_list.average()).toDouble()) +
//                    ("%.3f".format(yaw_list.average()).toDouble())) / 6
//            var small_wind_whole_avg = ("%.3f".format(x_list_small.average()).toDouble() +
//                    ("%.3f".format(y_list_small.average()).toDouble()) +
//                    ("%.3f".format(z_list_small.average()).toDouble()) +
//                    ("%.3f".format(roll_list_small.average()).toDouble()) +
//                    ("%.3f".format(pitch_list_small.average()).toDouble()) +
//                    ("%.3f".format(yaw_list_small.average()).toDouble())) / 6

//            println("big" + pitch_list)
//            println("small" + pitch_list_small)


//            if (x_list[499] != 0f) {
//                var computed = ("%.3f".format(big_wind_whole_avg).toDouble()) / ("%.3f".format(small_wind_whole_avg).toDouble())
//
//                println("average of big window x " + big_wind_whole_avg)
//                println("average of small window x " + small_wind_whole_avg)
//                println("computed: " + ("%.3f".format(computed).toDouble()))
//
//                if ((computed >= -0.5) && (computed <= .5) || computed.isNaN()) {
//                    println("No significant movement-----------")
//                } else {
//                    println("significant movement++++++++++++++++")
//                }
//            }


//          Old Prediction stuff
            val prediction: Int = RandomForestClassifier.predict(features)
//            val prediction: DoubleArray = (RandomForestClassifier.score(features))

            if (prediction != 0) {

                println("prediction: " + prediction.toString())

                updateVibration()

            }


            val timestamp = Timestamp(System.currentTimeMillis())
            val timemilli = System.currentTimeMillis()

            // Writing the data to a file, created onButtonClick()...
            if (csvBoolToggle == true) {

                // Actually writing to the file
                val data =
//                              ax.toString() +
//                        "," + ay.toString() +
//                        "," + az.toString() +
//                        "," + RPY[0].toString() +
//                        "," + RPY[1].toString() +
//                        "," + RPY[2].toString() +
//                        "," + prediction.toString() +
//                        "," + mark.toString() +
//                        "," + timemilli +
//                        "," + timestamp +
//                        "\n"

                        ax.toString() +
                        "," + ay.toString() +
                        "," + az.toString() +

                        "," + gx.toString() +
                        "," + gy.toString() +
                        "," + gz.toString() +

                        "," + prediction.toString() +
                        "," + mark.toString() +
                        "," + timemilli +
                        "," + timestamp +
                        "," + traintest +
                        "\n"


                print("csv string: " + data)
                mark = 0f

                try {
                    fileWriter?.append(data)

                    println("Write CSV successfully!")
                } catch (e: Exception) {
                    println("Writing CSV error!")
                    e.printStackTrace()
                } finally {
                    try {
//                        fileWriter!!.flush()
//                        fileWriter!!.close()
                    } catch (e: IOException) {
                        println("Flushing/closing error!")
                        e.printStackTrace()
                    }
                }
            }

        }

    }// Ending brace for updating sensors changed.


    /*
    updateVibration()

    This function handles the vibrations if the prediction value from the model == 1.
    It also will play short ringtone.

     */
    private fun updateVibration() {

        val t = System.currentTimeMillis()
        if ((lastVibTime + vibrationLength < t)) {

            vibrator.vibrate(vibrationLength.toLong())

//            val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
//            val vibrationPattern = longArrayOf(0, 300, 50, 100)
//            val indexInPatternToRepeat = -1
//            vibrator.vibrate(vibrationPattern, indexInPatternToRepeat)

            lastVibTime = t

            if (ringtoneLock == true) {
                if (lastNotificationTime + 2000 < t) {
                    //                val notification: Uri =
                    //                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    //                val r = RingtoneManager.getRingtone(applicationContext, notification)
                    //                r.play()


                    val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val r = RingtoneManager.getRingtone(applicationContext, notification)
                    r.play()
                    lastNotificationTime = t
                }
            }
        }
    }


    // This is for the exit button, mark button and toggle button...
    override fun onClick(v: View?) {
        if (v != null) {
            when (v.id) {

                // Exiting the application and closing the writing to file
                // Changing this clicking the button to exit three times
//                R.id.exitButton -> {
//
//                    try {
//                        fileWriter!!.flush()
//                        fileWriter!!.close()
//                    } catch (e: IOException) {
//                        println("Flushing/closing error!")
//                        e.printStackTrace()
//                    }
//
//                    finish()
//                    finishAffinity();
//                    System.exit(0)
//
//                }

                R.id.ringtoneButton -> {

                    if (ringtoneLock == false) {
                        ringtoneLock = true
                        var message: TextView = findViewById<TextView>(R.id.ringtoneButton)
                        message = findViewById(R.id.ringtoneButton)
                        message.text = "RT: Enabled"

                    } else {
                        ringtoneLock = false
                        var message: TextView = findViewById<TextView>(R.id.ringtoneButton)
                        message = findViewById(R.id.ringtoneButton)
                        message.text = "RT: Disabled"
                    }
                }

                // Putting a mark in the csv...
                R.id.markButton -> {
                    mark = 1f
                    println("mark: " + mark)
                }

                R.id.traintestButton -> {
                    if (traintestLock == false) {
                        traintestLock = true
                        var message: TextView = findViewById<TextView>(R.id.traintestButton)
                        message = findViewById(R.id.traintestButton)
                        message.text = "Positive"
                        traintest = 1

                    } else {
                        traintestLock = false
                        var message: TextView = findViewById<TextView>(R.id.traintestButton)
                        message = findViewById(R.id.traintestButton)
                        message.text = "Negative"
                        traintest = 0
                    }
                }


                // Toggling the writing to CSV on or off..
                R.id.csvButton -> {

                    if (csvLock == true)
                    {

                        var color: TextView = findViewById<TextView>(R.id.csvToggleText)
                        var message: TextView = findViewById<TextView>(R.id.csvToggleText)
                        message = findViewById(R.id.csvToggleText)

                        if (csvBoolToggle == false) {
                            csvBoolToggle = true
                            message.text = "CSV: Enabled"
                            color.setBackgroundColor(Color.parseColor("#E91E63"))


                            // If 'true' then I need to create the file to write to...
                            // Creating the file...
//                            val date = Date()
//                            val sdf = SimpleDateFormat("MM-dd-yyyy h:mm:ss a")
//                            val formattedDate: String = sdf.format(date)


                            try {
//                                fileWriter = FileWriter("/storage/emulated/0/Download/session_" + formattedDate + ".csv")
//                                fileWriter?.append(CSV_HEADER)
                                fileWriter?.append('\n')
                                println("Created CSV successfully!")
                            } catch (e: Exception) {
                                println("Creating CSV error!")
                                e.printStackTrace()
                            } finally {
                                try {
//                                    fileWriter!!.flush()
                                } catch (e: IOException) {
                                    println("Flushing/closing error!")
                                    e.printStackTrace()
                                }
                            }


                        } // End of if statment for setting true.
                        else {
                            csvBoolToggle = false
                            message.text = "CSV: Disabled"
                            color.setBackgroundColor(Color.parseColor("#3F51B5"))

                        }
                    }
                    else
                    {
                        Log.d("CSVLock", "Lock is not enabled to write out/stop writing...")
                    }
                }
            }
        }
    }


    /*
    OnKeyDown()

    This function I use to count the number of times the physical buttons are pressed on the watch.

    The top button I require be pressed 3 times in order to now 'exit' the application.
    The bottom button I require be pressed 2 times to enable the lock, allowing CSV to be written to
        AND turned off. So the counter will increment with every press but the lock will only be set
        to true when the lock is on even numbers 2,4,6 etc. Then the CSV can be enabled when on even numbers.
        so click twice and then enable csv and then click a third time to 'lock' csv into place so
        no accidental presses will disable the writing.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return if (event.repeatCount == 0) {
            when (keyCode) {
                KeyEvent.KEYCODE_STEM_1 -> {
                    // Do stuff
                    topCounter++
                    Log.d("ButtonCounter: ", "topCounter: " + topCounter.toString())

                    if (topCounter == 3) {
                        try {
//                            fileWriter!!.flush()
                            fileWriter!!.close()
                        } catch (e: IOException) {
                            println("Flushing/closing error!")
                            e.printStackTrace()
                        }

                        finish()
                        finishAffinity();
                        System.exit(0)
                    }
                    true
                }
                KeyEvent.KEYCODE_STEM_2 -> {

                    bottomCounter++
                    Log.d("ButtonCounter: ", "bottomCounter: " + bottomCounter.toString())

                    if (bottomCounter % 2 == 0) {
                        Log.d("ButtonCounter in Loop:", "bottomCounter: " + bottomCounter.toString())
                        csvLock = true

                        var csvbutt: Button = findViewById(R.id.csvButton)
                        csvbutt.performClick()

                    } else {
                        csvLock = false
                    }
                    true
                }
                else -> {
                    super.onKeyDown(keyCode, event)
                }
            }
        } else {
            super.onKeyDown(keyCode, event)
        }
    }


    // This tells in the logs if the application is running in foreground or background
    // It will notify when it changes states too.
//    class SampleLifecycleListener : DefaultLifecycleObserver {
//
//        private val YOUR_APP_PACKAGE_NAME: String = "com.example.fossil_sensor_app_testing"
//        private val INTERVAL: Long = 300 // poll every 3 secs
//        private var stopTask = false
//
//
//
//        override fun onStart(owner: LifecycleOwner) {
//            Log.d("SampleLifecycle", "Returning to foreground…")
//        }
//
//        override fun onStop(owner: LifecycleOwner) {
//            Log.d("SampleLifecycle", "Moving to background…")
//
//        }
//    }

//    fun calculateSD(numArray: List<Double>): Double {
//        var sum = 0.0
//        var standardDeviation = 0.0
//
//        for (num in numArray) {
//            sum += num
//        }
//
//        val mean = sum / numArray.size
//
//        for (num in numArray) {
//            standardDeviation += Math.pow(num - mean, 2.0)
//        }
//
//        val divider = numArray.size - 1
//
//        return Math.sqrt(standardDeviation / divider)
//    }


} // Ending brace for entire thing.

