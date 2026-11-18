package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BillLineItem
import com.example.data.model.BusinessProfile
import com.example.data.model.CopyType
import com.example.data.model.DocumentType
import com.example.data.model.Party
import com.example.data.model.SupplyBill
import com.example.export.NumberFormatters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class EditableItemRow(
    val id: Long,
    val materialDescription: String = "",
    val hsnCode: String = "",
    val numberOfBags: String = "",
    val qtyText: String = "",
    val rateText: String = ""
) {
    val qtyValue: Double get() = qtyText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val rateValue: Double get() = rateText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val amountValue: Double get() = qtyValue * rateValue
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BillEditorScreen(
    initialBill: SupplyBill?,
    businessProfile: BusinessProfile?,
    savedParties: List<Party>,
    isSaving: Boolean,
    onBack: () -> Unit,
    onSaveAndPreview: (
        bill: SupplyBill,
        incrementNextInvoiceNumber: Long?,
        saveReceiverAsParty: Boolean
    ) -> Unit
) {
    val isEditingExisting = initialBill != null && initialBill.id.isNotBlank()
    val todayFormatted = remember {
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date())
    }
    val suggestedInvoiceNo = remember(initialBill, businessProfile) {
        initialBill?.invoiceNumber?.takeIf { it.isNotBlank() }
            ?: (businessProfile?.nextInvoiceNumber ?: 1L).toString()
    }

    var documentType by remember {
        mutableStateOf(initialBill?.resolvedDocumentType() ?: DocumentType.BILL_OF_SUPPLY)
    }
    var copyType by remember {
        mutableStateOf(initialBill?.resolvedCopyType() ?: CopyType.ORIGINAL)
    }
    var invoiceNumber by remember { mutableStateOf(suggestedInvoiceNo) }
    var invoiceDate by remember { mutableStateOf(initialBill?.invoiceDate?.ifBlank { todayFormatted } ?: todayFormatted) }
    var reverseCharge by remember { mutableStateOf(initialBill?.reverseCharge ?: "N") }
    var placeState by remember {
        mutableStateOf(initialBill?.placeState ?: businessProfile?.companyState.orEmpty())
    }
    var placePinCode by remember {
        mutableStateOf(initialBill?.placePinCode ?: businessProfile?.companyPinCode.orEmpty())
    }

    // Transport
    var transportMode by remember { mutableStateOf(initialBill?.transportMode?.ifBlank { "By Road" } ?: "By Road") }
    var vehicleNumber by remember { mutableStateOf(initialBill?.vehicleNumber.orEmpty()) }
    var transporterName by remember { mutableStateOf(initialBill?.transporterName.orEmpty()) }
    var driverPhone by remember { mutableStateOf(initialBill?.driverPhone.orEmpty()) }
    var bhadaText by remember {
        mutableStateOf(
            initialBill?.bhadaAmount?.takeIf { it > 0.0 }?.let { trimDecimalZero(it) }.orEmpty()
        )
    }
    var truckAdvanceText by remember {
        mutableStateOf(
            initialBill?.truckAdvance?.takeIf { it > 0.0 }?.let { trimDecimalZero(it) }.orEmpty()
        )
    }

    // Receiver
    var receiverName by remember { mutableStateOf(initialBill?.receiverName.orEmpty()) }
    var receiverAddress by remember { mutableStateOf(initialBill?.receiverAddress.orEmpty()) }
    var receiverPoNo by remember { mutableStateOf(initialBill?.receiverPoNo.orEmpty()) }
    var receiverGstin by remember { mutableStateOf(initialBill?.receiverGstin.orEmpty()) }
    var receiverState by remember { mutableStateOf(initialBill?.receiverState.orEmpty()) }
    var receiverPinCode by remember { mutableStateOf(initialBill?.receiverPinCode.orEmpty()) }
    var saveReceiverAsParty by remember { mutableStateOf(!isEditingExisting) }

    // Consignee
    var consigneeName by remember { mutableStateOf(initialBill?.consigneeName.orEmpty()) }
    var consigneeAddress by remember { mutableStateOf(initialBill?.consigneeAddress.orEmpty()) }
    var consigneeGstin by remember { mutableStateOf(initialBill?.consigneeGstin.orEmpty()) }
    var consigneeState by remember { mutableStateOf(initialBill?.consigneeState.orEmpty()) }
    var consigneePinCode by remember { mutableStateOf(initialBill?.consigneePinCode.orEmpty()) }

    // Seller & Bank (prefilled from BusinessProfile or initialBill)
    var sellerName by remember {
        mutableStateOf(initialBill?.sellerName?.ifBlank { null } ?: businessProfile?.companyName.orEmpty())
    }
    var sellerAddress by remember {
        mutableStateOf(initialBill?.sellerAddress?.ifBlank { null } ?: businessProfile?.companyAddress.orEmpty())
    }
    var sellerState by remember {
        mutableStateOf(initialBill?.sellerState?.ifBlank { null } ?: businessProfile?.companyState.orEmpty())
    }
    var sellerPinCode by remember {
        mutableStateOf(initialBill?.sellerPinCode?.ifBlank { null } ?: businessProfile?.companyPinCode.orEmpty())
    }
    var sellerGstin by remember {
        mutableStateOf(initialBill?.sellerGstin?.ifBlank { null } ?: businessProfile?.companyGstin.orEmpty())
    }
    var sellerCinNo by remember {
        mutableStateOf(initialBill?.sellerCinNo?.ifBlank { null } ?: businessProfile?.cinNo.orEmpty())
    }
    var bankName by remember {
        mutableStateOf(initialBill?.bankName?.ifBlank { null } ?: businessProfile?.bankName.orEmpty())
    }
    var bankAccountNumber by remember {
        mutableStateOf(initialBill?.bankAccountNumber?.ifBlank { null } ?: businessProfile?.bankAccountNumber.orEmpty())
    }
    var bankIfsc by remember {
        mutableStateOf(initialBill?.bankIfsc?.ifBlank { null } ?: businessProfile?.bankIfsc.orEmpty())
    }
    var authorisedSignatory by remember {
        mutableStateOf(initialBill?.authorisedSignatory?.ifBlank { null } ?: businessProfile?.authorisedSignatory.orEmpty())
    }
    var termsAndConditions by remember {
        mutableStateOf(initialBill?.termsAndConditions?.ifBlank { null } ?: businessProfile?.termsAndConditions.orEmpty())
    }

    // Items & Taxes
    var qtyUnit by remember {
        val raw = initialBill?.qtyUnitLabel?.removePrefix("in ")?.trim()
        mutableStateOf(raw?.ifBlank { null } ?: businessProfile?.defaultQtyUnit?.ifBlank { null } ?: "MT")
    }
    val itemRows = remember {
        mutableStateListOf<EditableItemRow>().apply {
            val seed = initialBill?.items.orEmpty()
            if (seed.isEmpty()) {
                add(EditableItemRow(id = 1L))
            } else {
                seed.forEachIndexed { idx, item ->
                    add(
                        EditableItemRow(
                            id = idx.toLong() + 1L,
                            materialDescription = item.materialDescription,
                            hsnCode = item.hsnCode,
                            numberOfBags = item.numberOfBags,
                            qtyText = if (item.qty > 0.0) trimDecimalZero(item.qty) else "",
                            rateText = if (item.rate > 0.0) trimDecimalZero(item.rate) else ""
                        )
                    )
                }
            }
        }
    }
    var nextRowKey by remember { mutableStateOf(itemRows.size.toLong() + 2L) }

    var cgstText by remember {
        mutableStateOf(initialBill?.cgstRate?.takeIf { it > 0.0 }?.let { trimDecimalZero(it) } ?: "0")
    }
    var sgstText by remember {
        mutableStateOf(initialBill?.sgstRate?.takeIf { it > 0.0 }?.let { trimDecimalZero(it) } ?: "0")
    }
    var igstText by remember {
        mutableStateOf(initialBill?.igstRate?.takeIf { it > 0.0 }?.let { trimDecimalZero(it) } ?: "0")
    }

    var showSellerSection by remember { mutableStateOf(sellerName.isBlank()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPartyPickerForReceiver by remember { mutableStateOf<Boolean?>(null) } // true=receiver, false=consignee
    var validationError by remember { mutableStateOf<String?>(null) }
    var prefilledFromProfile by remember { mutableStateOf(businessProfile != null) }

    androidx.compose.runtime.LaunchedEffect(businessProfile) {
        if (!isEditingExisting && initialBill == null && !prefilledFromProfile && businessProfile != null) {
            prefilledFromProfile = true
            if (sellerName.isBlank()) sellerName = businessProfile.companyName
            if (sellerAddress.isBlank()) sellerAddress = businessProfile.companyAddress
            if (sellerState.isBlank()) sellerState = businessProfile.companyState
            if (sellerPinCode.isBlank()) sellerPinCode = businessProfile.companyPinCode
            if (sellerGstin.isBlank()) sellerGstin = businessProfile.companyGstin
            if (sellerCinNo.isBlank()) sellerCinNo = businessProfile.cinNo
            if (bankName.isBlank()) bankName = businessProfile.bankName
            if (bankAccountNumber.isBlank()) bankAccountNumber = businessProfile.bankAccountNumber
            if (bankIfsc.isBlank()) bankIfsc = businessProfile.bankIfsc
            if (authorisedSignatory.isBlank()) authorisedSignatory = businessProfile.authorisedSignatory
            if (termsAndConditions.isBlank()) termsAndConditions = businessProfile.termsAndConditions
            if (placeState.isBlank()) placeState = businessProfile.companyState
            if (placePinCode.isBlank()) placePinCode = businessProfile.companyPinCode
            if (invoiceNumber == "1") invoiceNumber = businessProfile.nextInvoiceNumber.coerceAtLeast(1L).toString()
            if (businessProfile.defaultQtyUnit.isNotBlank()) qtyUnit = businessProfile.defaultQtyUnit
            if (sellerName.isNotBlank()) showSellerSection = false
        }
    }

    val totalBeforeTax = itemRows.sumOf { it.amountValue }
    val cgstRate = cgstText.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
    val sgstRate = sgstText.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
    val igstRate = igstText.toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
    val totalTaxAmount = totalBeforeTax * (cgstRate + sgstRate + igstRate) / 100.0
    val totalAfterTax = totalBeforeTax + totalTaxAmount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (isEditingExisting) "Edit Bill #${initialBill?.invoiceNumber}" else "New Bill of Supply")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total After Tax",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "₹ ${NumberFormatters.formatAmount(totalAfterTax)}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = {
                            val validItems = itemRows.filter { it.materialDescription.isNotBlank() }
                            val invNo = invoiceNumber.trim()
                            when {
                                sellerName.isBlank() -> {
                                    showSellerSection = true
                                    validationError = "Please enter your Seller / Company Name."
                                }
                                invNo.isBlank() -> validationError = "Please enter an Invoice Number."
                                receiverName.isBlank() -> validationError = "Please enter Receiver (Billed To) Name."
                                validItems.isEmpty() -> validationError = "Please enter at least one Material Description."
                                else -> {
                                    validationError = null
                                    val builtItems = validItems.mapIndexed { idx, row ->
                                        BillLineItem(
                                            serialNo = idx + 1,
                                            materialDescription = row.materialDescription.trim(),
                                            hsnCode = row.hsnCode.trim(),
                                            numberOfBags = row.numberOfBags.trim(),
                                            qty = row.qtyValue,
                                            rate = row.rateValue,
                                            amount = row.amountValue
                                        )
                                    }
                                    val bill = SupplyBill(
                                        id = initialBill?.id.orEmpty(),
                                        userId = initialBill?.userId.orEmpty(),
                                        documentType = documentType.label,
                                        copyType = copyType.name,
                                        invoiceNumber = invNo,
                                        invoiceDate = invoiceDate.trim().ifBlank { todayFormatted },
                                        reverseCharge = reverseCharge,
                                        placeState = placeState.trim(),
                                        placePinCode = placePinCode.trim(),
                                        transportMode = transportMode.trim(),
                                        vehicleNumber = vehicleNumber.trim().uppercase(),
                                        transporterName = transporterName.trim(),
                                        driverPhone = driverPhone.trim(),
                                        bhadaAmount = bhadaText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                                        truckAdvance = truckAdvanceText.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
                                        sellerName = sellerName.trim(),
                                        sellerAddress = sellerAddress.trim(),
                                        sellerState = sellerState.trim(),
                                        sellerPinCode = sellerPinCode.trim(),
                                        sellerGstin = sellerGstin.trim().uppercase(),
                                        sellerCinNo = sellerCinNo.trim().uppercase(),
                                        bankName = bankName.trim(),
                                        bankAccountNumber = bankAccountNumber.trim(),
                                        bankIfsc = bankIfsc.trim().uppercase(),
                                        authorisedSignatory = authorisedSignatory.trim(),
                                        termsAndConditions = termsAndConditions.trim(),
                                        receiverName = receiverName.trim(),
                                        receiverAddress = receiverAddress.trim(),
                                        receiverPoNo = receiverPoNo.trim(),
                                        receiverGstin = receiverGstin.trim().uppercase(),
                                        receiverState = receiverState.trim(),
                                        receiverPinCode = receiverPinCode.trim(),
                                        consigneeName = consigneeName.trim(),
                                        consigneeAddress = consigneeAddress.trim(),
                                        consigneeGstin = consigneeGstin.trim().uppercase(),
                                        consigneeState = consigneeState.trim(),
                                        consigneePinCode = consigneePinCode.trim(),
                                        qtyUnitLabel = if (qtyUnit.isNotBlank()) "in $qtyUnit" else "",
                                        items = builtItems,
                                        totalBeforeTax = totalBeforeTax,
                                        cgstRate = cgstRate,
                                        sgstRate = sgstRate,
                                        igstRate = igstRate,
                                        totalTaxAmount = totalTaxAmount,
                                        totalAfterTax = totalAfterTax
                                    )
                                    val nextCounter = if (!isEditingExisting) {
                                        invNo.toLongOrNull()?.plus(1L)?.coerceIn(1L, 9_999_999L)
                                    } else null
                                    onSaveAndPreview(bill, nextCounter, saveReceiverAsParty)
                                }
                            }
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("save_and_preview_bill_button")
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Save & Preview")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (validationError != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = validationError.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // 1. INVOICE & COPY HEADER
                FormSectionCard(title = "Invoice & Copy Type") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DocumentType.entries.forEach { type ->
                            FilterChip(
                                selected = documentType == type,
                                onClick = { documentType = type },
                                label = { Text(type.label) },
                                leadingIcon = if (documentType == type) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }

                    Text(
                        text = "Copy Label",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CopyType.entries.forEach { copy ->
                            FilterChip(
                                selected = copyType == copy,
                                onClick = { copyType = copy },
                                label = { Text(copy.label) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = invoiceNumber,
                            onValueChange = { invoiceNumber = it.take(40) },
                            label = { Text("Invoice No *") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("bill_invoice_no_input")
                        )
                        OutlinedTextField(
                            value = invoiceDate,
                            onValueChange = { invoiceDate = it.take(30) },
                            label = { Text("Invoice Date *") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(Icons.Filled.CalendarToday, contentDescription = "Pick date")
                                }
                            },
                            modifier = Modifier.weight(1.3f)
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = placeState,
                            onValueChange = { placeState = it.take(60) },
                            label = { Text("State") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = placePinCode,
                            onValueChange = { placePinCode = it.take(12) },
                            label = { Text("Pin Code") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.9f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Rev Chg", style = MaterialTheme.typography.labelSmall)
                            FilterChip(
                                selected = reverseCharge == "Y",
                                onClick = { reverseCharge = if (reverseCharge == "Y") "N" else "Y" },
                                label = { Text(reverseCharge) }
                            )
                        }
                    }
                }

                // 2. RECEIVER (BILLED TO)
                FormSectionCard(
                    title = "Detail of Receiver (Billed to)",
                    trailingAction = {
                        if (savedParties.isNotEmpty()) {
                            TextButton(onClick = { showPartyPickerForReceiver = true }) {
                                Icon(Icons.Filled.People, contentDescription = null, Modifier.size(16.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Pick Party")
                            }
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = receiverName,
                        onValueChange = { receiverName = it },
                        label = { Text("Receiver Name * (e.g. Janki Traders)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth().testTag("bill_receiver_name_input")
                    )
                    OutlinedTextField(
                        value = receiverAddress,
                        onValueChange = { receiverAddress = it },
                        label = { Text("Address / Delivery At") },
                        maxLines = 2,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = receiverGstin,
                            onValueChange = { receiverGstin = it.uppercase().take(20) },
                            label = { Text("ITC GSTIN") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = receiverPoNo,
                            onValueChange = { receiverPoNo = it.take(50) },
                            label = { Text("PO No.") },
                            singleLine = true,
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = receiverState,
                            onValueChange = { receiverState = it.take(60) },
                            label = { Text("State") },
                            singleLine = true,
                            modifier = Modifier.weight(1.3f)
                        )
                        OutlinedTextField(
                            value = receiverPinCode,
                            onValueChange = { receiverPinCode = it.take(12) },
                            label = { Text("Pin Code") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.9f)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { saveReceiverAsParty = !saveReceiverAsParty }
                    ) {
                        Checkbox(
                            checked = saveReceiverAsParty,
                            onCheckedChange = { saveReceiverAsParty = it }
                        )
                        Text(
                            text = "Save receiver to Parties directory for 1-tap reuse",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // 3. CONSIGNEE (SHIPPED TO)
                FormSectionCard(
                    title = "Detail of Consignee (Shipped to)",
                    trailingAction = {
                        Row {
                            if (savedParties.isNotEmpty()) {
                                TextButton(onClick = { showPartyPickerForReceiver = false }) {
                                    Text("Pick")
                                }
                            }
                            TextButton(
                                onClick = {
                                    consigneeName = receiverName
                                    consigneeAddress = receiverAddress
                                    consigneeGstin = receiverGstin
                                    consigneeState = receiverState
                                    consigneePinCode = receiverPinCode
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, Modifier.size(15.dp))
                                Spacer(Modifier.size(4.dp))
                                Text("Same as Receiver")
                            }
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = consigneeName,
                        onValueChange = { consigneeName = it },
                        label = { Text("Consignee Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = consigneeAddress,
                        onValueChange = { consigneeAddress = it },
                        label = { Text("Consignee Address") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = consigneeGstin,
                            onValueChange = { consigneeGstin = it.uppercase().take(20) },
                            label = { Text("GSTIN") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = consigneeState,
                            onValueChange = { consigneeState = it.take(60) },
                            label = { Text("State") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = consigneePinCode,
                            onValueChange = { consigneePinCode = it.take(12) },
                            label = { Text("Pin") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.75f)
                        )
                    }
                }

                // 4. MATERIALS / LINE ITEMS
                FormSectionCard(title = "Materials & Line Items") {
                    Text(
                        text = "Quantity Unit Header",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("MT", "KG", "QTL", "BAG", "PCS").forEach { unit ->
                            FilterChip(
                                selected = qtyUnit == unit,
                                onClick = { qtyUnit = unit },
                                label = { Text("in $unit") }
                            )
                        }
                    }

                    itemRows.forEachIndexed { idx, row ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Item #${idx + 1}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "₹ ${NumberFormatters.formatAmount(row.amountValue)}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (itemRows.size > 1) {
                                            IconButton(
                                                onClick = { itemRows.removeAt(idx) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    Icons.Filled.Delete,
                                                    contentDescription = "Remove item",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = row.materialDescription,
                                    onValueChange = {
                                        itemRows[idx] = row.copy(materialDescription = it)
                                    },
                                    label = { Text("Material Description * (e.g. Rice)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("bill_item_desc_$idx")
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = row.hsnCode,
                                        onValueChange = { itemRows[idx] = row.copy(hsnCode = it.take(20)) },
                                        label = { Text("HSN") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = row.numberOfBags,
                                        onValueChange = { itemRows[idx] = row.copy(numberOfBags = it.take(20)) },
                                        label = { Text("Bags") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = row.qtyText,
                                        onValueChange = { input ->
                                            if (isDecimalInput(input)) {
                                                itemRows[idx] = row.copy(qtyText = input)
                                            }
                                        },
                                        label = { Text("Qty ($qtyUnit) *") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("bill_item_qty_$idx")
                                    )
                                    OutlinedTextField(
                                        value = row.rateText,
                                        onValueChange = { input ->
                                            if (isDecimalInput(input)) {
                                                itemRows[idx] = row.copy(rateText = input)
                                            }
                                        },
                                        label = { Text("Rate (₹) *") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("bill_item_rate_$idx")
                                    )
                                }
                            }
                        }
                    }

                    if (itemRows.size < 25) {
                        OutlinedButton(
                            onClick = {
                                itemRows.add(EditableItemRow(id = nextRowKey++))
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(Modifier.size(6.dp))
                            Text("Add Another Material")
                        }
                    }
                }

                // 5. GST TAXES & TRANSPORT
                FormSectionCard(title = "Taxes & Transport Details") {
                    Text(
                        text = "Quick GST Preset",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(
                            "0% (Exempt)" to Triple("0", "0", "0"),
                            "5% (2.5+2.5)" to Triple("2.5", "2.5", "0"),
                            "5% IGST" to Triple("0", "0", "5"),
                            "12% (6+6)" to Triple("6", "6", "0"),
                            "18% (9+9)" to Triple("9", "9", "0")
                        ).forEach { (label, rates) ->
                            FilterChip(
                                selected = cgstText == rates.first && sgstText == rates.second && igstText == rates.third,
                                onClick = {
                                    cgstText = rates.first
                                    sgstText = rates.second
                                    igstText = rates.third
                                },
                                label = { Text(label) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cgstText,
                            onValueChange = { if (isDecimalInput(it)) cgstText = it },
                            label = { Text("CGST %") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sgstText,
                            onValueChange = { if (isDecimalInput(it)) sgstText = it },
                            label = { Text("SGST %") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = igstText,
                            onValueChange = { if (isDecimalInput(it)) igstText = it },
                            label = { Text("IGST %") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = transportMode,
                            onValueChange = { transportMode = it.take(50) },
                            label = { Text("Transport Mode") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = vehicleNumber,
                            onValueChange = { vehicleNumber = it.uppercase().take(30) },
                            label = { Text("Vehicle No.") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = transporterName,
                            onValueChange = { transporterName = it.take(100) },
                            label = { Text("Transporter Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                        OutlinedTextField(
                            value = driverPhone,
                            onValueChange = { driverPhone = it.take(25) },
                            label = { Text("Driver Phone") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = bhadaText,
                            onValueChange = { if (isDecimalInput(it)) bhadaText = it },
                            label = { Text("Bhada (₹)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = truckAdvanceText,
                            onValueChange = { if (isDecimalInput(it)) truckAdvanceText = it },
                            label = { Text("Truck Advance (₹)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 6. SELLER & BANK OVERRIDE (COLLAPSIBLE)
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSellerSection = !showSellerSection },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Seller & Bank Details", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = sellerName.ifBlank { "Required: Enter your shop / company name" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (sellerName.isBlank()) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                            Icon(
                                imageVector = if (showSellerSection) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Toggle seller details"
                            )
                        }

                        AnimatedVisibility(visible = showSellerSection) {
                            Column(
                                modifier = Modifier.padding(top = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = sellerName,
                                    onValueChange = { sellerName = it },
                                    label = { Text("Company Name *") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                                    modifier = Modifier.fillMaxWidth().testTag("bill_seller_name_input")
                                )
                                OutlinedTextField(
                                    value = sellerAddress,
                                    onValueChange = { sellerAddress = it },
                                    label = { Text("Company Address") },
                                    maxLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = sellerState,
                                        onValueChange = { sellerState = it },
                                        label = { Text("State") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    OutlinedTextField(
                                        value = sellerPinCode,
                                        onValueChange = { sellerPinCode = it.take(12) },
                                        label = { Text("Pin") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(0.9f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = sellerGstin,
                                        onValueChange = { sellerGstin = it.uppercase().take(20) },
                                        label = { Text("Seller GSTIN") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    OutlinedTextField(
                                        value = sellerCinNo,
                                        onValueChange = { sellerCinNo = it.uppercase().take(30) },
                                        label = { Text("CIN NO") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = bankName,
                                    onValueChange = { bankName = it },
                                    label = { Text("Bank Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = bankAccountNumber,
                                        onValueChange = { bankAccountNumber = it.take(40) },
                                        label = { Text("Bank A/C") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1.2f)
                                    )
                                    OutlinedTextField(
                                        value = bankIfsc,
                                        onValueChange = { bankIfsc = it.uppercase().take(20) },
                                        label = { Text("Bank IFSC") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = authorisedSignatory,
                                    onValueChange = { authorisedSignatory = it },
                                    label = { Text("Authorised Signatory") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = termsAndConditions,
                                    onValueChange = { termsAndConditions = it.take(400) },
                                    label = { Text("Terms & Conditions") },
                                    maxLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            invoiceDate = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val pickerTarget = showPartyPickerForReceiver
    if (pickerTarget != null) {
        ModalBottomSheet(onDismissRequest = { showPartyPickerForReceiver = null }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (pickerTarget) "Select Receiver (Billed To)" else "Select Consignee (Shipped To)",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(savedParties, key = { it.id }) { party ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (pickerTarget) {
                                        receiverName = party.name
                                        receiverAddress = party.address
                                        receiverGstin = party.gstin
                                        receiverState = party.state
                                        receiverPinCode = party.pinCode
                                        saveReceiverAsParty = false
                                    } else {
                                        consigneeName = party.name
                                        consigneeAddress = party.address
                                        consigneeGstin = party.gstin
                                        consigneeState = party.state
                                        consigneePinCode = party.pinCode
                                    }
                                    showPartyPickerForReceiver = null
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(party.name, style = MaterialTheme.typography.titleMedium)
                                if (party.gstin.isNotBlank()) {
                                    Text(
                                        "GSTIN: ${party.gstin}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (party.address.isNotBlank()) {
                                    Text(
                                        party.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    trailingAction: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                trailingAction?.invoke()
            }
            content()
        }
    }
}

private fun isDecimalInput(value: String): Boolean {
    if (value.isEmpty()) return true
    return value.matches(Regex("^\\d*\\.?\\d*$")) && value.length <= 16
}

private fun trimDecimalZero(value: Double): String {
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}
