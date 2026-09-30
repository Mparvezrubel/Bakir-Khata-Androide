package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalanceWallet
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.CustomerType
import com.example.data.model.CustomerWithBalance
import com.example.ui.BakirKhataViewModel
import com.example.ui.FilterType
import com.example.ui.dialogs.AddCustomerDialog
import com.example.ui.dialogs.SmsReminderDialog
import com.example.ui.theme.KhataCardBorder
import com.example.ui.theme.KhataPayableBg
import com.example.ui.theme.KhataPayableBorder
import com.example.ui.theme.KhataPayableRed
import com.example.ui.theme.KhataPrimary
import com.example.ui.theme.KhataPrimaryGradientEnd
import com.example.ui.theme.KhataPrimaryGradientStart
import com.example.ui.theme.KhataReceivableBg
import com.example.ui.theme.KhataReceivableBorder
import com.example.ui.theme.KhataReceivableGreen
import com.example.ui.theme.KhataSettledBg
import com.example.ui.theme.KhataSettledBorder
import com.example.ui.theme.KhataSettledGray
import com.example.ui.theme.KhataTextPrimary
import com.example.ui.theme.KhataTextSecondary
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = viewModel.shopPrefs.shopName.ifBlank { "বাকির খাতা" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF4ADE80), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "ডিজিটাল লেজার • ১০০% অফলাইন",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                },
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(
                            onClick = onNavigateToSettings,
                            modifier = Modifier.testTag("settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "ব্যাকআপ ও সেটিংস",
                                tint = Color.White
                            )
                        }
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
                icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp)) },
                text = { Text("নতুন কাস্টমার", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                containerColor = KhataPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp),
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
            // Dashboard Summary Hero Card
            DashboardHeader(
                metrics = metrics,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // Search Bar
            SearchBarSection(
                query = searchQuery,
                onQueryChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
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
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
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
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, KhataCardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = KhataPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "মোট বাকি সারসংক্ষেপ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KhataTextSecondary
                    )
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KhataPrimary.copy(alpha = 0.08f)
                ) {
                    Text(
                        text = "${metrics.totalCustomers} জন ক্লায়েন্ট",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = KhataPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Two-column Financial Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Total Receivable (পাবো)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = KhataReceivableBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KhataReceivableBorder)
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
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "আপনি পাবেন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = KhataReceivableGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Formatters.formatTaka(metrics.totalReceivable),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = KhataReceivableGreen,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${metrics.receivableCount} জন গ্রাহকের কাছে",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = KhataReceivableGreen.copy(alpha = 0.85f)
                        )
                    }
                }

                // Total Payable (দেবো)
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = KhataPayableBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KhataPayableBorder)
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
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "আপনি দেবেন",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = KhataPayableRed
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = Formatters.formatTaka(metrics.totalPayable),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = KhataPayableRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${metrics.payableCount} জন সাপ্লায়ারের কাছে",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = KhataPayableRed.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Net balance strip
            val net = metrics.netBalance
            val isNetPositive = net >= 0
            val netColor = if (isNetPositive) KhataReceivableGreen else KhataPayableRed
            val netBg = if (isNetPositive) KhataReceivableBg else KhataPayableBg
            val netStatus = if (isNetPositive) "পাওনা বেশি" else "দেনা বেশি"

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = netColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "নিট ব্যালেন্স স্থিতি:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = KhataTextSecondary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = Formatters.formatTaka(net),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = netColor
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = netBg
                        ) {
                            Text(
                                text = netStatus,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = netColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
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
        placeholder = {
            Text(
                "গ্রাহকের নাম বা ফোন নম্বর খুঁজুন...",
                fontSize = 13.sp,
                color = KhataTextSecondary.copy(alpha = 0.7f)
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = KhataPrimary,
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(150))
            ) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = KhataTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedBorderColor = KhataCardBorder,
            focusedBorderColor = KhataPrimary
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
            val isSelected = selectedFilter == filter

            FilterChip(
                selected = isSelected,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = "${filter.titleBangla} $countText".trim(),
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) KhataPrimary else KhataCardBorder
                ),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = KhataPrimary,
                    selectedLabelColor = Color.White,
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = KhataTextPrimary
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

    val (avatarColor, badgeBg, badgeBorder, badgeText) = when {
        item.isReceivable -> Quadruple(KhataReceivableGreen, KhataReceivableBg, KhataReceivableBorder, "পাবো")
        item.isPayable -> Quadruple(KhataPayableRed, KhataPayableBg, KhataPayableBorder, "দেবো")
        else -> Quadruple(KhataSettledGray, KhataSettledBg, KhataSettledBorder, "পরিশোধিত")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("customer_card_${customer.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, KhataCardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Modern Styled Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(avatarColor.copy(alpha = 0.12f))
                    .border(1.5.dp, avatarColor.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    color = avatarColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
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
                        color = KhataTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (customer.customerType == CustomerType.SUPPLIER) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = KhataPrimary.copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = "পাইকার",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                                color = KhataPrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (customer.phone.isNotBlank()) {
                        Text(
                            text = customer.phone,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = KhataTextSecondary
                        )
                    }
                    if (item.lastTransactionDate != null) {
                        if (customer.phone.isNotBlank()) {
                            Text(
                                text = " • ",
                                fontSize = 11.sp,
                                color = KhataTextSecondary.copy(alpha = 0.6f)
                            )
                        }
                        Text(
                            text = Formatters.formatRelativeDateBangla(item.lastTransactionDate),
                            fontSize = 11.sp,
                            color = KhataTextSecondary
                        )
                    }
                }
            }

            // Balance and Quick Actions
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatTaka(item.displayAmount),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = avatarColor
                )

                Spacer(modifier = Modifier.height(2.dp))

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeBg,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, badgeBorder)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = avatarColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Action buttons: SMS & Call
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (customer.phone.isNotBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { onCallClick() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Call",
                                    modifier = Modifier.size(14.dp),
                                    tint = KhataPrimary
                                )
                            }
                        }
                    }

                    // SMS Reminder button (shows if customer owes money)
                    if (item.isReceivable && customer.phone.isNotBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = KhataReceivableBg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, KhataReceivableBorder),
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .clickable { onSmsClick() }
                                .testTag("quick_sms_button_${customer.id}")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Message,
                                    contentDescription = "SMS Reminder",
                                    modifier = Modifier.size(14.dp),
                                    tint = KhataReceivableGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun EmptyStateView(
    isSearch: Boolean,
    onAddClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isSearch) {
                // Generated 3D Ledger Art
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, KhataCardBorder),
                    modifier = Modifier.size(130.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_empty_ledger_1790791815560),
                        contentDescription = "বাকির খাতা",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Fit
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = KhataTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isSearch) "কোনো তথ্য পাওয়া যায়নি" else "আপনার খাতা সম্পূর্ণ খালি",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = KhataTextPrimary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (isSearch)
                    "অন্য কোনো নাম বা ফোন নম্বর লিখে খুঁজুন"
                else
                    "গ্রাহক বা সরবরাহকারী যুক্ত করে সহজে প্রতিদিনের বাকির হিসাব রাখুন",
                fontSize = 13.sp,
                color = KhataTextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            if (!isSearch) {
                Spacer(modifier = Modifier.height(20.dp))
                ExtendedFloatingActionButton(
                    onClick = onAddClick,
                    icon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    text = { Text("প্রথম কাস্টমার যোগ করুন", fontWeight = FontWeight.Bold, fontSize = 14.sp) },
                    containerColor = KhataPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp)
                )
            }
        }
    }
}
