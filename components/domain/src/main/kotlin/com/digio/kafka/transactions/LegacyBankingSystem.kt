package com.digio.kafka.transactions

import java.util.Date
import java.util.Random

class LegacyBankingSystem {
    companion object {
        private val customers = listOf("Alice", "Bob", "Charlie", "Dan", "Edwardo", "Fran")
        private val categories = listOf("CG01", "CG02", "CG03", "CG04")
        private val rand: Random = Random(90210)
    }

    fun getTransaction(): Transaction {
        return Transaction(
            randomCustomer(),
            rand.nextInt(100).toLong(),
            randomCategory(),
            Date().time
            )
    }

    private fun randomCustomer(): String {
        return customers[rand.nextInt(
            customers.size)]
    }
    private fun randomCategory(): String {
        return categories[rand.nextInt(
            categories.size)]
    }
}
