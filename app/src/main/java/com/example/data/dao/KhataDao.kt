package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction as RoomTransaction
import androidx.room.Update
import com.example.data.model.Customer
import com.example.data.model.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface KhataDao {

    @Query("SELECT * FROM customers ORDER BY updatedAt DESC")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerById(id: Long): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerByIdDirect(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY date DESC")
    fun getTransactionsForCustomer(customerId: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY date ASC")
    suspend fun getTransactionsForCustomerAscending(customerId: Long): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransactionById(transactionId: Long)

    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersDirect(): List<Customer>

    @Query("SELECT * FROM transactions")
    suspend fun getAllTransactionsDirect(): List<Transaction>

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM customers")
    suspend fun clearAllCustomers()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomersBatch(customers: List<Customer>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionsBatch(transactions: List<Transaction>)

    @RoomTransaction
    suspend fun restoreDatabase(customers: List<Customer>, transactions: List<Transaction>) {
        clearAllTransactions()
        clearAllCustomers()
        insertCustomersBatch(customers)
        insertTransactionsBatch(transactions)
    }
}
