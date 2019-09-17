package com.digio.kafka.transactions

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ser.std.StringSerializer
import org.apache.kafka.clients.producer.ProducerConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Properties
import org.apache.kafka.clients.producer.KafkaProducer

class TransactionProducer {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(TransactionProducer::class.java.name)
    }

    fun main(args: Array<String>) {
        val legacyBankingSystem = LegacyBankingSystem()
        val objectMapper = ObjectMapper()
        val properties = Properties().apply {
            this[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = "127.0.0.1:9092"
            this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
            this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name

            // producer acks
            this[ProducerConfig.ACKS_CONFIG] = "all"
            this[ProducerConfig.RETRIES_CONFIG] = "3"
            this[ProducerConfig.LINGER_MS_CONFIG] = "1"

            // leverage idempotent producer from Kafka 0.11 !
            this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true" // ensure we don't push duplicates
        }

        val producer = KafkaProducer<String, String>(properties)

        // Do the thing.

        // Close to producer gracefully
        Runtime.getRuntime().addShutdownHook(Thread(producer::close))
    }
}