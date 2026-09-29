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
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import com.example.data.model.CustomerType
import com.example.ui.theme.KhataPayableRed
import com.example.ui.theme.KhataPrimary
import com.example.ui.theme.KhataReceivableGreen

@Composable
fun AddCustomerDialog(
    onDismiss: () -> Unit,
    onAddCustomer: (name: String, phone: String, address: String, type: CustomerType, initialAmount: Double, isCredit: Boolean) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var customerType by remember { mutableStateOf(CustomerType.CUSTOMER) }
    var hasInitialDue by remember { mutableStateOf(false) }
    var initialAmountText by remember { mutableStateOf("") }
    var isReceivable by remember { mutableStateOf(true) }

    var nameError by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = KhataPrimary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "নতুন গ্রাহক / খাতা তৈরি করুন",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Customer Type Selection
                Text(
                    text = "খাতার ধরন:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Customer (ক্রেতা)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (customerType == CustomerType.CUSTOMER) 2.dp else 1.dp,
                                color = if (customerType == CustomerType.CUSTOMER) KhataPrimary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                                if (customerType == CustomerType.CUSTOMER) KhataPrimary.copy(alpha = 0.1f) else Color.Transparent
                            )
                            .clickable {
                                customerType = CustomerType.CUSTOMER
                                isReceivable = true
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (customerType == CustomerType.CUSTOMER) KhataPrimary else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "ক্রেতা / কাস্টমার",
                                fontSize = 13.sp,
                                fontWeight = if (customerType == CustomerType.CUSTOMER) FontWeight.Bold else FontWeight.Normal,
                                color = if (customerType == CustomerType.CUSTOMER) KhataPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Supplier (পাইকার)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (customerType == CustomerType.SUPPLIER) 2.dp else 1.dp,
                                color = if (customerType == CustomerType.SUPPLIER) KhataPrimary else MaterialTheme.colorScheme.outlineVariant,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .background(
                                if (customerType == CustomerType.SUPPLIER) KhataPrimary.copy(alpha = 0.1f) else Color.Transparent
                            )
                            .clickable {
                                customerType = CustomerType.SUPPLIER
                                isReceivable = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (customerType == CustomerType.SUPPLIER) KhataPrimary else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "সাপ্লায়ার / পাইকার",
                                fontSize = 13.sp,
                                fontWeight = if (customerType == CustomerType.SUPPLIER) FontWeight.Bold else FontWeight.Normal,
                                color = if (customerType == CustomerType.SUPPLIER) KhataPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Name field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    label = { Text("গ্রাহকের নাম *") },
                    placeholder = { Text("যেমন: আব্দুর রহিম") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                    isError = nameError,
                    supportingText = if (nameError) {
                        { Text("অনুগ্রহ করে নাম লিখুন", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mobile field
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("মোবাইল নম্বর") },
                    placeholder = { Text("যেমন: 01712345678") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_phone_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Address field
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("ঠিকানা (ঐচ্ছিক)") },
                    placeholder = { Text("যেমন: বাজার রোড, ঢাকা") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_address_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Initial Balance Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { hasInitialDue = !hasInitialDue }
                ) {
                    Checkbox(
                        checked = hasInitialDue,
                        onCheckedChange = { hasInitialDue = it }
                    )
                    Text(
                        text = "পূর্বের কোনো বাকি হিসাব আছে?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                if (hasInitialDue) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = initialAmountText,
                        onValueChange = { initialAmountText = it },
                        label = { Text("পূর্বের টাকার পরিমাণ (৳)") },
                        placeholder = { Text("যেমন: 500") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("initial_amount_input")
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isReceivable = true }
                        ) {
                            RadioButton(
                                selected = isReceivable,
                                onClick = { isReceivable = true }
                            )
                            Text(
                                text = "আপনি পাবেন",
                                fontSize = 13.sp,
                                color = KhataReceivableGreen,
                                fontWeight = if (isReceivable) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isReceivable = false }
                        ) {
                            RadioButton(
                                selected = !isReceivable,
                                onClick = { isReceivable = false }
                            )
                            Text(
                                text = "আপনি দেবেন",
                                fontSize = 13.sp,
                                color = KhataPayableRed,
                                fontWeight = if (!isReceivable) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    val amount = initialAmountText.toDoubleOrNull() ?: 0.0
                    onAddCustomer(
                        name.trim(),
                        phone.trim(),
                        address.trim(),
                        customerType,
                        amount,
                        isReceivable
                    )
                    onDismiss()
                },
                modifier = Modifier.testTag("save_customer_button"),
                colors = ButtonDefaults.buttonColors(containerColor = KhataPrimary)
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_customer_button")
            ) {
                Text("বাতিল")
            }
        }
    )
}
