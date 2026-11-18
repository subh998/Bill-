package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BusinessProfile
import com.example.data.model.UiState
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

@Composable
fun BusinessProfileScreen(
    profileState: UiState<BusinessProfile?>,
    isSaving: Boolean,
    onSaveProfile: (BusinessProfile) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser = Firebase.auth.currentUser
    val existingProfile = (profileState as? UiState.Success)?.data

    var companyName by remember { mutableStateOf("") }
    var companyAddress by remember { mutableStateOf("") }
    var companyState by remember { mutableStateOf("") }
    var companyPinCode by remember { mutableStateOf("") }
    var companyGstin by remember { mutableStateOf("") }
    var cinNo by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf("") }
    var bankAccountNumber by remember { mutableStateOf("") }
    var bankIfsc by remember { mutableStateOf("") }
    var authorisedSignatory by remember { mutableStateOf("") }
    var termsAndConditions by remember { mutableStateOf("") }
    var defaultQtyUnit by remember { mutableStateOf("MT") }
    var nextInvoiceNumber by remember { mutableStateOf("1") }
    var initializedForId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(existingProfile) {
        if (existingProfile != null && initializedForId != existingProfile.id + existingProfile.updatedAt.toString()) {
            companyName = existingProfile.companyName
            companyAddress = existingProfile.companyAddress
            companyState = existingProfile.companyState
            companyPinCode = existingProfile.companyPinCode
            companyGstin = existingProfile.companyGstin
            cinNo = existingProfile.cinNo
            bankName = existingProfile.bankName
            bankAccountNumber = existingProfile.bankAccountNumber
            bankIfsc = existingProfile.bankIfsc
            authorisedSignatory = existingProfile.authorisedSignatory
            termsAndConditions = existingProfile.termsAndConditions
            defaultQtyUnit = existingProfile.defaultQtyUnit.ifBlank { "MT" }
            nextInvoiceNumber = existingProfile.nextInvoiceNumber.coerceAtLeast(1L).toString()
            initializedForId = existingProfile.id + existingProfile.updatedAt.toString()
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Business Profile",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = "Saved once and automatically filled on every new Bill of Supply.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (profileState is UiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                if (profileState is UiState.Error) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = profileState.message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Seller Details Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Business,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Company & GST Details", style = MaterialTheme.typography.titleMedium)
                        }

                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Company Name * (e.g. BALAJEE TRADERS)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth().testTag("profile_company_name_input")
                        )
                        OutlinedTextField(
                            value = companyAddress,
                            onValueChange = { companyAddress = it },
                            label = { Text("Shop / Office Address") },
                            maxLines = 3,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = companyState,
                                onValueChange = { companyState = it },
                                label = { Text("State") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier.weight(1.3f)
                            )
                            OutlinedTextField(
                                value = companyPinCode,
                                onValueChange = { companyPinCode = it.take(12) },
                                label = { Text("Pin Code") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = companyGstin,
                                onValueChange = { companyGstin = it.uppercase().take(20) },
                                label = { Text("Company GSTIN") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier.weight(1.3f)
                            )
                            OutlinedTextField(
                                value = cinNo,
                                onValueChange = { cinNo = it.uppercase().take(30) },
                                label = { Text("CIN NO") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Bank & Signatory Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.AccountBalance,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text("Bank & Signatory", style = MaterialTheme.typography.titleMedium)
                        }

                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name / Account Title") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = bankAccountNumber,
                                onValueChange = { bankAccountNumber = it.take(40) },
                                label = { Text("Bank A/C No.") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.3f)
                            )
                            OutlinedTextField(
                                value = bankIfsc,
                                onValueChange = { bankIfsc = it.uppercase().take(20) },
                                label = { Text("Bank IFSC") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = authorisedSignatory,
                            onValueChange = { authorisedSignatory = it },
                            label = { Text("Authorised Signatory Name") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = termsAndConditions,
                            onValueChange = { termsAndConditions = it.take(400) },
                            label = { Text("Default Terms & Conditions") },
                            maxLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text("Billing Defaults", style = MaterialTheme.typography.titleSmall)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = nextInvoiceNumber,
                                onValueChange = { nextInvoiceNumber = it.filter(Char::isDigit).take(7) },
                                label = { Text("Next Invoice No.") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            Column(modifier = Modifier.weight(1.4f)) {
                                Text(
                                    "Default Qty Unit",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("MT", "KG", "QTL").forEach { unit ->
                                        FilterChip(
                                            selected = defaultQtyUnit == unit,
                                            onClick = { defaultQtyUnit = unit },
                                            label = { Text(unit) },
                                            leadingIcon = if (defaultQtyUnit == unit) {
                                                {
                                                    Icon(
                                                        Icons.Filled.Check,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            } else null
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                val nextNum = nextInvoiceNumber.toLongOrNull()?.coerceIn(1L, 9_999_999L) ?: 1L
                                onSaveProfile(
                                    (existingProfile ?: BusinessProfile()).copy(
                                        companyName = companyName.trim(),
                                        companyAddress = companyAddress.trim(),
                                        companyState = companyState.trim(),
                                        companyPinCode = companyPinCode.trim(),
                                        companyGstin = companyGstin.trim().uppercase(),
                                        cinNo = cinNo.trim().uppercase(),
                                        bankName = bankName.trim(),
                                        bankAccountNumber = bankAccountNumber.trim(),
                                        bankIfsc = bankIfsc.trim().uppercase(),
                                        authorisedSignatory = authorisedSignatory.trim(),
                                        termsAndConditions = termsAndConditions.trim(),
                                        defaultQtyUnit = defaultQtyUnit.trim().ifBlank { "MT" },
                                        nextInvoiceNumber = nextNum
                                    )
                                )
                            },
                            enabled = !isSaving && companyName.isNotBlank(),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("save_business_profile_button")
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Filled.Save, contentDescription = null)
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Save Business Profile")
                            }
                        }
                    }
                }
            }

            // Account card
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.displayName ?: "Signed In",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = currentUser?.email ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Sign Out")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
