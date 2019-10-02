package com.digio.kafka.transactions.serdes

import com.digio.kafka.transactions.Transaction
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.streams.processor.TimestampExtractor

class MessageTimeExtractor : TimestampExtractor {
  override fun extract(record: ConsumerRecord<Any, Any>?, previousTimestamp: Long): Long {
    return (record?.value() as Transaction).occurred
  }
}
