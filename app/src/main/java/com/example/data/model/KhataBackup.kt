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
        root.put("version", version)
        root.put("exportedAt", exportedAt)
        root.put("shopName", shopName)

        val customerArray = JSONArray()
        customers.forEach { c ->
            val cObj = JSONObject()
            cObj.put("id", c.id)
            cObj.put("name", c.name)
            cObj.put("phone", c.phone)
            cObj.put("address", c.address)
            cObj.put("customerType", c.customerType.name)
            cObj.put("createdAt", c.createdAt)
            cObj.put("updatedAt", c.updatedAt)
            customerArray.put(cObj)
        }
        root.put("customers", customerArray)

        val txArray = JSONArray()
        transactions.forEach { t ->
            val tObj = JSONObject()
            tObj.put("id", t.id)
            tObj.put("customerId", t.customerId)
            tObj.put("amount", t.amount)
            tObj.put("type", t.type.name)
            tObj.put("note", t.note)
            tObj.put("billNumber", t.billNumber)
            tObj.put("date", t.date)
            if (t.dueDate != null) {
                tObj.put("dueDate", t.dueDate)
            }
            txArray.put(tObj)
        }
        root.put("transactions", txArray)

        return root.toString(2)
    }

    companion object {
        fun fromJsonString(jsonStr: String): KhataBackupData {
            val root = JSONObject(jsonStr)
            val version = root.optInt("version", 1)
            val exportedAt = root.optLong("exportedAt", System.currentTimeMillis())
            val shopName = root.optString("shopName", "বাকির খাতা")

            val customerList = mutableListOf<Customer>()
            val customerArray = root.optJSONArray("customers")
            if (customerArray != null) {
                for (i in 0 until customerArray.length()) {
                    val cObj = customerArray.getJSONObject(i)
                    customerList.add(
                        Customer(
                            id = cObj.optLong("id", 0),
                            name = cObj.optString("name", "গ্রাহক"),
                            phone = cObj.optString("phone", ""),
                            address = cObj.optString("address", ""),
                            customerType = runCatching {
                                CustomerType.valueOf(cObj.optString("customerType", "CUSTOMER"))
                            }.getOrDefault(CustomerType.CUSTOMER),
                            createdAt = cObj.optLong("createdAt", System.currentTimeMillis()),
                            updatedAt = cObj.optLong("updatedAt", System.currentTimeMillis())
                        )
                    )
                }
            }

            val txList = mutableListOf<Transaction>()
            val txArray = root.optJSONArray("transactions")
            if (txArray != null) {
                for (i in 0 until txArray.length()) {
                    val tObj = txArray.getJSONObject(i)
                    txList.add(
                        Transaction(
                            id = tObj.optLong("id", 0),
                            customerId = tObj.optLong("customerId", 0),
                            amount = tObj.optDouble("amount", 0.0),
                            type = runCatching {
                                TransactionType.valueOf(tObj.optString("type", "GAVE_CREDIT"))
                            }.getOrDefault(TransactionType.GAVE_CREDIT),
                            note = tObj.optString("note", ""),
                            billNumber = tObj.optString("billNumber", ""),
                            date = tObj.optLong("date", System.currentTimeMillis()),
                            dueDate = if (tObj.has("dueDate")) tObj.optLong("dueDate") else null
                        )
                    )
                }
            }

            return KhataBackupData(
                version = version,
                exportedAt = exportedAt,
                shopName = shopName,
                customers = customerList,
                transactions = txList
            )
        }
    }
}
