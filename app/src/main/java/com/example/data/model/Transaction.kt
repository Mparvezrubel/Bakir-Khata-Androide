package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    // গ্রাহকের সাথে লেনদেন (Customer transactions)
    GAVE_CREDIT,  // আপনি বাকিতে দিয়েছেন (গ্রাহকের কাছে পাবেন ↑)
    GOT_PAYMENT,  // আপনি টাকা পেয়েছেন / জমা (গ্রাহকের বাকি কমবে ↓)

    // সরবরাহকারীর সাথে লেনদেন (Supplier transactions)
    TOOK_CREDIT,  // আপনি বাকিতে মাল নিয়েছেন (সরবরাহকারীকে দেবেন ↑)
    GAVE_PAYMENT  // আপনি টাকা পরিশোধ করেছেন (দেবেন বাকি কমবে ↓)
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("customerId"),
        Index("date")
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val customerId: Long,
    val amount: Double,
    val type: TransactionType,
    val note: String = "",
    val billNumber: String = "",
    val date: Long = System.currentTimeMillis(),
    val dueDate: Long? = null
)
