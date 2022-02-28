package com.example.fossil_sensor_app_testing

import android.app.*
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import androidx.core.app.JobIntentService
import androidx.core.app.NotificationCompat

class MyJobService : Service()
{

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        createNotificationChannel()


        // Creating the notification here:

        val intent1 = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent1, 0)

        val notification = NotificationCompat.Builder(this, "ChannelID1")
                .setContentTitle("Fossil_Testing")
                .setContentText("Application is running")
                .setContentIntent(pendingIntent)
                .build()

        startForeground(1, notification)

        return super.onStartCommand(intent, flags, startId)
        Log.d("ServiceDebugging", "Started Called")


        return START_STICKY
    }

    private fun createNotificationChannel()
    {
        // Make sure version is Oreo or above.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O )
        {
            val notificationChannel = NotificationChannel(
                    "ChannelID1","Foreground notification", NotificationManager.IMPORTANCE_DEFAULT)

            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(notificationChannel);


        }

    }

    override fun onBind(intent: Intent?): IBinder?
    {
        Log.d("ServiceDebugging", "OnBind() Called")

        return null
    }


    override fun onDestroy() {

        // true to remove notification
        Log.d("ServiceDebugging", "OnDestroy() Called")
        stopForeground(true)
        stopSelf()

        super.onDestroy()
    }
}









//class MyTestService  // Must create a default constructor
//    : IntentService("test-service") {
//    override fun onCreate() {
//        super.onCreate() // if you override onCreate(), make sure to call super().
//        // If a Context object is needed, call getApplicationContext() here.
//    }
//
//    override fun onHandleIntent(intent: Intent?) {
//        // This describes what will happen when service is triggered
//
//        Log.d("onStartJob","Starting Job...")
//        while(true)
//            MainActivity()
//        Log.d("onStartJob","After Calling MainActivity()")
//    }
//}

//class MyJobService : JobService() {
//
//    override fun onStartJob(parameters: JobParameters?): Boolean {
//        // runs on the main thread, so this Toast will appear
//        Log.d("onStartJob","Starting Job...")
//
////        val test = true
////        while(test) {
//            print("Running\n")
//            val intent = Intent(this, MainActivity::class.java)
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//            startActivity(intent)
////        }
//        Log.d("onStartJob","After Calling MainActivity()")
//
//
//        // returning false means the work has been done, return true if the job is being run asynchronously
//        return false
//    }
//
//    override fun onStopJob(parameters: JobParameters?): Boolean {
//        Log.d("onStopJob","Stopping Job...")
//
//
//        // return true to restart the job, hmm....
//        return true
//    }
//
//}
