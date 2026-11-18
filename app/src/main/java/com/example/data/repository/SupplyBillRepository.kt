package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.firebase.OperationType
import com.example.data.firebase.handleFirestoreError
import com.example.data.model.BillLineItem
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.SupplyBill
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SupplyBillRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireAuthorizedUser(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        return uid
    }

    fun observeBusinessProfile(userId: String): Flow<BusinessProfile?> = flow {
        requireAuthorizedUser(userId)
        val docRef = db.collection("users").document(userId)
            .collection("profile").document(BUSINESS_DOC_ID)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(
                            BusinessProfile::class.java,
                            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                        )
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveBusinessProfile(userId: String, profile: BusinessProfile): Result<Unit> {
        val docPath = "users/$userId/profile/$BUSINESS_DOC_ID"
        return try {
            val uid = requireAuthorizedUser(userId)
            require(profile.companyName.isNotBlank()) { "Company name is required." }
            val docRef = db.collection("users").document(uid)
                .collection("profile").document(BUSINESS_DOC_ID)
            val existingSnapshot = docRef.get().await()
            if (existingSnapshot.exists()) {
                docRef.update(profile.toUpdateMap()).await()
            } else {
                docRef.set(profile.toCreateMap(uid)).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeParties(userId: String): Flow<List<Party>> = flow {
        requireAuthorizedUser(userId)
        val colRef = db.collection("users").document(userId).collection("parties")
        emitAll(
            colRef.orderBy("name", Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Party::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveParty(userId: String, party: Party): Result<String> {
        val basePath = "users/$userId/parties"
        var docPath = basePath
        return try {
            val uid = requireAuthorizedUser(userId)
            require(party.name.isNotBlank()) { "Party name is required." }
            val colRef = db.collection("users").document(uid).collection("parties")
            if (party.id.isBlank()) {
                val docRef = colRef.document()
                docPath = docRef.path
                docRef.set(party.toCreateMap(uid)).await()
                Result.success(docRef.id)
            } else {
                val docRef = colRef.document(party.id)
                docPath = docRef.path
                docRef.update(party.toUpdateMap()).await()
                Result.success(party.id)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteParty(userId: String, partyId: String): Result<Unit> {
        val docPath = "users/$userId/parties/$partyId"
        return try {
            val uid = requireAuthorizedUser(userId)
            require(partyId.isNotBlank()) { "Party ID cannot be blank." }
            db.collection("users").document(uid)
                .collection("parties").document(partyId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeBills(userId: String): Flow<List<SupplyBill>> = flow {
        requireAuthorizedUser(userId)
        val colRef = db.collection("users").document(userId).collection("bills")
        emitAll(
            colRef.orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.map { doc -> doc.toSupplyBillSafely() }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path)
                    }
                    throw error
                }
        )
    }

    fun observeBill(userId: String, billId: String): Flow<SupplyBill?> = flow {
        requireAuthorizedUser(userId)
        require(billId.isNotBlank()) { "Bill ID cannot be blank." }
        val docRef = db.collection("users").document(userId)
            .collection("bills").document(billId)
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) snapshot.toSupplyBillSafely() else null
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, docRef.path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveBill(
        userId: String,
        bill: SupplyBill,
        incrementNextInvoiceNumber: Long? = null
    ): Result<String> {
        var docPath = "users/$userId/bills"
        return try {
            val uid = requireAuthorizedUser(userId)
            require(bill.sellerName.isNotBlank()) { "Seller company name is required." }
            require(bill.receiverName.isNotBlank()) { "Receiver name is required." }
            require(bill.invoiceNumber.isNotBlank()) { "Invoice number is required." }
            require(bill.items.isNotEmpty()) { "At least one material line item is required." }
            require(bill.items.size <= 25) { "Maximum 25 items per bill supported." }

            val colRef = db.collection("users").document(uid).collection("bills")
            val savedId = if (bill.id.isBlank()) {
                val docRef = colRef.document()
                docPath = docRef.path
                docRef.set(bill.toCreateMap(uid)).await()

                if (incrementNextInvoiceNumber != null) {
                    val profileRef = db.collection("users").document(uid)
                        .collection("profile").document(BUSINESS_DOC_ID)
                    val profileSnap = profileRef.get().await()
                    if (profileSnap.exists()) {
                        profileRef.update(
                            mapOf(
                                "nextInvoiceNumber" to incrementNextInvoiceNumber.coerceIn(1L, 9_999_999L),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).await()
                    }
                }
                docRef.id
            } else {
                val docRef = colRef.document(bill.id)
                docPath = docRef.path
                docRef.update(bill.toUpdateMap()).await()
                bill.id
            }
            Result.success(savedId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteBill(userId: String, billId: String): Result<Unit> {
        val docPath = "users/$userId/bills/$billId"
        return try {
            val uid = requireAuthorizedUser(userId)
            require(billId.isNotBlank()) { "Bill ID cannot be blank." }
            db.collection("users").document(uid)
                .collection("bills").document(billId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    private fun DocumentSnapshot.toSupplyBillSafely(): SupplyBill {
        val rawItems = (get("items") as? List<*>)?.mapIndexedNotNull { idx, entry ->
            val map = entry as? Map<*, *> ?: return@mapIndexedNotNull null
            BillLineItem(
                serialNo = (map["serialNo"] as? Number)?.toInt() ?: (idx + 1),
                materialDescription = map["materialDescription"] as? String ?: "",
                hsnCode = map["hsnCode"] as? String ?: "",
                numberOfBags = map["numberOfBags"] as? String ?: "",
                qty = (map["qty"] as? Number)?.toDouble() ?: 0.0,
                rate = (map["rate"] as? Number)?.toDouble() ?: 0.0,
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0
            )
        } ?: emptyList()

        return SupplyBill(
            id = id,
            userId = getString("userId") ?: "",
            documentType = getString("documentType") ?: "Bill of Supply",
            copyType = getString("copyType") ?: "ORIGINAL",
            invoiceNumber = getString("invoiceNumber") ?: "",
            invoiceDate = getString("invoiceDate") ?: "",
            reverseCharge = getString("reverseCharge") ?: "N",
            placeState = getString("placeState") ?: "",
            placePinCode = getString("placePinCode") ?: "",
            transportMode = getString("transportMode") ?: "By Road",
            vehicleNumber = getString("vehicleNumber") ?: "",
            transporterName = getString("transporterName") ?: "",
            driverPhone = getString("driverPhone") ?: "",
            bhadaAmount = (get("bhadaAmount") as? Number)?.toDouble() ?: 0.0,
            truckAdvance = (get("truckAdvance") as? Number)?.toDouble() ?: 0.0,
            sellerName = getString("sellerName") ?: "",
            sellerAddress = getString("sellerAddress") ?: "",
            sellerState = getString("sellerState") ?: "",
            sellerPinCode = getString("sellerPinCode") ?: "",
            sellerGstin = getString("sellerGstin") ?: "",
            sellerCinNo = getString("sellerCinNo") ?: "",
            bankName = getString("bankName") ?: "",
            bankAccountNumber = getString("bankAccountNumber") ?: "",
            bankIfsc = getString("bankIfsc") ?: "",
            authorisedSignatory = getString("authorisedSignatory") ?: "",
            termsAndConditions = getString("termsAndConditions") ?: "",
            receiverName = getString("receiverName") ?: "",
            receiverAddress = getString("receiverAddress") ?: "",
            receiverPoNo = getString("receiverPoNo") ?: "",
            receiverGstin = getString("receiverGstin") ?: "",
            receiverState = getString("receiverState") ?: "",
            receiverPinCode = getString("receiverPinCode") ?: "",
            consigneeName = getString("consigneeName") ?: "",
            consigneeAddress = getString("consigneeAddress") ?: "",
            consigneeGstin = getString("consigneeGstin") ?: "",
            consigneeState = getString("consigneeState") ?: "",
            consigneePinCode = getString("consigneePinCode") ?: "",
            qtyUnitLabel = getString("qtyUnitLabel") ?: "in MT",
            items = rawItems,
            totalBeforeTax = (get("totalBeforeTax") as? Number)?.toDouble() ?: 0.0,
            cgstRate = (get("cgstRate") as? Number)?.toDouble() ?: 0.0,
            sgstRate = (get("sgstRate") as? Number)?.toDouble() ?: 0.0,
            igstRate = (get("igstRate") as? Number)?.toDouble() ?: 0.0,
            totalTaxAmount = (get("totalTaxAmount") as? Number)?.toDouble() ?: 0.0,
            totalAfterTax = (get("totalAfterTax") as? Number)?.toDouble() ?: 0.0,
            createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    companion object {
        const val BUSINESS_DOC_ID = "business"
    }
}
