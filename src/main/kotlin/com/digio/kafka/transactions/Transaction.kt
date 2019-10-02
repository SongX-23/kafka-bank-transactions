package com.digio.kafka.transactions

data class Transaction(val customer: String,
                       val amount: Long,
                       var category: String,
                       val occurred: Long) {

    override fun toString(): String {
        return "Transaction{" +
                "customer='" + customer + '\'' +
                ", amount=" + amount +
                ", category=" + category +
                ", occurred=" + occurred +
                '}';
    }

    constructor() : this("", 0, "", 0)
}
