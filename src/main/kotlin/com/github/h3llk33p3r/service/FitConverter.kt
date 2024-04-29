package com.github.h3llk33p3r.service

import com.garmin.fit.Activity
import com.garmin.fit.ActivityMesg
import com.garmin.fit.DateTime
import com.garmin.fit.DeviceIndex
import com.garmin.fit.DeviceInfoMesg
import com.garmin.fit.Event
import com.garmin.fit.EventMesg
import com.garmin.fit.EventType
import com.garmin.fit.FileEncoder
import com.garmin.fit.FileIdMesg
import com.garmin.fit.Fit
import com.garmin.fit.FitRuntimeException
import com.garmin.fit.LapMesg
import com.garmin.fit.Manufacturer
import com.garmin.fit.Mesg
import com.garmin.fit.RecordMesg
import com.garmin.fit.SessionMesg
import com.garmin.fit.Sport
import com.garmin.fit.util.SemicirclesConverter.degreesToSemicircles
import com.github.h3llk33p3r.client.SportDetail
import com.github.h3llk33p3r.client.SportSummary
import com.github.h3llk33p3r.io.ActivityType
import com.github.h3llk33p3r.io.ActivityType.UNKNOWN
import com.github.h3llk33p3r.io.SportContainer
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

/**
 * Fit converter service to create fit file.
 */
class FitConverter {

    private val log = LoggerFactory.getLogger(FitConverter::class.java)

    /**
     * With provided raw input files, create a fit activity file
     *
     * @param outputDirectory The directory to write result file into
     * @param summary The sport summary data as returned by the webservice
     * @param detail The sport detail
     * @return The file in which the activity has been generated or null if we cannot support it for now
     */
    fun convertToFit(outputDirectory: File, summary: SportSummary, detail: SportDetail): File? {
        val container = SportContainer(summary, detail)
        log.info("Generating fit file for activity [{}]", container.id)
        if (container.activityType.supported) {

            val messages: MutableList<Mesg> = ArrayList()
            //Mandatory field
            messages.add(generateFileId(container.startDate))
            messages.add(generateDeviceInfo(container.startDate))
            messages.add(generateActivityMessage(container))
            //Manage data of activity
            messages.addAll(fillActivity(container))
            val outputFile = File(outputDirectory, "${container.activityType.name}-${container.id}.fit")
            writeFitFile(outputFile, messages)
            return outputFile
        } else {
            log.error("Unsupported activity type [{}] skipping trackId [{}]", container.activityType, container.id)
        }
        return null

    }

    /**
     * Manage the event messages.
     * At least the start and end event are generated
     */
    private fun fillActivity(sport: SportContainer): Collection<Mesg> {

        val messages: MutableList<Mesg> = ArrayList()
        //First we declare the  start of the activity
        val start = EventMesg()
        start.timestamp = DateTime(sport.startDate)
        start.event = Event.TIMER
        start.eventType = EventType.START
        messages.add(start)

        //Now we must declare all records !
        when (sport.activityType) {
            ActivityType.RUNNING -> fillRunningRecord(sport, messages)
            ActivityType.`INDOOR_SWIMMING` -> TODO()
            ActivityType.WALKING -> TODO()
            ActivityType.CYCLING -> TODO()
            UNKNOWN -> TODO()
        }

        //the end of the activity
        val end = EventMesg()
        end.timestamp = DateTime(sport.endDate)
        end.event = Event.TIMER
        end.eventType = EventType.STOP_ALL
        messages.add(end)
        return messages
    }

    /**
     * Method to manage a running workout
     */
    private fun fillRunningRecord(sport: SportContainer, messages: MutableList<Mesg>) {

        for (data in sport.timedData) {
            //Compute new data point
            val recordMesg = RecordMesg()
            recordMesg.timestamp = DateTime(data.timestamp)
            recordMesg.heartRate = data.heartRate
            //RPM => Revolution per minute. We must divide per 2 the number of step.
            data.stepFrequency?.let { recordMesg.cadence = (data.stepFrequency / 2).toShort() }

            recordMesg.speed = data.speed
            //Convert as mention in the rest doc !
            data.altitude?.let { recordMesg.altitude = (data.altitude / 100).toFloat() }
            data.latitude?.let { recordMesg.positionLat = degreesToSemicircles(data.latitude) }
            data.longitude?.let { recordMesg.positionLong = degreesToSemicircles(data.longitude) }
            data.stride?.let { recordMesg.stepLength = (data.stride * 10).toFloat() }

            messages.add(recordMesg)

        }

        //FIXME: manage if multiple lap !
        val lap = LapMesg()
        lap.timestamp = DateTime(sport.startDate)
        lap.totalElapsedTime = sport.activityDuration.toSeconds().toFloat()
        lap.totalDistance = sport.summary.dis!!.toFloat()
        lap.totalAscent = sport.summary.altitude_ascend
        lap.totalDescent = sport.summary.altitude_descend
        messages.add(lap)

        val session = sessionMesg(sport)
        session.sport = Sport.RUNNING
        messages.add(session)

    }

    private fun sessionMesg(sport: SportContainer): SessionMesg {
        val session = SessionMesg()
        session.timestamp = DateTime(sport.endDate)
        session.startTime = DateTime(sport.startDate)
        session.totalElapsedTime = sport.activityDuration.toSeconds().toFloat()
        session.totalAscent = sport.summary.altitude_ascend
        session.totalDescent = sport.summary.altitude_descend
        sport.summary.dis?.let { session.totalDistance = sport.summary.dis!!.toFloat() }
        sport.summary.calorie?.let { session.totalCalories = sport.summary.calorie!!.toFloat().toInt() }
        sport.summary.avg_heart_rate?.let { session.avgHeartRate = sport.summary.avg_heart_rate!!.toFloat().toInt().toShort() }
        sport.summary.min_heart_rate?.let { session.minHeartRate = sport.summary.min_heart_rate!!.toShort() }
        sport.summary.max_heart_rate?.let { session.maxHeartRate = sport.summary.max_heart_rate!!.toShort() }
        sport.summary.avg_stride_length?.let { session.avgStepLength = sport.summary.avg_stride_length!!.toFloat() * 10 }
        //Not setting avg cadence => Garmin connect compute it !
        return session
    }


    private fun writeFitFile(outputFile: File, messages: List<Mesg>) {
        val encode: FileEncoder
        try {
            encode = FileEncoder(outputFile, Fit.ProtocolVersion.V2_0)
        } catch (e: FitRuntimeException) {
            log.error("Error opening file [{}]", outputFile, e)
            return
        }
        messages.forEach { encode.write(it) }
        // Close the output stream
        try {
            encode.close()
        } catch (e: FitRuntimeException) {
            log.error("Error closing encode for file [{}]", outputFile, e)
            return
        }
        log.info("Fit file created [{}]", outputFile.name)
    }

    private fun generateActivityMessage(summary: SportContainer): ActivityMesg {
        val msg = ActivityMesg()
        //Field set with zepp application fit export
        //FIXME : Must be on Z timezone ?
        msg.timestamp = DateTime(summary.startDate)
        msg.totalTimerTime = summary.activityDuration.toSeconds().toFloat()
        //FIXME: Alway 1 session event if multisport ?
        msg.numSessions = 1
        msg.type = Activity.MANUAL
        msg.localTimestamp = DateTime(summary.startDate).timestamp
        return msg
    }

    /**
     * Every FIT file MUST contain a File ID message
     *
     * @return a File id message
     */
    private fun generateFileId(timestamp: Date): Mesg {
        val fileIdMesg = FileIdMesg()
        fileIdMesg.type = com.garmin.fit.File.ACTIVITY
        fileIdMesg.manufacturer = Manufacturer.DEVELOPMENT
        fileIdMesg.product = FIT_CONVERTER_PRODUCT
        fileIdMesg.timeCreated = DateTime(timestamp)
        fileIdMesg.serialNumber = FIT_CONVERTER_SN
        return fileIdMesg
    }

    private fun generateDeviceInfo(timestamp: Date): Mesg {
        // A Device Info message is a BEST PRACTICE for FIT ACTIVITY files
        val deviceInfoMesg = DeviceInfoMesg()
        deviceInfoMesg.deviceIndex = DeviceIndex.CREATOR
        deviceInfoMesg.manufacturer = Manufacturer.DEVELOPMENT
        deviceInfoMesg.product = FIT_CONVERTER_PRODUCT
        deviceInfoMesg.productName = PRODUCT_NAME
        deviceInfoMesg.serialNumber = FIT_CONVERTER_SN
        deviceInfoMesg.softwareVersion = SOFTWARE_VERSION
        deviceInfoMesg.timestamp = DateTime(timestamp)
        return deviceInfoMesg
    }


    companion object {
        // Max 20 Chars!  Amazfit Stratos 3?
        private const val PRODUCT_NAME = "zepp-fit-extractor"
        private const val FIT_CONVERTER_PRODUCT = 1
        private const val FIT_CONVERTER_SN: Long = 9131870
        private const val SOFTWARE_VERSION = 1.0f
    }
}
