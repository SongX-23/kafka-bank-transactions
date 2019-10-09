package com.digio.kafka.transactions.serdes

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Serializer
import org.slf4j.LoggerFactory

class JsonSerializer<T> : Serializer<T> {
  companion object {
    private val logger = LoggerFactory.getLogger(JsonDeserializer::class.java)
  }

  private val objectMapper = ObjectMapper()

  override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {
  }

  override fun serialize(topic: String, o: T): ByteArray? {
    var retVal: ByteArray? = null

    try {
      retVal = objectMapper.writeValueAsBytes(o)
    } catch (e: JsonProcessingException) {
      logger.error(e.toString())
    }

    return retVal
  }

  override fun close() {}
}
