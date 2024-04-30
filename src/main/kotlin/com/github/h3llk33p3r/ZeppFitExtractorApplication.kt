package com.github.h3llk33p3r

import com.github.h3llk33p3r.command.ExporterCommands
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.shell.command.annotation.CommandScan
import org.springframework.shell.command.annotation.EnableCommand


@SpringBootApplication
class ZeppFitExtractorApplication

fun main(args: Array<String>) {
    runApplication<ZeppFitExtractorApplication>(*args)
}

