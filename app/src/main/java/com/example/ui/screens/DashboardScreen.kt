package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomerType
import com.example.data.model.CustomerWithBalance
import com.example.ui.BakirKhataViewModel
import com.example.ui.FilterType
import com.example.ui.dialogs.AddCustomerDialog
import com.example.ui.dialogs.SmsReminderDialog
import com.example.ui.theme.KhataPayableBg
import com.example.ui.theme.KhataPayableRed
import com.example.ui.theme.KhataPrimary
import com.example.ui.theme.KhataReceivableBg
import com.example.ui.theme.KhataReceivableGreen
import com.example.ui.theme.KhataSettledBg
import com.example.ui.theme.KhataSettledGray
import com.example.util.Formatters
import com.example.util.ReminderHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: BakirKhataViewModel,
    onCustomerClick: (Long) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val metrics by viewModel.metrics.collectAsStateWithLifecycle()
    val customers by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var smsTargetCustomer by remember { mutableStateOf<CustomerWithBalance?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = viewModel.shopPrefs.shopName.ifBlank { "বাকির খাতা" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CloudDone,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "অফলাইন সংরক্ষিত • ডিজিটাল খাতা",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "ব্যাকআপ ও সেটিংস",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KhataPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddCustomerDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("নতুন কাস্টমার", fontWeight = FontWeight.SemiBold) },
                containerColor = KhataPrimary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_customer_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Dashboard Summary Cards
            DashboardHeader(
                metrics = metrics,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Search Bar
            SearchBarSection(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            // Filter Chips
            FilterRow(
                selectedFilter = selectedFilter,
                metrics = metrics,
                onFilterSelected = { viewModel.onFilterChange(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Customer List
            if (customers.isEmpty()) {
                EmptyStateView(
                    isSearch = searchQuery.isNotBlank(),
                    onAddClick = { showAddCustomerDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(customers, key = { it.customer.id }) { item ->
                        CustomerCard(
                            item = item,
                            onClick = { onCustomerClick(item.customer.id) },
                            onSmsClick = { smsTargetCustomer = item },
                            onCallClick = { ReminderHelper.makePhoneCall(context, item.customer.phone) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddCustomerDialog) {
        AddCustomerDialog(
            onDismiss = { showAddCustomerDialog = false },
            onAddCustomer = { name, phone, address, type, amount, isCredit ->
                viewModel.addCustomer(name, phone, address, type, amount, isCredit)
            }
        )
    }

    smsTargetCustomer?.let { customerItem ->
        SmsReminderDialog(
            customerWithBalance = customerItem,
            shopName = viewModel.shopPrefs.shopName,
            smsTemplate = viewModel.shopPrefs.smsReminderTemplate,
            onDismiss = { smsTargetCustomer = null }
        )
    }
}

@Composable
fun DashboardHeader(
    metrics: com.example.data.repository.DashboardMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Receivable (পাবো)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = KhataReceivableBg)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(KhataReceivableGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "আপনি পাবেন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = KhataReceivableGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Formatters.formatTaka(metrics.totalReceivable),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = KhataReceivableGreen
                        )
                        Text(
                            text = "${metrics.receivableCount} জন গ্রাহক",
                            fontSize = 11.sp,
                            color = KhataReceivableGreen.copy(alpha = 0.8f)
                        )
                    }
                }

                // Total Payable (দেবো)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = KhataPayableBg)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(KhataPayableRed, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "আপনি দেবেন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = KhataPayableRed
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Formatters.formatTaka(metrics.totalPayable),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = KhataPayableRed
                        )
                        Text(
                            text = "${metrics.payableCount} জন বাকিদার",
                            fontSize = 11.sp,
                            color = KhataPayableRed.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Net balance badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "নিট ব্যালেন্স:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val net = metrics.netBalance
                val netColor = if (net >= 0) KhataReceivableGreen else KhataPayableRed
                val netLabel = if (net >= 0) "(পাওনা বাকি বেশি)" else "(দেনা বাকি বেশি)"

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Formatters.formatTaka(net),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = netColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = netLabel,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun SearchBarSection(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text("গ্রাহকের নাম বা ফোন নম্বর খুঁজুন...", fontSize = 14.sp) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
        },
        trailingIcon = {
            AnimatedVisibility(visible = query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface
        ),
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .testTag("search_customer_input")
    )
}

@Composable
fun FilterRow(
    selectedFilter: FilterType,
    metrics: com.example.data.repository.DashboardMetrics,
    onFilterSelected: (FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterType.values().forEach { filter ->
            val countText = when (filter) {
                FilterType.ALL -> "(${metrics.totalCustomers})"
                FilterType.RECEIVABLE -> "(${metrics.receivableCount})"
                FilterType.PAYABLE -> "(${metrics.payableCount})"
                FilterType.SETTLED -> ""
                FilterType.SUPPLIERS -> ""
            }
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text("${filter.titleBangla} $countText".trim(), fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = KhataPrimary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun CustomerCard(
    item: CustomerWithBalance,
    onClick: () -> Unit,
    onSmsClick: () -> Unit,
    onCallClick: () -> Unit
) {
    val customer = item.customer
    val initial = customer.name.firstOrNull()?.toString() ?: "ক"

    val avatarBg = when {
        item.isReceivable -> KhataReceivableGreen
        item.isPayable -> KhataPayableRed
        else -> KhataSettledGray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarBg.copy(alpha = 0.15f))
                    .border(1.5.dp, avatarBg.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = avatarBg,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = customer.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (customer.customerType == CustomerType.SUPPLIER) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "পাইকার",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (customer.phone.isNotBlank()) {
                        Text(
                            text = customer.phone,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    if (item.lastTransactionDate != null) {
                        if (customer.phone.isNotBlank()) {
                            Text(
                                text = " • ",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Text(
                            text = Formatters.formatRelativeDateBangla(item.lastTransactionDate),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Balance and Quick Actions
            Column(horizontalAlignment = Alignment.End) {
                val balanceColor = when {
                    item.isReceivable -> KhataReceivableGreen
                    item.isPayable -> KhataPayableRed
                    else -> KhataSettledGray
                }

                Text(
                    text = Formatters.formatTaka(item.displayAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = balanceColor
                )

                Text(
                    text = item.statusTextBangla,
                    fontSize = 11.sp,
                    color = balanceColor.copy(alpha = 0.9f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Action buttons: SMS & Call
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (customer.phone.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onCallClick() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Call",
                                modifier = Modifier.size(15.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // SMS Reminder button (shows if customer owes money)
                    if (item.isReceivable && customer.phone.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(KhataReceivableBg)
                                .clickable { onSmsClick() }
                                .testTag("quick_sms_button_${customer.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Message,
                                contentDescription = "SMS Reminder",
                                modifier = Modifier.size(15.dp),
                                tint = KhataReceivableGreen
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(
    isSearch: Boolean,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = if (isSearch) Icons.Default.Search else Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isSearch) "কোনো তথ্য পাওয়া যায়নি" else "এখনো কোনো বাকি হিসাব যোগ করা হয়নি",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isSearch) "অন্য নাম বা ফোন নম্বর লিখে অনুসন্ধান করুন" else "নতুন গ্রাহক যুক্ত করে বাকির হিসাব শুরু করুন",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline
            )
            if (!isSearch) {
                Spacer(modifier = Modifier.height(16.dp))
                ExtendedFloatingActionButton(
                    onClick = onAddClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("প্রথম গ্রাহক যোগ করুন") },
                    containerColor = KhataPrimary,
                    contentColor = Color.White
                )
            }
        }
    }
}
