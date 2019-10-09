package com.digio.kafka.transactions

import com.digio.kafka.transactions.serdes.JsonDeserializer
import com.digio.kafka.transactions.serdes.JsonSerializer
import junit.framework.TestCase
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.LongDeserializer
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.TopologyTestDriver
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.Produced
import org.apache.kafka.streams.kstream.WindowedSerdes
import org.apache.kafka.streams.test.ConsumerRecordFactory
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import java.util.Date

class StreamingTopologyTest : TestCase() {
  companion object {
    private val transactionRecordFactory: ConsumerRecordFactory<String, Transaction> =
        ConsumerRecordFactory("transaction-topic", StringSerializer(), JsonSerializer<Transaction>())
    private val categoryRecordFactory: ConsumerRecordFactory<String, String> =
        ConsumerRecordFactory("category-topic", StringSerializer(), StringSerializer())

    private fun createTransactionRecord(message: Transaction): ConsumerRecord<ByteArray, ByteArray> {
      return transactionRecordFactory.create("transaction-topic", message.customer, message)
    }

    private fun createCategoryRecord(key: String, value: String): ConsumerRecord<ByteArray, ByteArray> {
      return categoryRecordFactory.create("category-topic", key, value)
    }

    private fun readOutput(driver: TopologyTestDriver): ProducerRecord<String, Long> {
      return driver.readOutput("output", StringDeserializer(), LongDeserializer())
    }

    private fun readTransaction(driver: TopologyTestDriver): ProducerRecord<String, Transaction> {
      return driver.readOutput("output", StringDeserializer(), JsonDeserializer<Transaction>(Transaction::class.java))
    }
  }

  fun `test total`() {
    val builder: StreamsBuilder = StreamsBuilder()
    val stream: KStream<String, Transaction>? = StreamingTopology.createStream(builder)
    val totals: KStream<String, Long>? = StreamingTopology.computeTotals(stream)
    totals?.to("output", Produced.with(Serdes.StringSerde(), Serdes.LongSerde()))

    TopologyTestDriver(builder.build(), StreamingTopology.config()).use { testDriver ->
      testDriver.pipeInput(
          createTransactionRecord(
              Transaction("dean", 150, "", Date().time)))
      testDriver.pipeInput(
          createTransactionRecord(
              Transaction("alice", 6, "", Date().time)))
      testDriver.pipeInput(
          createTransactionRecord(
              Transaction("dean", 100, "", Date().time)))

      readOutput(testDriver)
      val aliceTotal = readOutput(testDriver)
      val deanTotal = readOutput(testDriver)

      assertThat<String>(deanTotal.key(), `is`<String>("dean"))
      assertThat<Long>(deanTotal.value(), `is`<Long>(250L))

      assertThat<String>(aliceTotal.key(), `is`<String>("alice"))
      assertThat<Long>(aliceTotal.value(), `is`<Long>(6L))
    }
  }

  fun `test rolling total in same window`() {
    val builder = StreamsBuilder()
    val stream = StreamingTopology.createStream(builder)
    val totals = StreamingTopology.computeRunningTotal(stream)
    totals?.to("output", Produced.with(WindowedSerdes.timeWindowedSerdeFrom(String::class.java), Serdes.LongSerde()))



    TopologyTestDriver(builder.build(), StreamingTopology.config()).use { testDriver ->

      val now = Date().time

      testDriver.pipeInput(createTransactionRecord(
          Transaction("dean", 150, "", now)))
      testDriver.pipeInput(createTransactionRecord(
          Transaction("dean", 100, "", now)))

      readOutput(testDriver)
      val deanTotal = readOutput(testDriver)

      assertThat(deanTotal.value(), `is`(250L))
    }
  }

  fun `test rolling total in different window`() {
    val builder = StreamsBuilder()
    val stream = StreamingTopology.createStream(builder)
    val totals = StreamingTopology.computeRunningTotal(stream)
    totals?.to("output", Produced.with(WindowedSerdes.timeWindowedSerdeFrom(String::class.java), Serdes.LongSerde()))



    TopologyTestDriver(builder.build(), StreamingTopology.config()).use { testDriver ->

      val now = Date().time

      testDriver.pipeInput(createTransactionRecord(
          Transaction("dean", 150, "", now)))
      testDriver.pipeInput(
          createTransactionRecord(
              Transaction("dean", 100, "", now + 1000 * 40)))

      readOutput(testDriver)
      val deanTotal = readOutput(testDriver)

      assertThat(deanTotal.value(), `is`(100L))
    }
  }

  fun `test transaction enhancement`() {

    val builder = StreamsBuilder()
    val transactionStream = StreamingTopology.createStream(builder)
    val lookup = StreamingTopology.createCategoryLookupTable(builder)

    val enhanced = StreamingTopology.categorisedStream(transactionStream!!, lookup)
    enhanced.to("output")


    TopologyTestDriver(builder.build(), StreamingTopology.config()).use { testDriver ->
      val categoryKey = "CG01"
      testDriver.pipeInput(
          createCategoryRecord(categoryKey, "Groceries"))
      testDriver.pipeInput(createTransactionRecord(
          Transaction("dean", 100, categoryKey, Date().time)))

      val actual = readTransaction(testDriver)

      // Assert that there is only one message on the topic
      assertThat(testDriver.readOutput("output"), `is`<Any>(nullValue()))

      assertThat(actual.value().amount, `is`(100L))
      assertThat(actual.value().category, `is`("Groceries"))

    }

  }
}
