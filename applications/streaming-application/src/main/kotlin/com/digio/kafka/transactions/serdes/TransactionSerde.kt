package com.digio.kafka.transactions.serdes

import com.digio.kafka.transactions.Transaction
import org.apache.kafka.common.serialization.Deserializer
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serializer

class TransactionSerde : Serde<Transaction> {
  override fun configure(configs: MutableMap<String, *>?, isKey: Boolean) {
  }

  override fun deserializer(): Deserializer<Transaction> {
    return JsonDeserializer(Transaction::class.java)
  }

  override fun close() {
  }

  override fun serializer(): Serializer<Transaction> {
    return JsonSerializer()
  }

}
