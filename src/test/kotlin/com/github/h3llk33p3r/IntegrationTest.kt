package com.github.h3llk33p3r

import com.garmin.fit.Decode
import com.garmin.fit.FitDecoder
import com.garmin.fit.plugins.ActivityFileValidationPlugin
import com.garmin.fit.plugins.ActivityFileValidationResult
import com.github.h3llk33p3r.client.SportDetail
import com.github.h3llk33p3r.client.SportSummary
import com.github.h3llk33p3r.service.FitConverter
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.*


class IntegrationTest {
    private val converter = FitConverter()

    val logger = org.slf4j.LoggerFactory.getLogger(IntegrationTest::class.java)

    private fun checkFitGenerated(fitFile: File) {
        val activityValidation = ActivityFileValidationPlugin()
        val decode = Decode()
        val fitDecoder = FitDecoder()


        //Check integrity
        fitFile.inputStream().use {
            if (!decode.checkFileIntegrity(it)) {
                throw RuntimeException("FIT file [${fitFile.absoluteFile}] integrity failed.")
            }
        }

        //Check content
        fitFile.inputStream().use { inputStream ->
            fitDecoder.decode(inputStream, activityValidation)
            val results = activityValidation.results
            val warnChecks = results.stream().filter { it.status == ActivityFileValidationResult.Status.WARNING }.toList()
            val failedCheck = results.stream().filter { it.status == ActivityFileValidationResult.Status.FAILED }.toList()
            logger.info("Results for fit file ${fitFile.absoluteFile}")
            if (warnChecks.isNotEmpty()) {
                warnChecks.forEach { logger.warn(it.toString()) }
            }
            //TODO : Assert here !
            if (failedCheck.isNotEmpty()) {
                failedCheck.forEach { logger.error(it.toString()) }
            }
            assertTrue(failedCheck.isEmpty(), "No error should be present in the generated fit file")
        }
    }

    @Test
    @Throws(Exception::class)
    fun testFitForRunning() {
        val output = File("target")
        val detail = Utils.MAPPER.readValue(File("src/test/resources/json/run_1698569116_detail.json"), SportDetail::class.java)
        val summary = Utils.MAPPER.readValue(File("src/test/resources/json/run_1698569116_summary.json"), SportSummary::class.java)
        assertNotNull(detail)
        assertNotNull(summary)
        val outputfit = converter.convertToFit(output, summary, detail)
        assertNotNull(outputfit)
        checkFitGenerated(outputfit!!)
    }

    @Test
    @Throws(Exception::class)
    fun testFitForWalking() {
        val output = File("target")
        val detail = Utils.MAPPER.readValue(File("src/test/resources/json/walk_1542558074_detail.json"), SportDetail::class.java)
        val summary = Utils.MAPPER.readValue(File("src/test/resources/json/walk_1542558074_summary.json"), SportSummary::class.java)
        assertNotNull(detail)
        assertNotNull(summary)
        val outputfit = converter.convertToFit(output, summary, detail)
        assertNotNull(outputfit)
        checkFitGenerated(outputfit!!)
    }

    @Test
    @Throws(Exception::class)
    fun testFitForSwimming() {
        val output = File("target")
        val detail = Utils.MAPPER.readValue(File("src/test/resources/json/swimming_1654683111_detail.json"), SportDetail::class.java)
        val summary = Utils.MAPPER.readValue(File("src/test/resources/json/swimming_1654683111_summary.json"), SportSummary::class.java)
        assertNotNull(detail)
        assertNotNull(summary)
        val outputfit = converter.convertToFit(output, summary, detail)
        assertNotNull(outputfit)
        checkFitGenerated(outputfit!!)
    }
}
