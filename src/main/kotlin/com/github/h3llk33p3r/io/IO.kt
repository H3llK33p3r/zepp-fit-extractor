package com.github.h3llk33p3r.io

import com.garmin.fit.Sport
import com.github.h3llk33p3r.client.SportDetail
import com.github.h3llk33p3r.client.SportSummary
import java.time.Duration
import java.util.*
import kotlin.math.max


//Summary
/**
 * @param zeppType: The int used by zepp to identify the sport type !
 * @param supported: Flag in order to avoid fit generation when sport is not yet implemented
 * @param fitType The enum used for fit file for corresponding sport
 * @param gaitSupported If the sport can have gait information
 */
enum class ActivityType(val zeppType: Int, val supported: Boolean, val fitType: Sport, val gaitSupported: Boolean) {

    RUNNING(1, true, Sport.RUNNING, true),
    WALKING(6, true, Sport.WALKING, true),
    CYCLING(9, true, Sport.CYCLING, false),
    INDOOR_SWIMMING(14, false, Sport.SWIMMING, false),
    UNKNOWN(-1, false, Sport.GENERIC, false);

    companion object {
        fun fromZepp(type: Int): ActivityType {
            return entries.find { it.zeppType == type } ?: UNKNOWN
        }
    }

}


/**
 * Container of a sport activity in order to parse and handle raw data transmitted for Zepp WS.
 *
 * Creating this container parse data in order to retrieve time record for fit file generation
 * TODO : Complete doc !
 */
class SportContainer(val summary: SportSummary, val sportDetail: SportDetail) {

    fun Date.asUnixTimestamp(): Long = time / 1000

    val activityType: ActivityType = ActivityType.fromZepp(summary.type)

    val id: String = summary.trackid

    val startDate: Date = Date(sportDetail.trackid * 1000L)

    /**
     * Computed end date based on sport enddate and data relative time stored (HR time, step time, ...)
     */
    val endDate: Date

    /**
     * Total activity tume
     */
    val activityDuration: Duration
        get() = Duration.ofMillis(endDate.time - startDate.time)

    /**
     * Map of unixTimestamp to Acvitity data for the provided summary and detail
     */
    val timedData = ArrayList<ActivityData>()

    init {

        val zeppCoordinateFactor = 100000000.0
        //To initialize all data. We need to find the correct endDate based either on time accumulation or the endDate of the sport summary
        val maxEndTimeRecord: Long = startDate.asUnixTimestamp() + timeList.sum().toLong()
        endDate = Date(max(summary.end_time.toLong() * 1000, maxEndTimeRecord * 1000))

        //Collect metric and timed data for each metric !
        val times = timeList
        val latitudes = latitudeList
        val longitudes = longitudeList
        val altitudes = altList
        val timedHr = timedHeartRate(startDate.asUnixTimestamp(), endDate.asUnixTimestamp())
        val timeSpeed = timedSpeed(startDate.asUnixTimestamp(), endDate.asUnixTimestamp())
        val timeGait = timedGait(startDate.asUnixTimestamp(), endDate.asUnixTimestamp())
        //Now create timedData based on time values !

        var unixTs = startDate.asUnixTimestamp()
        var latitude: Long? = if (latitudes.isNotEmpty()) 0 else null
        var longitude: Long? = if (longitudes.isNotEmpty()) 0 else null

        times.forEachIndexed { index, i ->
            unixTs += i
            var latitudeInDeg: Double? = null
            var longitudeInDeg: Double? = null
            if (latitude != null) {
                latitude += latitudes[index]
                latitudeInDeg = latitude / zeppCoordinateFactor
            }
            if (longitude != null) {
                longitude += longitudes[index]
                longitudeInDeg = longitude / zeppCoordinateFactor
            }
            val gait: GaitContainer = timeGait[unixTs]!!

            timedData.add(
                ActivityData(
                    Date(unixTs * 1000),
                    latitudeInDeg,
                    longitudeInDeg,
                    altitudes.getOrNull(index),
                    timedHr[unixTs]!!,
                    timeSpeed[unixTs]!!,
                    gait.step,
                    gait.stride,
                    gait.stepFrequency
                )
            )
        }
    }


    /**
     * Return the time value as a list of int
     */
    private val timeList: List<Int>
        get() = sportDetail.time?.split(';')?.filter { it.isNotEmpty() }?.map { it.toInt() } ?: listOf()

    /**
     * Return the list of latitutes to link to a time value index
     */
    private val latitudeList: List<Long>
        get() = sportDetail.longitude_latitude?.split(';')?.filter { it.isNotEmpty() }?.map { it.split(',')[0].toLong() } ?: listOf()

    /**
     * Return the list of longitudes to link to a time value index
     */
    private val longitudeList: List<Long>
        get() = sportDetail.longitude_latitude?.split(';')?.filter { it.isNotEmpty() }?.map { it.split(',')[1].toLong() } ?: listOf()

    /**
     * List of altitudes in cm
     */
    private val altList: List<Long>
        get() {
            val noZeppValue = -2000000L
            val tmp: MutableList<Long> = sportDetail.altitude?.split(';')?.filter { it.isNotEmpty() }?.map { it.toLong() }?.toMutableList() ?: java.util.ArrayList()
            //Manage possible -20000 at the beginning and replace with the first real alt value
            val idx = tmp.lastIndexOf(noZeppValue)
            //Replace if possible and not out of bound
            if (idx != -1 && (idx + 1) < tmp.size) {
                for (i in 0..idx) {
                    tmp[i] = tmp[idx + 1]
                }
            }
            return tmp;
        }

    /**
     * Return a map of Heart Rate value from the start unix timestamp of the activity to the end filling with correct value.
     * @param from The unix timestamp from date to generate (in second)
     * @param to The unix timestamp to date to generate (in second)
     * @return A map of unix timestamp (in sec) with the heart values
     */
    private fun timedHeartRate(from: Long, to: Long): Map<Long, Short> {
        val elements = sportDetail.heart_rate?.split(';')?.filter { it.isNotEmpty() }?.map {
            var (timeDelta, hrDelta) = it.split(',');
            if (timeDelta.isEmpty()) {
                timeDelta = "1"
            }
            Pair(timeDelta.toLong(), hrDelta.toShort())
        } ?: listOf()
        return timedCumulative(from, to, elements, 0) { s1, s2 ->
            (s1 + s2).toShort()
        }
    }

    /**
     * Return a map of speed (m/s) value from the start unix timestamp of the activity to the end filling with correct value.
     * @param from The unix timestamp from date to generate (in second)
     * @param to The unix timestamp to date to generate (in second)
     * @return A map of unix timestamp (in sec) with the heart values
     */
    private fun timedSpeed(from: Long, to: Long): Map<Long, Float> {
        val elements = sportDetail.speed?.split(';')?.filter { it.isNotEmpty() }?.map { val (timeDelta, speed) = it.split(','); Pair(timeDelta.toLong(), speed.toFloat()) } ?: listOf()
        return timedFixed(from, to, elements, 0f)
    }


    /**
     * TODO //time difference(second),step number difference,stride (cm), step frequency(not necessarily available)
     */
    private fun timedGait(from: Long, to: Long): Map<Long, GaitContainer> {

        val pairOfStep = ArrayList<Pair<Long, Long>>()
        val pairOfStride = ArrayList<Pair<Long, Long>>()
        val pairOfStepFreq = ArrayList<Pair<Long, Long>>()

        sportDetail.gait?.split(';')?.filter { it.isNotEmpty() }?.forEach {
            val (timeDelta, stepNumber, stride, stepFrequency) = it.split(',');

            pairOfStep.add(Pair(timeDelta.toLong(), stepNumber.toLong()))
            pairOfStride.add(Pair(timeDelta.toLong(), stride.toLong()))
            pairOfStepFreq.add(Pair(timeDelta.toLong(), stepFrequency.toLong()))
        }

        val timedStep = timedCumulative(from, to, pairOfStep, 0L) { l1, l2 -> l1 + l2 }
        val timedStride = timedFixed(from, to, pairOfStride, 0L)
        val timedFreq = timedFixed(from, to, pairOfStepFreq, 0L)

        return timedStep.keys.associateWith { GaitContainer(timedStep[it]!!, timedStride[it]!!, timedFreq[it]!!) }

    }

    /**
     * Accumulate second part of the pair list
     */
    private fun <T : Number> timedCumulative(from: Long, to: Long, elements: List<Pair<Long, T>>, initValue: T, operator: (T, T) -> T): HashMap<Long, T> {
        val result = HashMap<Long, T>()
        var workingDate = from
        var value: T = initValue

        elements.forEachIndexed { index, pair ->
            value = operator(value, pair.second)
            val start = if (index == 0) 0 else 1
            for (i in start..pair.first) {
                result[workingDate++] = value
            }
        }
        //Fill to the end
        while (workingDate <= to) {
            result[workingDate++] = value
        }
        return result
    }

    /**
     * Use the raw value of the pair list to fill the map
     */
    private fun <T : Number> timedFixed(from: Long, to: Long, elements: List<Pair<Long, T>>, initialValue: T): HashMap<Long, T> {
        val result = HashMap<Long, T>()
        var workingDate = from
        var value: T = initialValue
        elements.forEachIndexed { index, pair ->
            value = pair.second
            val start = if (index == 0) 0 else 1
            for (i in start..pair.first) {
                result[workingDate++] = value
            }
        }
        //Fill to the end
        while (workingDate <= to) {
            result[workingDate++] = value
        }
        return result
    }

}

/**
 * @param step Number of step since the begging at this time
 * @param stride String in cm
 * @param stepFrequency Number of step per minute at this time
 */
class GaitContainer(val step: Long, val stride: Long, val stepFrequency: Long)


/**
 * Container of information about the sport session at the timestamp !
 *
 * @param timestamp  The date and time of this collection of metric
 * @param latitude The latitude of this point in degres.
 * @param longitude The longitude of this point in degres.
 * @param altitude The altitude of this point in cm
 * @param heartRate  The current heart rate at this point
 * @param speed  The instant speed at this point in m/s
 * @param step The number of step from the begning of this sport session
 * @param stride The stride (step length) at this point in cm
 * @param stepFrequency The step frequencen per minute at this point
 *
 */
class ActivityData(
    val timestamp: Date, val latitude: Double?, val longitude: Double?, val altitude: Long?,
    val heartRate: Short?, val speed: Float?, val step: Long?, val stride: Long?, val stepFrequency: Long?
) {

}
