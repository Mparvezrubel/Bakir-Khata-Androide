package com.example.data.repository

import com.example.data.dao.KhataDao
import com.example.data.model.Customer
import com.example.data.model.CustomerType
import com.example.data.model.CustomerWithBalance
import com.example.data.model.KhataBackupData
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

data class DashboardMetrics(
    val totalReceivable: Double = 0.0, // মোট পাবো
    val totalPayable: Double = 0.0,    // মোট দেবো
    val netBalance: Double = 0.0,      // নিট ব্যালেন্স
    val totalCustomers: Int = 0,       // মোট কাস্টমার
    val receivableCount: Int = 0,
    val payableCount: Int = 0
)

class KhataRepository(private val dao: KhataDao) {

    val customersWithBalances: Flow<List<CustomerWithBalance>> =
        combine(dao.getAllCustomers(), dao.getAllTransactions()) { customers, transactions ->
            val txByCustomer = transactions.groupBy { it.customerId }

            customers.map { customer ->
                val txs = txByCustomer[customer.id] ?: emptyList()
                calculateCustomerBalance(customer, txs)
            }
        }

    val dashboardMetrics: Flow<DashboardMetrics> =
        customersWithBalances.combine(dao.getAllTransactions()) { customerBalances, _ ->
            var receivable = 0.0
            var payable = 0.0
            var recCount = 0
            var payCount = 0

            customerBalances.forEach { item ->
                when (item.customer.customerType) {
                    CustomerType.CUSTOMER -> {
                        if (item.netBalance > 0.001) {
                            receivable += item.netBalance
                            recCount++
                        } else if (item.netBalance < -0.001) {
                            payable += kotlin.math.abs(item.netBalance)
                            payCount++
                        }
                    }
                    CustomerType.SUPPLIER -> {
                        if (item.netBalance > 0.001) {
                            payable += item.netBalance
                            payCount++
                        } else if (item.netBalance < -0.001) {
                            receivable += kotlin.math.abs(item.netBalance)
                            recCount++
                        }
                    }
                }
            }

            DashboardMetrics(
                totalReceivable = receivable,
                totalPayable = payable,
                netBalance = receivable - payable,
                totalCustomers = customerBalances.size,
                receivableCount = recCount,
                payableCount = payCount
            )
        }

    private fun calculateCustomerBalance(customer: Customer, txs: List<Transaction>): CustomerWithBalance {
        var totalGiven = 0.0
        var totalReceived = 0.0

        txs.forEach { tx ->
            when (tx.type) {
                TransactionType.GAVE_CREDIT -> totalGiven += tx.amount
                TransactionType.GOT_PAYMENT -> totalReceived += tx.amount
                TransactionType.TOOK_CREDIT -> totalGiven += tx.amount
                TransactionType.GAVE_PAYMENT -> totalReceived += tx.amount
            }
        }

        val net = totalGiven - totalReceived

        return CustomerWithBalance(
            customer = customer,
            netBalance = net,
            totalGiven = totalGiven,
            totalReceived = totalReceived,
            lastTransactionDate = txs.maxOfOrNull { it.date },
            transactionCount = txs.size
        )
    }

    fun getCustomer(customerId: Long): Flow<Customer?> = dao.getCustomerById(customerId)

    fun getTransactionsForCustomer(customerId: Long): Flow<List<Transaction>> =
        dao.getTransactionsForCustomer(customerId)

    suspend fun insertCustomer(customer: Customer): Long = withContext(Dispatchers.IO) {
        dao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        dao.updateCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        dao.deleteCustomer(customer)
    }

    suspend fun addTransaction(transaction: Transaction): Long = withContext(Dispatchers.IO) {
        val txId = dao.insertTransaction(transaction)
        val customer = dao.getCustomerByIdDirect(transaction.customerId)
        if (customer != null) {
            dao.updateCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
        }
        txId
    }

    suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        dao.deleteTransaction(transaction)
        val customer = dao.getCustomerByIdDirect(transaction.customerId)
        if (customer != null) {
            dao.updateCustomer(customer.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun exportToJson(shopName: String): String = withContext(Dispatchers.IO) {
        val customers = dao.getAllCustomersDirect()
        val transactions = dao.getAllTransactionsDirect()
        val backupData = KhataBackupData(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            shopName = shopName,
            customers = customers,
            transactions = transactions
        )
        backupData.toJsonString()
    }

    suspend fun restoreFromJson(jsonString: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val backupData = KhataBackupData.fromJsonString(jsonString)
            dao.restoreDatabase(backupData.customers, backupData.transactions)
            Result.success(backupData.customers.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun seedDemoDataIfEmpty() = withContext(Dispatchers.IO) {
        val count = dao.getAllCustomersDirect().size
        if (count == 0) {
            val c1Id = dao.insertCustomer(
                Customer(
                    name = "আব্দুর রহিম",
                    phone = "01711223344",
                    address = "চকবাজার, ঢাকা",
                    customerType = CustomerType.CUSTOMER
                )
            )
            val c2Id = dao.insertCustomer(
                Customer(
                    name = "মো: কামাল হোসেন",
                    phone = "01819876543",
                    address = "মিয়া বাড়ি, চট্টগ্রাম",
                    customerType = CustomerType.CUSTOMER
                )
            )
            val c3Id = dao.insertCustomer(
                Customer(
                    name = "মেসার্স জামান ট্রেডার্স",
                    phone = "01912345678",
                    address = "খাতুনগঞ্জ, চট্টগ্রাম",
                    customerType = CustomerType.SUPPLIER
                )
            )
            val c4Id = dao.insertCustomer(
                Customer(
                    name = "সুমন আহমেদ",
                    phone = "01678123456",
                    address = "নিউ মার্কেট, সিলেট",
                    customerType = CustomerType.CUSTOMER
                )
            )

            val now = System.currentTimeMillis()
            val day = 24 * 60 * 60 * 1000L

            // Transactions for c1
            dao.insertTransaction(
                Transaction(
                    customerId = c1Id,
                    amount = 3500.0,
                    type = TransactionType.GAVE_CREDIT,
                    note = "মিনিকেট চাল ২ বস্তা ও তেল",
                    date = now - 5 * day
                )
            )
            dao.insertTransaction(
                Transaction(
                    customerId = c1Id,
                    amount = 1500.0,
                    type = TransactionType.GOT_PAYMENT,
                    note = "নগদ জমা",
                    date = now - 2 * day
                )
            )

            // Transactions for c2
            dao.insertTransaction(
                Transaction(
                    customerId = c2Id,
                    amount = 4850.0,
                    type = TransactionType.GAVE_CREDIT,
                    note = "মুদি মালামাল ও মসলা",
                    date = now - 3 * day
                )
            )

            // Transactions for c3 (Supplier)
            dao.insertTransaction(
                Transaction(
                    customerId = c3Id,
                    amount = 12000.0,
                    type = TransactionType.TOOK_CREDIT,
                    note = "পাইকারি চিনি ও ডাল চালান #৪০৪",
                    date = now - 7 * day
                )
            )
            dao.insertTransaction(
                Transaction(
                    customerId = c3Id,
                    amount = 7000.0,
                    type = TransactionType.GAVE_PAYMENT,
                    note = "ব্যাংক ট্রান্সফার পেমেন্ট",
                    date = now - 1 * day
                )
            )

            // Transactions for c4 (Clear account)
            dao.insertTransaction(
                Transaction(
                    customerId = c4Id,
                    amount = 1200.0,
                    type = TransactionType.GAVE_CREDIT,
                    note = "বিস্কুট ও চানাচুর কার্টুন",
                    date = now - 6 * day
                )
            )
            dao.insertTransaction(
                Transaction(
                    customerId = c4Id,
                    amount = 1200.0,
                    type = TransactionType.GOT_PAYMENT,
                    note = "বিকাশ পেমেন্ট",
                    date = now - 1 * day
                )
            )
        }
    }
}
