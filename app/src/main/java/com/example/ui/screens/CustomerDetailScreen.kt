package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.BakirKhataViewModel
import com.example.ui.dialogs.AddTransactionDialog
import com.example.ui.dialogs.SmsReminderDialog
import com.example.ui.theme.KhataCardBorder
import com.example.ui.theme.KhataPayableBg
import com.example.ui.theme.KhataPayableBorder
import com.example.ui.theme.KhataPayableRed
import com.example.ui.theme.KhataPrimary
import com.example.ui.theme.KhataReceivableBg
import com.example.ui.theme.KhataReceivableBorder
import com.example.ui.theme.KhataReceivableGreen
import com.example.ui.theme.KhataSettledBg
import com.example.ui.theme.KhataSettledBorder
import com.example.ui.theme.KhataSettledGray
import com.example.ui.theme.KhataTextPrimary
import com.example.ui.theme.KhataTextSecondary
import com.example.ui.theme.WhatsAppGreen
import com.example.util.Formatters
import com.example.util.ReminderHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    viewModel: BakirKhataViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val customerWithBalance by viewModel.selectedCustomerWithBalance.collectAsStateWithLifecycle()
    val transactions by viewModel.selectedCustomerTransactions.collectAsStateWithLifecycle()

    var showAddTxDialog by remember { mutableStateOf<TransactionType?>(null) }
    var showSmsDialog by remember { mutableStateOf(false) }
    var showDeleteCustomerConfirm by remember { mutableStateOf(false) }
    var txToDelete by remember { mutableStateOf<Transaction?>(null) }

    BackHandler {
        onBack()
    }

    if (customerWithBalance == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("গ্রাহকের তথ্য পাওয়া যায়নি")
        }
        return
    }

    val item = customerWithBalance!!
    val customer = item.customer
    val isSupplier = customer.customerType == CustomerType.SUPPLIER

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = customer.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White
                        )
                        Text(
                            text = if (isSupplier) "সাপ্লায়ার / পাইকার খাতা" else "কাস্টমার / ক্রেতা খাতা",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("detail_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (customer.phone.isNotBlank()) {
                        IconButton(
                            onClick = { ReminderHelper.makePhoneCall(context, customer.phone) },
                            modifier = Modifier.testTag("detail_call_button")
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = Color.White)
                        }
                    }
                    IconButton(
                        onClick = { showDeleteCustomerConfirm = true },
                        modifier = Modifier.testTag("delete_customer_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Customer", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = KhataPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            // Action Buttons at bottom
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, KhataCardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!isSupplier) {
                        // Customer: Gave Credit (বাকিতে দিলাম - পাবো)
                        Button(
                            onClick = { showAddTxDialog = TransactionType.GAVE_CREDIT },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_gave_credit"),
                            colors = ButtonDefaults.buttonColors(containerColor = KhataPayableRed),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("বাকিতে দিলাম", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Customer: Got Payment (টাকা পেলাম - জমা)
                        Button(
                            onClick = { showAddTxDialog = TransactionType.GOT_PAYMENT },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_got_payment"),
                            colors = ButtonDefaults.buttonColors(containerColor = KhataReceivableGreen),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("টাকা পেলাম", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    } else {
                        // Supplier: Took Credit (বাকিতে নিলাম - দেবো)
                        Button(
                            onClick = { showAddTxDialog = TransactionType.TOOK_CREDIT },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_took_credit"),
                            colors = ButtonDefaults.buttonColors(containerColor = KhataPayableRed),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("বাকিতে নিলাম", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        // Supplier: Paid (টাকা দিলাম - পরিশোধ)
                        Button(
                            onClick = { showAddTxDialog = TransactionType.GAVE_PAYMENT },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("btn_gave_payment"),
                            colors = ButtonDefaults.buttonColors(containerColor = KhataReceivableGreen),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                        ) {
                            Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("টাকা দিলাম", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Customer Header & Balance Card
            CustomerLedgerHeader(
                item = item,
                onSmsClick = { showSmsDialog = true },
                onWhatsAppClick = {
                    if (customer.phone.isNotBlank()) {
                        val message = ReminderHelper.composeReminderMessage(
                            template = viewModel.shopPrefs.smsReminderTemplate,
                            customerName = customer.name,
                            amount = item.displayAmount,
                            shopName = viewModel.shopPrefs.shopName
                        )
                        ReminderHelper.openWhatsApp(context, customer.phone, message)
                    } else {
                        Toast.makeText(context, "ফোন নম্বর দেওয়া নেই", Toast.LENGTH_SHORT).show()
                    }
                },
                onShareStatement = {
                    shareCustomerStatement(context, item, transactions, viewModel.shopPrefs.shopName)
                }
            )

            // Transactions Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "লেনদেন বিবরণী (${transactions.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = KhataTextPrimary
                )
                Text(
                    text = "তারিখ অনুযায়ী",
                    fontSize = 11.sp,
                    color = KhataTextSecondary
                )
            }

            // Ledger Transaction List
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = KhataTextSecondary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "কোনো লেনদেন এন্ট্রি নেই",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = KhataTextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "নিচের বোতামগুলো চেপে প্রথম লেনদেন যোগ করুন",
                            fontSize = 12.sp,
                            color = KhataTextSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionItemCard(
                            transaction = tx,
                            isSupplier = isSupplier,
                            onDeleteClick = { txToDelete = tx }
                        )
                    }
                }
            }
        }
    }

    // Add Transaction Dialog
    showAddTxDialog?.let { initialType ->
        AddTransactionDialog(
            customer = customer,
            initialType = initialType,
            onDismiss = { showAddTxDialog = null },
            onSaveTransaction = { amount, type, note, bill, date ->
                viewModel.addTransaction(customer.id, amount, type, note, bill, date)
            }
        )
    }

    // SMS Reminder Dialog
    if (showSmsDialog) {
        SmsReminderDialog(
            customerWithBalance = item,
            shopName = viewModel.shopPrefs.shopName,
            smsTemplate = viewModel.shopPrefs.smsReminderTemplate,
            onDismiss = { showSmsDialog = false }
        )
    }

    // Delete Customer Confirmation
    if (showDeleteCustomerConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteCustomerConfirm = false },
            title = { Text("গ্রাহক খাতা মুছে ফেলতে চান?", fontWeight = FontWeight.Bold) },
            text = { Text("এই গ্রাহকের নাম এবং সমস্ত পূর্ববর্তী লেনদেনের হিসাব স্থায়ীভাবে মুছে ফেলা হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomer(customer)
                        showDeleteCustomerConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCustomerConfirm = false }) {
                    Text("বাতিল")
                }
            }
        )
    }

    // Delete Transaction Confirmation
    txToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("লেনদেনটি মুছে ফেলতে চান?", fontWeight = FontWeight.Bold) },
            text = { Text("টাকা: ${Formatters.formatTaka(tx.amount)}\nবিবরণ: ${tx.note.ifBlank { "বিবরণ নেই" }}") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(tx)
                        txToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { txToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun CustomerLedgerHeader(
    item: CustomerWithBalance,
    onSmsClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onShareStatement: () -> Unit
) {
    val customer = item.customer
    val initial = customer.name.firstOrNull()?.toString() ?: "ক"

    val (balanceColor, balanceBg, balanceBorder) = when {
        item.isReceivable -> Triple(KhataReceivableGreen, KhataReceivableBg, KhataReceivableBorder)
        item.isPayable -> Triple(KhataPayableRed, KhataPayableBg, KhataPayableBorder)
        else -> Triple(KhataSettledGray, KhataSettledBg, KhataSettledBorder)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .shadow(3.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, KhataCardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Profile Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(balanceColor.copy(alpha = 0.12f))
                        .border(1.5.dp, balanceColor.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = balanceColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = customer.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = KhataTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (customer.phone.isNotBlank()) {
                        Text(
                            text = customer.phone,
                            fontSize = 12.sp,
                            color = KhataTextSecondary
                        )
                    }
                    if (customer.address.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = KhataTextSecondary
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = customer.address,
                                fontSize = 11.sp,
                                color = KhataTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Balance Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(balanceBg)
                    .border(1.dp, balanceBorder, RoundedCornerShape(16.dp))
                    .padding(vertical = 14.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = item.statusTextBangla,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = balanceColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = Formatters.formatTaka(item.displayAmount),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = balanceColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Chips: WhatsApp, SMS, Statement
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // WhatsApp Reminder
                OutlinedButton(
                    onClick = onWhatsAppClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = WhatsAppGreen),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppGreen.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = WhatsAppGreen
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("হোয়াটসঅ্যাপ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = WhatsAppGreen)
                }

                // SMS Reminder
                OutlinedButton(
                    onClick = onSmsClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("detail_sms_reminder_button"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = KhataPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KhataPrimary.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Message,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = KhataPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("এসএমএস", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KhataPrimary)
                }

                // Share Statement
                OutlinedButton(
                    onClick = onShareStatement,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("detail_share_statement_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, KhataCardBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = KhataTextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("বিবরণী", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KhataTextPrimary)
                }
            }
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: Transaction,
    isSupplier: Boolean,
    onDeleteClick: () -> Unit
) {
    val isCredit = transaction.type == TransactionType.GAVE_CREDIT || transaction.type == TransactionType.TOOK_CREDIT
    val typeTitle = when (transaction.type) {
        TransactionType.GAVE_CREDIT -> "বাকিতে বিক্রি (পাবো)"
        TransactionType.GOT_PAYMENT -> "টাকা গ্রহণ (জমা)"
        TransactionType.TOOK_CREDIT -> "বাকিতে ক্রয় (দেবো)"
        TransactionType.GAVE_PAYMENT -> "টাকা পরিশোধ"
    }

    val typeColor = if (isCredit) KhataPayableRed else KhataReceivableGreen
    val icon = if (isCredit) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
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
            // Type indicator icon
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = typeColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typeTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = typeColor
                )

                if (transaction.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.note,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = KhataTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Formatters.formatDateTime(transaction.date),
                        fontSize = 11.sp,
                        color = KhataTextSecondary
                    )
                    if (transaction.billNumber.isNotBlank()) {
                        Text(
                            text = " • ভাউচার: ${transaction.billNumber}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KhataTextSecondary
                        )
                    }
                }
            }

            // Amount and Delete
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${if (isCredit) "+" else "-"} ${Formatters.formatTaka(transaction.amount)}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = typeColor
                )

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete entry",
                        tint = KhataTextSecondary.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun shareCustomerStatement(
    context: android.content.Context,
    item: CustomerWithBalance,
    transactions: List<Transaction>,
    shopName: String
) {
    val sb = StringBuilder()
    sb.appendLine("=================================")
    sb.appendLine("       ${shopName.ifBlank { "বাকির খাতা" }}")
    sb.appendLine("       গ্রাহক হিসাব বিবরণী")
    sb.appendLine("=================================")
    sb.appendLine("গ্রাহক: ${item.customer.name}")
    if (item.customer.phone.isNotBlank()) {
        sb.appendLine("মোবাইল: ${item.customer.phone}")
    }
    sb.appendLine("তারিখ: ${Formatters.formatDate(System.currentTimeMillis())}")
    sb.appendLine("---------------------------------")
    sb.appendLine("মোট বাকিতে প্রদান: ${Formatters.formatTaka(item.totalGiven)}")
    sb.appendLine("মোট জমা গ্রহণ: ${Formatters.formatTaka(item.totalReceived)}")
    sb.appendLine("---------------------------------")
    sb.appendLine("বর্তমান অবস্থা: ${item.statusTextBangla} ${Formatters.formatTaka(item.displayAmount)}")
    sb.appendLine("=================================")
    sb.appendLine("সর্বশেষ লেনদেনসমূহ:")
    transactions.take(10).forEach { tx ->
        val typeName = when (tx.type) {
            TransactionType.GAVE_CREDIT -> "বাকিতে বিক্রি"
            TransactionType.GOT_PAYMENT -> "জমা"
            TransactionType.TOOK_CREDIT -> "বাকিতে ক্রয়"
            TransactionType.GAVE_PAYMENT -> "পরিশোধ"
        }
        sb.appendLine("• ${Formatters.formatDate(tx.date)}: $typeName ${Formatters.formatTaka(tx.amount)} ${if (tx.note.isNotBlank()) "(${tx.note})" else ""}")
    }
    sb.appendLine("=================================")
    sb.appendLine("ধন্যবাদ!")

    ReminderHelper.shareText(context, "${item.customer.name}-এর হিসাব বিবরণী", sb.toString())
}
