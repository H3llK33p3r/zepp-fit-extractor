package com.github.h3llk33p3r.command

import com.fasterxml.jackson.module.kotlin.readValue
import com.github.h3llk33p3r.Utils.Companion.MAPPER
import com.github.h3llk33p3r.Utils.Companion.SUMMARIES_FILENAME
import com.github.h3llk33p3r.Utils.Companion.ZEPP_BASE_URL
import com.github.h3llk33p3r.client.SportDetail
import com.github.h3llk33p3r.client.SportSummary
import com.github.h3llk33p3r.client.ZeppRestClient
import com.github.h3llk33p3r.io.ActivityType
import com.github.h3llk33p3r.service.FitConverter
import org.slf4j.LoggerFactory
import org.springframework.shell.standard.EnumValueProvider
import org.springframework.shell.standard.FileValueProvider
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod
import org.springframework.shell.standard.ShellOption
import java.io.File

@ShellComponent("Zepp Fix Extractor")
class ExporterCommands {

    private val converter = FitConverter()
    private val logger = LoggerFactory.getLogger(ExporterCommands::class.java)

    @ShellMethod(key = ["download-all"], value = "Download all sport activities from remote web services")
    fun downloadAll(
        @ShellOption(
            value = ["-t", "--token"],
            help = "The token to use to be able to download all resources."
        ) token: String,

        @ShellOption(
            value = ["-o", "--output"],
            help = "The output directory to store the data.",
            valueProvider = FileValueProvider::class
        ) outputDirectory: File,
    ) {

        checkAndCreateOutDirectory(outputDirectory)
        logger.info("Starting download")
        val client = ZeppRestClient(ZEPP_BASE_URL, token)
        val summaries = client.summaries
        //Write to file
        MAPPER.writeValue(File(outputDirectory, SUMMARIES_FILENAME), summaries)
        summaries.forEach {
            val detail = client.getDetail(it)
            MAPPER.writeValue(File(outputDirectory, "${it.trackid}.json"), detail)
        }

        logger.info("All resources have been downloaded")
    }

    private fun checkAndCreateOutDirectory(outputDirectory: File) {
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw RuntimeException("Unable to create the output directory [${outputDirectory.absolutePath}]")
            }
        } else if (!outputDirectory.isDirectory) {
            throw RuntimeException("The targeted output is not a directory [${outputDirectory.absolutePath}]")
        }
    }

    @ShellMethod(key = ["generate-all"], value = "Generate all activities .fit file using previous downloaded resources.")
    fun downloadAll(
        @ShellOption(
            value = ["-i", "--input-dir"],
            help = "The input directory containing json downloaded with download-all command",
            valueProvider = FileValueProvider::class
        ) inputDir: File,
        @ShellOption(
            value = ["-o", "--output"],
            help = "The output directory to store .fit files.",
            valueProvider = FileValueProvider::class
        ) outputDirectory: File
    ) {

        val summariesFile = File(inputDir, SUMMARIES_FILENAME)
        if (!summariesFile.exists()) {
            throw RuntimeException("The target input directory does not contains the summaries file. Please check your command -i option.")
        }
        checkAndCreateOutDirectory(outputDirectory)
        val summaries = MAPPER.readValue<List<SportSummary>>(summariesFile)
        logger.info("There is ${summaries.size} activity loaded from summaries file")
        generateFor(summaries, inputDir, outputDirectory)

    }

    private fun generateFor(
        summaries: List<SportSummary>,
        inputDir: File,
        outputDirectory: File
    ) {
        summaries.forEach { sumary ->
            val detailFile = File(inputDir, "${sumary.trackid}.json")
            val detail = MAPPER.readValue<SportDetail>(detailFile)
            val result = converter.convertToFit(outputDirectory, sumary, detail)
            result?.let { logger.info("Activity {} has been saved into {}", detail.trackid, it.absoluteFile) }
        }
    }

    @ShellMethod(key = ["generate-single"], value = "Generate a single activity based on the trackId")
    fun generateSingle(
        @ShellOption(
            value = ["-i", "--input-dir"],
            help = "The input directory containing json downloaded with download-all command",
            valueProvider = FileValueProvider::class
        ) inputDir: File,
        @ShellOption(
            value = ["-o", "--output"],
            help = "The output directory to store .fit files.",
            valueProvider = FileValueProvider::class
        ) outputDirectory: File,
        @ShellOption(
            value = ["--id"],
            help = "The identifier of the activity."
        ) trackId: String
    ) {

        val summariesFile = File(inputDir, SUMMARIES_FILENAME)
        if (!summariesFile.exists()) {
            throw RuntimeException("The target input directory does not contains the summaries file. Please check your command -i option.")
        }
        checkAndCreateOutDirectory(outputDirectory)
        val summaries = MAPPER.readValue<List<SportSummary>>(summariesFile)
        val summary = summaries.find { it.trackid == trackId }
        if (summary != null) {
            generateFor(listOf(summary), inputDir, outputDirectory)
        }
    }

    @ShellMethod(key = ["generate-sport"], value = "Generate a activities for a provided sport type")
    fun generateForSport(
        @ShellOption(
            value = ["-i", "--input-dir"],
            help = "The input directory containing json downloaded with download-all command",
            valueProvider = FileValueProvider::class
        ) inputDir: File,
        @ShellOption(
            value = ["-o", "--output"],
            help = "The output directory to store .fit files.",
            valueProvider = FileValueProvider::class
        ) outputDirectory: File,
        @ShellOption(
            value = ["-s", "--sport"],
            help = "The sport to generate the activities.",
            valueProvider = EnumValueProvider::class
        ) sportType: ActivityType
    ) {

        val summariesFile = File(inputDir, SUMMARIES_FILENAME)
        if (!summariesFile.exists()) {
            throw RuntimeException("The target input directory does not contains the summaries file. Please check your command -i option.")
        }
        checkAndCreateOutDirectory(outputDirectory)
        val summaries = MAPPER.readValue<List<SportSummary>>(summariesFile)
        val filtered = summaries.filter { ActivityType.fromZepp(it.type) == sportType }.toList()
        generateFor(filtered, inputDir, outputDirectory)
    }

}
