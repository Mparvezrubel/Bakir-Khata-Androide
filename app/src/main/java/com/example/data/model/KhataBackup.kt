package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

data class KhataBackupData(
    val version: Int = 1,
    val exportedAt: Long = System.currentTimeMillis(),
    val shopName: String,
    val customers: List<Customer>,
    val transactions: List<Transaction>
) {
    fun toJsonString(): String {
        val root = JSONObject()
            .put("version", version)
            .put("exportedAt", exportedAt)
            .put("shopName", shopName)
        val customerArray = JSONArray()
        customers.forEach { customer ->
            customerArray.put(JSONObject()
                .put("id", customer.id)
                .put("name", customer.name)
                .put("phone", customer.phone)
                .put("address", customer.address)
                .put("customerType", customer.customerType.name)
                .put("createdAt", customer.createdAt)
                .put("updatedAt", customer.updatedAt))
        }
        root.put("customers", customerArray)
        val transactionArray = JSONArray()
        transactions.forEach { transaction ->
            val item = JSONObject()
                .put("id", transaction.id)
                .put("customerId", transaction.customerId)
                .put("amount", transaction.amount)
                .put("type", transaction.type.name)
                .put("note", transaction.note)
                .put("billNumber", transaction.billNumber)
                .put("date", transaction.date)
            transaction.dueDate?.let { item.put("dueDate", it) }
            transactionArray.put(item)
        }
        root.put("transactions", transactionArray)
        return root.toString(2)
    }

    companion object {
        fun fromJsonString(jsonStr: String): KhataBackupData {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", 1)
            require(version == 1) { "Unsupported backup version: $version" }
            require(root.has("customers") && !root.isNull("customers")) { "Missing customers list" }
            require(root.has("transactions") && !root.isNull("transactions")) { "Missing transactions list" }
            val customerArray = root.getJSONArray("customers")
            val transactionArray = root.getJSONArray("transactions")
            val customers = ArrayList<Customer>(customerArray.length())
            for (i in 0 until customerArray.length()) {
                val item = customerArray.getJSONObject(i)
                val type = item.optString("customerType", item.optString("type", "CUSTOMER"))
                customers += Customer(
                    id = item.optLong("id", 0L),
                    name = item.optString("name", "গ্রাহক"),
                    phone = item.optString("phone", ""),
                    address = item.optString("address", ""),
                    customerType = runCatching { CustomerType.valueOf(type) }.getOrDefault(CustomerType.CUSTOMER),
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis())
                )
            }
            val customerIds = customers.mapTo(HashSet()) { it.id }
            val transactions = ArrayList<Transaction>(transactionArray.length())
            for (i in 0 until transactionArray.length()) {
                val item = transactionArray.getJSONObject(i)
                val customerId = item.optLong("customerId", 0L)
                require(customerId in customerIds) { "Transaction references an unknown customer" }
                val dueDate = if (item.has("dueDate") && !item.isNull("dueDate")) item.optLong("dueDate") else null
                transactions += Transaction(
                    id = item.optLong("id", 0L),
                    customerId = customerId,
                    amount = item.optDouble("amount", 0.0),
                    type = runCatching { TransactionType.valueOf(item.optString("type", "GAVE_CREDIT")) }
                        .getOrDefault(TransactionType.GAVE_CREDIT),
                    note = item.optString("note", ""),
                    billNumber = item.optString("billNumber", ""),
                    date = item.optLong("date", System.currentTimeMillis()),
                    dueDate = dueDate
                )
            }
            return KhataBackupData(
                version = version,
                exportedAt = root.optLong("exportedAt", root.optLong("exportTimestamp", System.currentTimeMillis())),
                shopName = root.optString("shopName", "বাকির খাতা"),
                customers = customers,
                transactions = transactions
            )
        }
    }
}
