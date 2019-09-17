package com.digio.kafka.transactions

import java.math.BigDecimal

data class Transaction(val customer: String,
                       val amount: BigDecimal,
                       val category: String,
                       val occurred: Long) {

    override fun toString(): String {
        return "Transaction{" +
                "customer='" + customer + '\'' +
                ", amount=" + amount +
                ", category=" + category +
                ", occurred=" + occurred +
                '}';
    }
}