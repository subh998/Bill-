package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class CopyType(val label: String) {
    ORIGINAL("Original"),
    DUPLICATE("Duplicate"),
    TRIPLICATE("Triplicate"),
    EXTRA_OC("Extra OC");

    companion object {
        fun fromRaw(raw: String): CopyType =
            entries.firstOrNull { it.name == raw } ?: ORIGINAL
    }
}

enum class DocumentType(val label: String) {
    BILL_OF_SUPPLY("Bill of Supply"),
    TAX_INVOICE("Tax Invoice");

    companion object {
        fun fromLabel(raw: String): DocumentType =
            entries.firstOrNull { it.label == raw } ?: BILL_OF_SUPPLY
    }
}

@Keep
@IgnoreExtraProperties
data class BusinessProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val companyName: String = "",
    val companyAddress: String = "",
    val companyState: String = "",
    val companyPinCode: String = "",
    val companyGstin: String = "",
    val cinNo: String = "",
    val bankName: String = "",
    val bankAccountNumber: String = "",
    val bankIfsc: String = "",
    val authorisedSignatory: String = "",
    val termsAndConditions: String = "",
    val defaultQtyUnit: String = "MT",
    val nextInvoiceNumber: Long = 1L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "companyName" to companyName.trim().take(120),
        "companyAddress" to companyAddress.trim().take(250),
        "companyState" to companyState.trim().take(60),
        "companyPinCode" to companyPinCode.trim().take(12),
        "companyGstin" to companyGstin.trim().uppercase().take(20),
        "cinNo" to cinNo.trim().uppercase().take(30),
        "bankName" to bankName.trim().take(100),
        "bankAccountNumber" to bankAccountNumber.trim().take(40),
        "bankIfsc" to bankIfsc.trim().uppercase().take(20),
        "authorisedSignatory" to authorisedSignatory.trim().take(80),
        "termsAndConditions" to termsAndConditions.trim().take(400),
        "defaultQtyUnit" to defaultQtyUnit.trim().ifBlank { "MT" }.take(20),
        "nextInvoiceNumber" to nextInvoiceNumber.coerceIn(1L, 9_999_999L),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "companyName" to companyName.trim().take(120),
        "companyAddress" to companyAddress.trim().take(250),
        "companyState" to companyState.trim().take(60),
        "companyPinCode" to companyPinCode.trim().take(12),
        "companyGstin" to companyGstin.trim().uppercase().take(20),
        "cinNo" to cinNo.trim().uppercase().take(30),
        "bankName" to bankName.trim().take(100),
        "bankAccountNumber" to bankAccountNumber.trim().take(40),
        "bankIfsc" to bankIfsc.trim().uppercase().take(20),
        "authorisedSignatory" to authorisedSignatory.trim().take(80),
        "termsAndConditions" to termsAndConditions.trim().take(400),
        "defaultQtyUnit" to defaultQtyUnit.trim().ifBlank { "MT" }.take(20),
        "nextInvoiceNumber" to nextInvoiceNumber.coerceIn(1L, 9_999_999L),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class Party(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val address: String = "",
    val gstin: String = "",
    val state: String = "",
    val pinCode: String = "",
    val phone: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "name" to name.trim().take(120),
        "address" to address.trim().take(250),
        "gstin" to gstin.trim().uppercase().take(20),
        "state" to state.trim().take(60),
        "pinCode" to pinCode.trim().take(12),
        "phone" to phone.trim().take(20),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.trim().take(120),
        "address" to address.trim().take(250),
        "gstin" to gstin.trim().uppercase().take(20),
        "state" to state.trim().take(60),
        "pinCode" to pinCode.trim().take(12),
        "phone" to phone.trim().take(20),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class BillLineItem(
    val serialNo: Int = 1,
    val materialDescription: String = "",
    val hsnCode: String = "",
    val numberOfBags: String = "",
    val qty: Double = 0.0,
    val rate: Double = 0.0,
    val amount: Double = 0.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "serialNo" to serialNo,
        "materialDescription" to materialDescription.trim().take(120),
        "hsnCode" to hsnCode.trim().take(20),
        "numberOfBags" to numberOfBags.trim().take(20),
        "qty" to qty.coerceAtLeast(0.0),
        "rate" to rate.coerceAtLeast(0.0),
        "amount" to amount.coerceAtLeast(0.0)
    )
}

@Keep
@IgnoreExtraProperties
data class SupplyBill(
    @DocumentId val id: String = "",
    val userId: String = "",
    val documentType: String = DocumentType.BILL_OF_SUPPLY.label,
    val copyType: String = CopyType.ORIGINAL.name,
    val invoiceNumber: String = "",
    val invoiceDate: String = "",
    val reverseCharge: String = "N",
    val placeState: String = "",
    val placePinCode: String = "",
    val transportMode: String = "By Road",
    val vehicleNumber: String = "",
    val transporterName: String = "",
    val driverPhone: String = "",
    val bhadaAmount: Double = 0.0,
    val truckAdvance: Double = 0.0,
    val sellerName: String = "",
    val sellerAddress: String = "",
    val sellerState: String = "",
    val sellerPinCode: String = "",
    val sellerGstin: String = "",
    val sellerCinNo: String = "",
    val bankName: String = "",
    val bankAccountNumber: String = "",
    val bankIfsc: String = "",
    val authorisedSignatory: String = "",
    val termsAndConditions: String = "",
    val receiverName: String = "",
    val receiverAddress: String = "",
    val receiverPoNo: String = "",
    val receiverGstin: String = "",
    val receiverState: String = "",
    val receiverPinCode: String = "",
    val consigneeName: String = "",
    val consigneeAddress: String = "",
    val consigneeGstin: String = "",
    val consigneeState: String = "",
    val consigneePinCode: String = "",
    val qtyUnitLabel: String = "in MT",
    val items: List<BillLineItem> = emptyList(),
    val totalBeforeTax: Double = 0.0,
    val cgstRate: Double = 0.0,
    val sgstRate: Double = 0.0,
    val igstRate: Double = 0.0,
    val totalTaxAmount: Double = 0.0,
    val totalAfterTax: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun resolvedCopyType(): CopyType = CopyType.fromRaw(copyType)
    fun resolvedDocumentType(): DocumentType = DocumentType.fromLabel(documentType)

    fun toCreateMap(ownerUid: String): Map<String, Any> =
        toMutableFieldsMap() + mapOf(
            "userId" to ownerUid,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

    fun toUpdateMap(): Map<String, Any> =
        toMutableFieldsMap() + mapOf(
            "updatedAt" to FieldValue.serverTimestamp()
        )

    private fun toMutableFieldsMap(): Map<String, Any> {
        val safeDocType = if (documentType == DocumentType.TAX_INVOICE.label) {
            DocumentType.TAX_INVOICE.label
        } else {
            DocumentType.BILL_OF_SUPPLY.label
        }
        val safeCopyType = CopyType.fromRaw(copyType).name
        val safeReverseCharge = if (reverseCharge.uppercase() == "Y") "Y" else "N"
        val boundedItems = items.take(25).mapIndexed { index, item ->
            val computedAmount = if (item.amount > 0.0) item.amount else item.qty * item.rate
            item.copy(serialNo = index + 1, amount = computedAmount).toMap()
        }
        val beforeTax = totalBeforeTax.coerceIn(0.0, 10_000_000_000.0)
        val cgst = cgstRate.coerceIn(0.0, 100.0)
        val sgst = sgstRate.coerceIn(0.0, 100.0)
        val igst = igstRate.coerceIn(0.0, 100.0)
        val taxAmt = totalTaxAmount.coerceIn(0.0, 10_000_000_000.0)
        val afterTax = totalAfterTax.coerceIn(0.0, 10_000_000_000.0)

        return mapOf(
            "documentType" to safeDocType,
            "copyType" to safeCopyType,
            "invoiceNumber" to invoiceNumber.trim().take(40),
            "invoiceDate" to invoiceDate.trim().take(30),
            "reverseCharge" to safeReverseCharge,
            "placeState" to placeState.trim().take(60),
            "placePinCode" to placePinCode.trim().take(12),
            "transportMode" to transportMode.trim().take(50),
            "vehicleNumber" to vehicleNumber.trim().uppercase().take(30),
            "transporterName" to transporterName.trim().take(100),
            "driverPhone" to driverPhone.trim().take(25),
            "bhadaAmount" to bhadaAmount.coerceIn(0.0, 100_000_000.0),
            "truckAdvance" to truckAdvance.coerceIn(0.0, 100_000_000.0),
            "sellerName" to sellerName.trim().take(120),
            "sellerAddress" to sellerAddress.trim().take(250),
            "sellerState" to sellerState.trim().take(60),
            "sellerPinCode" to sellerPinCode.trim().take(12),
            "sellerGstin" to sellerGstin.trim().uppercase().take(20),
            "sellerCinNo" to sellerCinNo.trim().uppercase().take(30),
            "bankName" to bankName.trim().take(100),
            "bankAccountNumber" to bankAccountNumber.trim().take(40),
            "bankIfsc" to bankIfsc.trim().uppercase().take(20),
            "authorisedSignatory" to authorisedSignatory.trim().take(80),
            "termsAndConditions" to termsAndConditions.trim().take(400),
            "receiverName" to receiverName.trim().take(120),
            "receiverAddress" to receiverAddress.trim().take(250),
            "receiverPoNo" to receiverPoNo.trim().take(50),
            "receiverGstin" to receiverGstin.trim().uppercase().take(20),
            "receiverState" to receiverState.trim().take(60),
            "receiverPinCode" to receiverPinCode.trim().take(12),
            "consigneeName" to consigneeName.trim().take(120),
            "consigneeAddress" to consigneeAddress.trim().take(250),
            "consigneeGstin" to consigneeGstin.trim().uppercase().take(20),
            "consigneeState" to consigneeState.trim().take(60),
            "consigneePinCode" to consigneePinCode.trim().take(12),
            "qtyUnitLabel" to qtyUnitLabel.trim().take(20),
            "items" to boundedItems,
            "totalBeforeTax" to beforeTax,
            "cgstRate" to cgst,
            "sgstRate" to sgst,
            "igstRate" to igst,
            "totalTaxAmount" to taxAmt,
            "totalAfterTax" to afterTax
        )
    }
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
