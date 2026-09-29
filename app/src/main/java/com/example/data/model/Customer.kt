package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class CustomerType {
    CUSTOMER, // সাধারণ ক্রেতা / কাস্টমার
    SUPPLIER  // পাইকার / সরবরাহকারী
}

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val phone: String,
    val address: String = "",
    val customerType: CustomerType = CustomerType.CUSTOMER,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
