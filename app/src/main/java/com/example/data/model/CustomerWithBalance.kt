package com.example.data.model

data class CustomerWithBalance(
    val customer: Customer,
    val netBalance: Double,
    val totalGiven: Double,
    val totalReceived: Double,
    val lastTransactionDate: Long?,
    val transactionCount: Int
) {
    // আপনি কি টাকা পাবেন নাকি দেবেন?
    val isReceivable: Boolean
        get() = when (customer.customerType) {
            CustomerType.CUSTOMER -> netBalance > 0.001
            CustomerType.SUPPLIER -> netBalance < -0.001
        }

    val isPayable: Boolean
        get() = when (customer.customerType) {
            CustomerType.CUSTOMER -> netBalance < -0.001
            CustomerType.SUPPLIER -> netBalance > 0.001
        }

    val isSettled: Boolean
        get() = kotlin.math.abs(netBalance) <= 0.001

    // ডিসপ্লে করার জন্য এবসলিউট ব্যালেন্স
    val displayAmount: Double
        get() = kotlin.math.abs(netBalance)

    // বাংলা স্ট্যাটাস
    val statusTextBangla: String
        get() = when {
            isSettled -> "হিসাব পরিষ্কার"
            isReceivable -> "আপনি পাবেন"
            else -> "আপনি দেবেন"
        }
}
