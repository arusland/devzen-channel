package io.arusland.devzen.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule

fun Any.toJson(pretty: Boolean = true): String {
    return if (pretty) mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this)
    else mapper.writeValueAsString(this)
}

private val mapper = ObjectMapper().registerModule(KotlinModule())
