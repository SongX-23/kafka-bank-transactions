package com.digio.kafka.transactions.serdes

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory
import java.io.IOException

class JsonDeserializer<T>(private val type: Class<T>) : Deserializer<T> {
  companion object {
    private val logger = LoggerFactory.getLogger(JsonDeserializer::class.java)
  }
  private val mapper = ObjectMapper()

  override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {
  }

  override fun deserialize(s: String, bytes: ByteArray): T? {

    try {
      return mapper.readValue(bytes, type)
    } catch (e: IOException) {
      logger.warn("Could not deserialise for type {}", this.type.javaClass.name)
      e.printStackTrace()
    }

    return null
  }

  override fun close() {}
}
