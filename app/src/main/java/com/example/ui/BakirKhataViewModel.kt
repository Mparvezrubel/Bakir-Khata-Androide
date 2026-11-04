package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.Customer
import com.example.data.model.CustomerType
import com.example.data.model.CustomerWithBalance
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.data.preferences.ShopPreferences
import com.example.data.repository.DashboardMetrics
import com.example.data.repository.KhataRepository
import com.example.util.DriveBackupStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Dashboard filters used by the customer list. */
enum class FilterType(val titleBangla: String) {
    ALL("সব গ্রাহক"),
    RECEIVABLE("পাবো"),
    PAYABLE("দেবো"),
    SETTLED("পরিশোধিত"),
    SUPPLIERS("সাপ্লায়ার")
}

@OptIn(ExperimentalCoroutinesApi::class)
class BakirKhataViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = KhataRepository(database.khataDao())
    private val restoreMutex = Mutex()
    val shopPrefs = ShopPreferences(application)

    val metrics: StateFlow<DashboardMetrics> = repository.dashboardMetrics.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics()
    )
    val rawCustomers: StateFlow<List<CustomerWithBalance>> = repository.customersWithBalances.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _selectedFilter = MutableStateFlow(FilterType.ALL)
    val selectedFilter = _selectedFilter.asStateFlow()

    val filteredCustomers: StateFlow<List<CustomerWithBalance>> = combine(rawCustomers, _searchQuery, _selectedFilter) { list, query, filter ->
        list.filter { item ->
            val matchesQuery = query.isBlank() || item.customer.name.contains(query, true) ||
                item.customer.phone.contains(query, true) || item.customer.address.contains(query, true)
            val matchesFilter = when (filter) {
                FilterType.ALL -> true
                FilterType.RECEIVABLE -> item.isReceivable
                FilterType.PAYABLE -> item.isPayable
                FilterType.SETTLED -> item.isSettled
                FilterType.SUPPLIERS -> item.customer.customerType == CustomerType.SUPPLIER
            }
            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCustomerId = MutableStateFlow<Long?>(null)
    val selectedCustomerId = _selectedCustomerId.asStateFlow()
    val selectedCustomerWithBalance: StateFlow<CustomerWithBalance?> = combine(rawCustomers, _selectedCustomerId) { list, id ->
        if (id == null) null else list.firstOrNull { it.customer.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val selectedCustomerTransactions: StateFlow<List<Transaction>> = _selectedCustomerId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getTransactionsForCustomer(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Do not seed demo rows on a fresh install: that would make an empty-ledger cloud restore unsafe.
        // Demo data remains available through the explicit demo-data action.
        if (DriveBackupStorage.hasFolder(application)) {
            viewModelScope.launch { restoreDriveBackupIfLocalDatabaseEmpty() }
        }
    }

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onFilterChange(filter: FilterType) { _selectedFilter.value = filter }
    fun selectCustomer(customerId: Long?) { _selectedCustomerId.value = customerId }

    fun addCustomer(name: String, phone: String, address: String, type: CustomerType, initialAmount: Double = 0.0, initialIsCredit: Boolean = true) {
        viewModelScope.launch {
            val customerId = repository.insertCustomer(Customer(name = name.trim(), phone = phone.trim(), address = address.trim(), customerType = type))
            if (initialAmount > 0.0) {
                val txType = when (type) {
                    CustomerType.CUSTOMER -> if (initialIsCredit) TransactionType.GAVE_CREDIT else TransactionType.GOT_PAYMENT
                    CustomerType.SUPPLIER -> if (initialIsCredit) TransactionType.TOOK_CREDIT else TransactionType.GAVE_PAYMENT
                }
                repository.addTransaction(Transaction(customerId = customerId, amount = initialAmount, type = txType, note = "পূর্বের বকেয়া (প্রারম্ভিক ব্যালেন্স)"))
            }
        }
    }

    fun updateCustomer(customer: Customer) { viewModelScope.launch { repository.updateCustomer(customer) } }
    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            if (_selectedCustomerId.value == customer.id) _selectedCustomerId.value = null
        }
    }
    fun addTransaction(customerId: Long, amount: Double, type: TransactionType, note: String, billNumber: String = "", date: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            repository.addTransaction(Transaction(customerId = customerId, amount = amount, type = type, note = note.trim(), billNumber = billNumber.trim(), date = date))
        }
    }
    fun deleteTransaction(transaction: Transaction) { viewModelScope.launch { repository.deleteTransaction(transaction) } }

    suspend fun getBackupJson(): String = repository.exportToJson(shopPrefs.shopName)
    fun markBackupCompleted() { shopPrefs.lastBackupTimestamp = System.currentTimeMillis() }

    /** Auto-restore is deliberately limited to an empty local ledger; never overwrites existing customer data. */
    suspend fun restoreDriveBackupIfLocalDatabaseEmpty(): Boolean = restoreMutex.withLock {
        withContext(Dispatchers.IO) {
            val dao = database.khataDao()
            if (dao.getAllCustomersDirect().isNotEmpty()) return@withContext false
            val json = DriveBackupStorage.readLatestBackup(getApplication())?.takeIf(String::isNotBlank)
                ?: return@withContext false
            val result = repository.restoreFromJson(json)
            if (result.isSuccess) {
                markBackupCompleted()
                shopPrefs.lastAutoBackupTimestamp = System.currentTimeMillis()
                true
            } else false
        }
    }

    fun restoreFromJson(json: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.restoreFromJson(json)
            if (result.isSuccess) {
                markBackupCompleted()
                onResult(true, "সফলভাবে ${result.getOrNull()} জন গ্রাহকের হিসাব রিস্টোর সম্পন্ন হয়েছে।")
            } else onResult(false, "রিস্টোর ব্যর্থ হয়েছে: সঠিক ব্যাকআপ ফাইল নির্বাচন করুন।")
        }
    }

    fun resetToDemoData() {
        viewModelScope.launch {
            repository.restoreFromJson("{\"version\":1,\"customers\":[],\"transactions\":[]}")
            repository.seedDemoDataIfEmpty()
        }
    }
}
