package com.kkk.heartbeatgooglefit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.TextView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.Scope
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.FitnessOptions
import com.google.android.gms.fitness.data.DataSet
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.request.DataReadRequest
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {
    private var mApiClient: GoogleApiClient? = null
    private var responseStepCount = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        callGoogleClient()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        when (resultCode) {
            Activity.RESULT_OK -> when (requestCode) {
                2 ->
                    accessGoogleFit()
                else -> {
                    Log.i("TAG", "no data()")
                    // Result wasn't from Google Fit
                }
            }
            else -> {
                // Permission not granted
                Log.i("TAG", " Permission not granted()")
                //Log.i("TAGS", resultCode.toString())
            }
        }
    }

    @SuppressLint("NewApi")
    private fun callGoogleClient() {
        val fitnessOptions = FitnessOptions.builder()
            .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_HEART_RATE_SUMMARY, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_CALORIES_EXPENDED, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.TYPE_HEART_POINTS, FitnessOptions.ACCESS_READ)
            .addDataType(DataType.AGGREGATE_HEART_POINTS, FitnessOptions.ACCESS_READ)
            .build()

        val account = GoogleSignIn.getAccountForExtension(this, fitnessOptions)

        if (!GoogleSignIn.hasPermissions(account, fitnessOptions)
        ) {
            GoogleSignIn.requestPermissions(this, 2, account, fitnessOptions);
        } else {
            accessGoogleFit()
        }
    }

    private fun accessGoogleFit() {
        if (mApiClient == null) {
            mApiClient = GoogleApiClient.Builder(this)
                .addApi(Fitness.HISTORY_API)
                .addScope(Scope(Scopes.FITNESS_ACTIVITY_READ_WRITE))
                .addConnectionCallbacks(
                    object : GoogleApiClient.ConnectionCallbacks {
                        override fun onConnected(bundle: Bundle?) {
                            //Log.i("Connected", "Connected!!!")
                            ViewTodayStepCountTask(
                                this@MainActivity,
                                mApiClient,
                                responseStepCount
                            ).execute()
                        }

                        override fun onConnectionSuspended(i: Int) {
                            if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                                Log.i("Network Lost", "onConnectionSuspended!!!")
                            } else if (i
                                == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED
                            ) {
                                Log.i("Disconnected", "onConnectionSuspended!!!")
                            }
                        }
                    }
                )
                .enableAutoManage(
                    this,
                    4,
                    GoogleApiClient.OnConnectionFailedListener { result ->
                        result.errorMessage?.let { Log.i("onConnectionFailed", it) }
                    })
                .build()
        }
    }


    private class ViewTodayStepCountTask(
        context: Context,
        mGoogleApiClient: GoogleApiClient?,
        responseStepCount: Int
    ) :
        AsyncTask<String?, String?, ArrayList<DailyStep>?>() {
        private var mApiClient = mGoogleApiClient
        private var context = context
        private var txtView: TextView? = null
//        private var prograssBar: ArcProgress? = null
        private var responseStepCount = responseStepCount

        override fun onPreExecute() {
//            txtView = (context as Activity).findViewById<TextView>(R.id.main_steps_count)
//            prograssBar =
//                (context as Activity).findViewById<ArcProgress>(R.id.daily_steps_progress_circular)

        }

        @SuppressLint("NewApi")
        override fun doInBackground(vararg p0: String?): ArrayList<DailyStep>? {
            val calendar: Calendar = Calendar.getInstance()
            val year: Int = calendar.get(Calendar.YEAR)
            val month: Int = calendar.get(Calendar.MONTH)
            val day: Int = calendar.get(Calendar.DATE)
            calendar.set(year, month, day, 23, 59, 59)
            val endTime = calendar.timeInMillis
            calendar.add(Calendar.WEEK_OF_YEAR, -4)
            val startTime = calendar.timeInMillis
            var count = "0"
            var stepList = ArrayList<DailyStep>()

            val dateFormat = DateFormat.getDateInstance()
            val timeFormat = DateFormat.getTimeInstance()
            Log.e(
                "History",
                "Range Start: " + dateFormat.format(startTime) + " ${timeFormat.format(startTime)}"
            )
            Log.e(
                "History",
                "Range End: " + dateFormat.format(endTime) + " ${timeFormat.format(endTime)}"
            )

            //Check how many steps were walked and recorded in today
//            mApiClient?.let {
//                val result = Fitness.HistoryApi.readDailyTotalFromLocalDevice(
//                    it,
//                    DataType.TYPE_STEP_COUNT_DELTA
//                )
//                    .await(1, TimeUnit.MINUTES)
//                if (result.total != null) {
//                    showDataSet(result.total!!)
//                }
//            }

            //Check how many steps were walked and recorded in the last 4 weeks

            val ESTIMATED_STEP_DELTAS: DataSource = DataSource.Builder()
                .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .setType(DataSource.TYPE_DERIVED)
                .setStreamName("estimated_steps")
                .setAppPackageName("com.google.android.gms")
                .build()
            val readRequest = startTime?.let {
                endTime?.let { it1 ->
                    DataReadRequest.Builder()
//                        .aggregate(
//                            ESTIMATED_STEP_DELTAS,
//                            DataType.AGGREGATE_STEP_COUNT_DELTA
//                        )
//                        .aggregate(
//                            DataType.TYPE_HEART_RATE_BPM,
//                            DataType.AGGREGATE_HEART_RATE_SUMMARY
//                        )
                        .aggregate(
                            DataType.TYPE_HEART_POINTS,
                            DataType.AGGREGATE_HEART_POINTS
                        )
//                        .aggregate(
//                            DataType.TYPE_CALORIES_EXPENDED,
//                            DataType.AGGREGATE_CALORIES_EXPENDED
//                        )
                        .bucketByTime(1, TimeUnit.DAYS)
                        .setTimeRange(it, it1, TimeUnit.MILLISECONDS)
                        .build()
                }
            }
            //Log.e("History", "Read Request: OK")
            val dataReadResult =
                Fitness.HistoryApi.readData(mApiClient, readRequest).await(1, TimeUnit.MINUTES)

//            Log.e("History", "Read Result: OK")
//            Log.e("History", "Bucket Size in Read Result" + dataReadResult.buckets.size)
//            Log.e("History", "Dataset Size in Read Result" + dataReadResult.dataSets.size)

            //Used for aggregated data
            if (dataReadResult.buckets.size > 0) {
                Log.e("History", "Number of buckets: " + dataReadResult.buckets.size)
                for (bucket in dataReadResult.buckets) {
                    val dataSets = bucket.dataSets
                    for (dataSet in dataSets) {
                        showDataSet(dataSet)?.let {
                            stepList.add(it)
                        }
                    }
                }
            } else if (dataReadResult.dataSets.size > 0) {
                 Log.e("History", "Number of returned DataSets: " + dataReadResult.dataSets.size)
                for (dataSet in dataReadResult.dataSets) {
                    showDataSet(dataSet)?.let { stepList.add(it) }
                }
            }
            return stepList
        }

        override fun onPostExecute(dailyStepList: ArrayList<DailyStep>?) {
            super.onPostExecute(dailyStepList)
            Log.e("dailyStepList",dailyStepList?.size.toString())
//            dailyStepList?.let { homeViewModel.getDailyUpdateStep(context, it) }
//            reverseTimer()
        }

        override fun onProgressUpdate(vararg values: String?) {
            super.onProgressUpdate(*values)
        }

        private fun showDataSet(dataSet: DataSet): DailyStep? {
            //Log.e("History", "Show Data: OK")
            //Log.e("History", "Data returned for Data type: " + dataSet.dataType.name)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            val timeFormat = DateFormat.getTimeInstance()
            var stepCountList = ArrayList<Double>()
            var dateList = ArrayList<String>()
            var dailyStep: DailyStep? = null
            var count = "0"
            //Log.e("History", "Data Point Size: " + dataSet.dataPoints.size)
            for (dp in dataSet.dataPoints) {
                Log.e("History", "Data point:")
                Log.e("History", "\tType: " + dp.dataType.name)
                Log.e(
                    "History",
                    "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(
                        dp.getStartTime(TimeUnit.MILLISECONDS)
                    )
                )
                Log.e(
                    "History",
                    "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)) + " " + timeFormat.format(
                        dp.getStartTime(TimeUnit.MILLISECONDS)
                    )
                )

                dateList.add(dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)))

                for (field in dp.dataType.fields) {
                    val count = dp.getValue(field)
                    Log.e(
                            "History",
                    "\tField: " + field.name.toString() + " Value: " + count
                    )
                    stepCountList.add(dp.getValue(field).toString().toDouble())

                }
            }

            for (i in 0 until dateList.size) {
                dailyStep = (DailyStep(stepCountList[i].toDouble(), dateList[i]))
            }
            return dailyStep
        }

        private fun reverseTimer() {
            object : CountDownTimer((60 * 1000 + 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    var seconds = (millisUntilFinished / 1000).toInt()
                    val hours = seconds / (60 * 60)
                    val tempMint = seconds - hours * 60 * 60
                    val minutes = tempMint / 60
                    seconds = tempMint - minutes * 60
                }

                override fun onFinish() {
                    //context.showToast("Start Ok")
                    ViewTodayStepCountTask(
                        context,
                        mApiClient,
                        responseStepCount
                    ).execute()
                }
            }.start()
        }

    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        // Log.d("onConnectionFailed", p0.toString())
    }
}

class DailyStep (
    var stepCount: Double? = null,

    var date: String? = null
)

