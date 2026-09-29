package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Customer
import com.example.data.model.CustomerType
import com.example.data.model.TransactionType
import com.example.ui.theme.KhataPayableRed
import com.example.ui.theme.KhataPrimary
import com.example.ui.theme.KhataReceivableGreen

@Composable
fun AddTransactionDialog(
    customer: Customer,
    initialType: TransactionType? = null,
    onDismiss: () -> Unit,
    onSaveTransaction: (amount: Double, type: TransactionType, note: String, billNumber: String, date: Long) -> Unit
) {
    val isSupplier = customer.customerType == CustomerType.SUPPLIER

    val defaultType = initialType ?: if (!isSupplier) TransactionType.GAVE_CREDIT else TransactionType.TOOK_CREDIT
    var selectedType by remember { mutableStateOf(defaultType) }

    var amountText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var billNumber by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "নতুন লেনদেন এন্ট্রি",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "গ্রাহক: ${customer.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Type selector
                Text(
                    text = "লেনদেনের প্রকৃতি:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isSupplier) {
                        // Customer: Gave Credit (বাকিতে বিক্রি / দিলেন)
                        val isGave = selectedType == TransactionType.GAVE_CREDIT
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isGave) 2.dp else 1.dp,
                                    color = if (isGave) KhataPayableRed else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(if (isGave) KhataPayableRed.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { selectedType = TransactionType.GAVE_CREDIT }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isGave) KhataPayableRed else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "বাকিতে দিলাম",
                                    fontSize = 12.sp,
                                    fontWeight = if (isGave) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isGave) KhataPayableRed else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "(আপনি পাবেন)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Customer: Got Payment (টাকা পেলাম / জমা)
                        val isGot = selectedType == TransactionType.GOT_PAYMENT
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isGot) 2.dp else 1.dp,
                                    color = if (isGot) KhataReceivableGreen else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(if (isGot) KhataReceivableGreen.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { selectedType = TransactionType.GOT_PAYMENT }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isGot) KhataReceivableGreen else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "টাকা পেলাম",
                                    fontSize = 12.sp,
                                    fontWeight = if (isGot) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isGot) KhataReceivableGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "(জমা নিল)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    } else {
                        // Supplier: Took Credit (বাকিতে মাল নিলাম)
                        val isTook = selectedType == TransactionType.TOOK_CREDIT
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isTook) 2.dp else 1.dp,
                                    color = if (isTook) KhataPayableRed else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(if (isTook) KhataPayableRed.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { selectedType = TransactionType.TOOK_CREDIT }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = if (isTook) KhataPayableRed else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "বাকিতে নিলাম",
                                    fontSize = 12.sp,
                                    fontWeight = if (isTook) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isTook) KhataPayableRed else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "(আপনি দেবেন)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }

                        // Supplier: Gave Payment (টাকা পরিশোধ করলাম)
                        val isPaid = selectedType == TransactionType.GAVE_PAYMENT
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isPaid) 2.dp else 1.dp,
                                    color = if (isPaid) KhataReceivableGreen else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(if (isPaid) KhataReceivableGreen.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable { selectedType = TransactionType.GAVE_PAYMENT }
                                .padding(vertical = 10.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.ArrowUpward,
                                    contentDescription = null,
                                    tint = if (isPaid) KhataReceivableGreen else MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "টাকা পরিশোধ",
                                    fontSize = 12.sp,
                                    fontWeight = if (isPaid) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isPaid) KhataReceivableGreen else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "(বাকি কমবে)",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if (it.isNotBlank()) amountError = false
                    },
                    label = { Text("টাকার পরিমাণ (৳) *") },
                    placeholder = { Text("যেমন: 1250") },
                    leadingIcon = {
                        Text(
                            text = "৳",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = KhataPrimary,
                            modifier = Modifier.padding(start = 12.dp, end = 4.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amountError,
                    supportingText = if (amountError) {
                        { Text("সঠিক টাকার অংক লিখুন", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Note / Details
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("মালের বিবরণ বা নোট (ঐচ্ছিক)") },
                    placeholder = { Text("যেমন: মিনিকেট চাল ১ বস্তা, তেল ৫ লিটার") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_note_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Bill / Voucher number
                OutlinedTextField(
                    value = billNumber,
                    onValueChange = { billNumber = it },
                    label = { Text("মেমো / বিল নং (ঐচ্ছিক)") },
                    placeholder = { Text("যেমন: মেমো #১০২") },
                    leadingIcon = { Icon(Icons.Default.Receipt, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_bill_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        amountError = true
                        return@Button
                    }
                    onSaveTransaction(
                        amount,
                        selectedType,
                        note.trim(),
                        billNumber.trim(),
                        System.currentTimeMillis()
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("save_transaction_button"),
                colors = ButtonDefaults.buttonColors(containerColor = KhataPrimary)
            ) {
                Text("যুক্ত করুন")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_transaction_button")
            ) {
                Text("বাতিল")
            }
        }
    )
}
