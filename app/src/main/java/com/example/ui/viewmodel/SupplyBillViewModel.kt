package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.BusinessProfile
import com.example.data.model.Party
import com.example.data.model.SupplyBill
import com.example.data.model.UiState
import com.example.data.repository.SupplyBillRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SupplyBillViewModel(
    private val repository: SupplyBillRepository,
    val currentUserId: String
) : ViewModel() {

    val businessProfile: StateFlow<UiState<BusinessProfile?>> =
        repository.observeBusinessProfile(currentUserId)
            .map<BusinessProfile?, UiState<BusinessProfile?>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing business profile", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load business profile"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val parties: StateFlow<UiState<List<Party>>> =
        repository.observeParties(currentUserId)
            .map<List<Party>, UiState<List<Party>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing parties", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load parties"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val bills: StateFlow<UiState<List<SupplyBill>>> =
        repository.observeBills(currentUserId)
            .map<List<SupplyBill>, UiState<List<SupplyBill>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing bills", rootError)
                emit(UiState.Error(rootError.message ?: "Failed to load bills"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun saveBusinessProfile(
        profile: BusinessProfile,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveBusinessProfile(currentUserId, profile)
            _isSaving.value = false
            onResult(result)
        }
    }

    fun saveParty(
        party: Party,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveParty(currentUserId, party)
            _isSaving.value = false
            onResult(result)
        }
    }

    fun deleteParty(
        partyId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.deleteParty(currentUserId, partyId)
            onResult(result)
        }
    }

    fun saveBill(
        bill: SupplyBill,
        incrementNextInvoiceNumber: Long?,
        alsoSaveReceiverAsParty: Boolean,
        onResult: (Result<String>) -> Unit
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            val result = repository.saveBill(
                userId = currentUserId,
                bill = bill,
                incrementNextInvoiceNumber = incrementNextInvoiceNumber
            )
            if (result.isSuccess && alsoSaveReceiverAsParty && bill.receiverName.isNotBlank()) {
                val existingParties = (parties.value as? UiState.Success)?.data.orEmpty()
                val alreadyExists = existingParties.any {
                    it.name.equals(bill.receiverName.trim(), ignoreCase = true)
                }
                if (!alreadyExists) {
                    repository.saveParty(
                        currentUserId,
                        Party(
                            name = bill.receiverName.trim(),
                            address = bill.receiverAddress.trim(),
                            gstin = bill.receiverGstin.trim().uppercase(),
                            state = bill.receiverState.trim(),
                            pinCode = bill.receiverPinCode.trim()
                        )
                    )
                }
            }
            _isSaving.value = false
            onResult(result)
        }
    }

    fun deleteBill(
        billId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.deleteBill(currentUserId, billId)
            onResult(result)
        }
    }

    private companion object {
        const val TAG = "SupplyBillVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
