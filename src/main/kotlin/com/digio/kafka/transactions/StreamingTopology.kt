package com.digio.kafka.transactions

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Properties
import com.digio.kafka.transactions.serdes.TransactionSerde
import org.apache.kafka.streams.StreamsConfig
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.GlobalKTable
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Windowed
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.kstream.TimeWindows
import java.util.concurrent.TimeUnit


class StreamingTopology {
  companion object {
    private val logger: Logger = LoggerFactory.getLogger(TransactionProducer::class.java.name)
    fun config(): Properties {
      return Properties().apply {
        this[StreamsConfig.APPLICATION_ID_CONFIG] = "bank-starter-app"
        this[StreamsConfig.BOOTSTRAP_SERVERS_CONFIG] = "localhost:9092"
        this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        this[StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG] = "0"
        this[StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG] = Serdes.String()::class.java.name
        this[StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG] = TransactionSerde::class.java.name
      }
    }

    fun createStream(builder: StreamsBuilder): KStream<String, Transaction>? {
      return builder.stream("transaction-topic", Consumed.with(Serdes.StringSerde(), TransactionSerde()))
    }

    fun computeTotals(kStream: KStream<String, Transaction>?): KStream<String, Long>? {
      return kStream?.groupByKey()
          ?.aggregate(
              { 0L },
              { _, value, aggregate ->
                aggregate + value.amount
              },
              Materialized.with(Serdes.StringSerde(), Serdes.LongSerde())
          )
          ?.toStream()
    }

    fun computeRunningTotal(kStream: KStream<String, Transaction>?): KStream<Windowed<String>, Long>? {
      val windowSizeMs = TimeUnit.SECONDS.toMillis(30)
      val weeklySpendWindow = TimeWindows.of(windowSizeMs)
      return kStream?.groupByKey()
          ?.windowedBy(weeklySpendWindow)
          ?.aggregate(
              { 0L },
              { _, value, aggregate ->
                aggregate + value.amount
              },
              Materialized.with(Serdes.StringSerde(), Serdes.LongSerde())
          )
          ?.toStream()
    }

    fun createCategoryLookupTable(builder: StreamsBuilder): GlobalKTable<String, String> {
      return builder.globalTable("category-topic",
          Consumed.with(Serdes.StringSerde(), Serdes.StringSerde()),
          Materialized.`as`("category-lookup-store"))
    }

    fun categorisedStream(kStream: KStream<String, Transaction>, kTable: GlobalKTable<String, String>)
        : KStream<String, Transaction> {
      return kStream.leftJoin<String, String, Transaction>(kTable,
          { _: String, transaction: Transaction ->
            transaction.category
          },
          { transaction: Transaction, category: String ->
            transaction.category = category
            transaction
          })
      //return kStream?.leftJoin(kTable, this::keyValueMapper, this::valueJoiner)
    }

    fun topology(builder: StreamsBuilder) {
      val stringLongProduced = Produced.with(Serdes.StringSerde(), Serdes.LongSerde())
      val stream = createStream(builder)
      val totalStream = computeTotals(stream)
      val windowedLongKStream = computeRunningTotal(stream)
      val enhancedTransactions = categorisedStream(stream!!, createCategoryLookupTable(builder))

      totalStream?.to("customer-total-topic", stringLongProduced)
      windowedLongKStream?.selectKey { key, _ -> key.toString() }
          ?.to("customer-rolling-total-topic", stringLongProduced)
      enhancedTransactions
          .peek { _, value -> logger.info(value.toString()) }
          .to("enhanced-transactions-topic",
              Produced.with<String, Transaction>(Serdes.StringSerde(), TransactionSerde()))
    }
  }

  fun main(args: Array<String>) {
    val builder = StreamsBuilder()
    topology(builder)

    // Start and start streaming
    val topology: Topology = builder.build()
    logger.info(topology.describe().toString())
    val streams = KafkaStreams(topology, config())
    streams.cleanUp() // only do this in dev - not in prod
    streams.start()
    // shutdown hook to correctly close the streams application
    Runtime.getRuntime().addShutdownHook(Thread(streams::close))
  }
}
