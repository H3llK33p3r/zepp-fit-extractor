package com.github.h3llk33p3r

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

class Utils {

  companion object {
    @JvmField
    val MAPPER: ObjectMapper = ObjectMapper().registerKotlinModule()
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

  }
}
