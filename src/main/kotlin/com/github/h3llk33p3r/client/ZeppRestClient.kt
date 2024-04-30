package com.github.h3llk33p3r.client

import com.github.h3llk33p3r.Utils
import com.github.h3llk33p3r.io.ActivityType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import java.io.IOException


class ZeppRestClient(private val baseUrl: String, private val token: String) {

    private val log = LoggerFactory.getLogger(ZeppRestClient.javaClass)

    //Create the client
    private val client: OkHttpClient = OkHttpClient.Builder().addInterceptor(Interceptor { chain: Interceptor.Chain ->
        val originalReq = chain.request()
        val newRequest = originalReq.newBuilder()
            .header(HEADER_APPTOKEN, token)
            .header(HEADER_APPPLATFORM, APP_PLATFORM)
            .header(HEADER_APPNAME, APP_NAME)
            .method(originalReq.method, originalReq.body)
            .build()
        chain.proceed(newRequest)
    }).build()

    val summaries: List<SportSummary>
        get() {
            log.info("Getting activity history from server [{}]", baseUrl)
            val resultList: MutableList<SportSummary> = ArrayList()
            getHistory(resultList, null)

            val counted = resultList.groupBy { it.type }.mapKeys {
                val newKey = ActivityType.fromZepp(it.key);
                if (newKey == ActivityType.UNKNOWN) "UNKNOWN-${it.key}" else newKey.name
            }.mapValues { it.value.size }

            //For a detailed output !
            log.info("All history has been retrieved [{} activities]. Here are the details {}", resultList.size, counted)

            return resultList
        }

    private fun getHistory(resultList: MutableList<SportSummary>, fromTrackId: Int?) {

        val urlBuilder = (baseUrl + WORKOUT_HISTORY_PATH).toHttpUrlOrNull()!!.newBuilder()
        if (fromTrackId != null) {
            urlBuilder.addQueryParameter(PARAM_TRACK_ID, fromTrackId.toString())
        }
        val request: Request = Request.Builder().url(urlBuilder.build()).build()
        val call = client.newCall(request)
        try {
            call.execute().use { response ->
                if (response.isSuccessful) {
                    val result = Utils.MAPPER.readValue(
                        response.body!!.bytes(), SportSummaryResponse::class.java
                    )
                    log.info("Workout history page retrieved with [{}] elements", result.data.summary.size)
                    resultList.addAll(result.data.summary)
                    if (result.data.next != -1) {
                        log.info("There is more workout to get. Getting now from [{}]", result.data.next)
                        getHistory(resultList, result.data.next)
                    }
                } else {
                    throw RuntimeException("Unable to get the workout history [fromTrackId=$fromTrackId]")
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }


    fun getDetail(summary: SportSummary): SportDetail {
        val urlBuilder = (baseUrl + WORKOUT_DETAILS_PATH).toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter(PARAM_TRACK_ID, summary.trackid)
            .addQueryParameter(PARAM_SOURCE, summary.source)

        val request: Request = Request.Builder().url(urlBuilder.build()).build()
        val call = client.newCall(request)

        log.info("Getting activity details for trackId {}", summary.trackid)
        try {
            call.execute().use {
                if (it.isSuccessful) {
                    val webResult: SportDetailResponse =
                        Utils.MAPPER.readValue(it.body!!.bytes(), SportDetailResponse::class.java)
                    return webResult.data
                } else {
                    throw RuntimeException("Unable to get the details [trackId=${summary.trackid}]")
                }
            }
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    companion object {
        const val WORKOUT_HISTORY_PATH: String = "/v1/sport/run/history.json"
        const val WORKOUT_DETAILS_PATH: String = "/v1/sport/run/detail.json"

        const val PARAM_TRACK_ID: String = "trackid"
        const val PARAM_SOURCE: String = "source"

        const val HEADER_APPTOKEN: String = "apptoken"
        const val HEADER_APPPLATFORM: String = "appPlatform"
        const val HEADER_APPNAME: String = "appname"

        private const val APP_NAME = "com.xiaomi.hm.health"
        private const val APP_PLATFORM = "web"
    }
}


/**
 * A page of web result when getting all activities for a user
 */
class SportSummaryPage {
    var next: Int = 1
    var summary: List<SportSummary> = java.util.ArrayList()
}

/**
 * The web response getting all activities of a user
 */
class SportSummaryResponse {
    var code = 0
    lateinit var message: String
    lateinit var data: SportSummaryPage
}

class SportSummary {

    /**
     * trackId : Is a workout identifier the value is equal to the activity start time at Z timezone
     */
    lateinit var trackid: String

    /**
     * Source of the information (technical data not related to user or sport)
     */
    var source: String? = null

    /**
     * Distance in m
     */
    var dis: String? = null

    /**
     * Qty of calorie burn
     */
    var calorie: String? = null

    /**
     * end time timestamp
     */
    lateinit var end_time: String

    /**
     * Duration of the activity
     */
    var run_time: String? = null

    /**
     * Avg pace (ie 0.3285151)
     */
    var avg_pace: String? = null

    /**
     * Step frequency ? (ie 170.0)
     */
    var avg_frequency: String? = null

    /**
     * (ie 163.0)
     */
    var avg_heart_rate: String? = null
    var type = 0
    var location: String? = null
    var city: String? = null
    var forefoot_ratio: String? = null
    var bind_device: String? = null
    var max_pace: Float? = null
    var min_pace: Float? = null
    var version = 0
    var altitude_ascend: Int? = null
    var altitude_descend: Int? = null
    var total_step: Int? = null
    var avg_stride_length: Int? = null
    var max_frequency: Int? = null
    var max_altitude: Int? = null
    var min_altitude: Int? = null
    var lap_distance: Int? = null
    var sync_to: String? = null
    var distance_ascend: Int? = null
    var max_cadence: Int? = null
    var avg_cadence: Int? = null
    var landing_time: Int? = null
    var flight_ratio: Int? = null
    var climb_dis_descend: Int? = null
    var climb_dis_ascend_time: Int? = null
    var climb_dis_descend_time: Int? = null
    var child_list: String? = null
    var parent_trackid: Int? = null
    var max_heart_rate: Int? = null
    var min_heart_rate: Int? = null
    var swolf: Int? = null
    var total_strokes: Int? = null
    var total_trips: Int? = null
    var avg_stroke_speed: Float? = null
    var max_stroke_speed: Float? = null
    var avg_distance_per_stroke: Float? = null
    var swim_pool_length: Int? = null
    var te: Int? = null
    var swim_style: Int? = null
    var unit: Int? = null
    var add_info: String? = null
    var sport_mode: Int? = null
    var downhill_num: Int? = null
    var downhill_max_altitude_desend: Int? = null
    var strokes: Int? = null
    var fore_hand: Int? = null
    var back_hand: Int? = null
    var serve: Int? = null
    var second_half_start_time: Int? = null
    var pb: String? = null
    var rope_skipping_count: Int? = null
    var rope_skipping_avg_frequency: Int? = null
    var rope_skipping_max_frequency: Int? = null
    var rope_skipping_rest_time: Int? = null
    var left_landing_time: Int? = null
    var left_flight_ratio: Int? = null
    var right_landing_time: Int? = null
    var right_flight_ratio: Int? = null
    var marathon: String? = null
    var situps: Int? = null
    var anaerobic_te: Int? = null
    var target_type: Int? = null
    var target_value: String? = null
    var total_group: Int? = null
    var spo2_max: Int? = null
    var spo2_min: Int? = null
    var avg_altitude: Float? = null
    var max_slope: Int? = null
    var avg_slope: Int? = null
    var avg_pulloar_time: Float? = null
    var avg_return_time: Float? = null
    var floor_number: Int? = null
    var upstairs_height: Float? = null
    var min_upstairs_floors: Float? = null
    var accumulated_gap: Int? = null
    var auto_recognition: Boolean? = null
    var app_name: String? = null
    var pause_time: String? = null
    var heartrate_setting_type: Int? = null

}

// Details
class SportDetailResponse {
    var code = 0
    var message: String? = null
    lateinit var data: SportDetail
}

class SportDetail {
    /**
     * The Workout identifier (start timestamp)
     */
    var trackid = 0

    /**
     * Source of the information (technical data not related to user or sport)
     */
    var source: String? = null

    /**
     * latitude and longitude list
     * format: {latitude interpolation,longitude interpolation;},
     *
     * each value of the original latitude and longitude * 100000000 to do the interpolation.
     * that is to keep the decimal point after 8,
     * the first latitude/longitude is interpolation with 0,
     * for aligning the data: the missing point with the empty string occupies the position.
     *
     * e.g. 4004663552,11629333504;16403,8392;;;;14877,8392;
     */
    var longitude_latitude: String? = null

    /**
     * altitude list, format: {altitude;altitude;...},
     * none-interpolation,
     * unit centimeter,
     * default value after 2016-08 is -2000000,
     * before 2016-08 is 0.
     * e.g. 7800;7772;7763;7763;-2000000
     */
    var altitude: String? = null

    /**
     * gps accuracy list, unit meter, e.g. 0;1;1;1;1
     */
    var accuracy: String? = null

    /**
     * time list , format {nbSec;nbSec;nbSec;...]},
     * Interpolation : time difference(second)
     */
    var time: String? = null

    /**
     *
     * gait list, format:
     * {time difference(second),step number difference,stride (cm), step frequency(not necessarily available);…},
     *
     * e.g. 2,0,71,0 ; 2,0,0,0  ; 147,1,0,0;
     */
    var gait: String? = null

    /**
     * pace list, format: {pace;…},
     * unit second/meter,
     * keep 2 decimal places,
     *
     * e.g. 0.33;0.33;0.25;0.26;
     */
    var pace: String? = null

    /**
     * pause list, format: {pause start time, pause end time interpolation, pause start index, pause stop index, pause type;},
     *
     * pause type: 2 manual, 3 auto,
     *
     * e.g. 1439383257,555,100,123,2;…
     */
    var pause: String? = null
    var spo2: String? = null
    var flag: String? = null
    var kilo_pace: String? = null
    var mile_pace: String? = null

    /**
     * heart Rate list
     *
     * format: {time interpolation(second), heart rate interpolation},
     *
     * the first time is interpolation with startTime, if time interpolation is 1, there is an empty string,
     *
     * e.g. 11,80;0,10;7,-6;4,1;,-1;
     */
    var heart_rate: String? = null
    var version = 0
    var provider: String? = null

    /**
     * speed list,
     * format: {time interpolation(second),speed(meter/second)},
     *
     * e.g. 2,1.20;4,2.45;3,3.5
     */
    var speed: String? = null
    var bearing: String? = null

    /**
     * run distance list,
     * format: {time interpolation(second),distance interpolation(meter);…},
     *
     * e.g. 2,0;10,48;2,27;2,5;
     */
    var distance: String? = null
    var lap: String? = null
    var air_pressure_altitude: String? = null
    var course: String? = null
    var correct_altitude: String? = null
    var stroke_speed: String? = null
    var cadence: String? = null
    var daily_performance_info: String? = null
    var rope_skipping_frequency: String? = null
    var weather_info: String? = null
    var coaching_segment: String? = null
    var golf_swing_rt_data: String? = null
    var power_meter: String? = null
}
