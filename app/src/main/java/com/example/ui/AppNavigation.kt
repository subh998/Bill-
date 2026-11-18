package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.model.SupplyBill
import com.example.data.model.UiState
import com.example.data.repository.SupplyBillRepository
import com.example.export.BillExportManager
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BillEditorScreen
import com.example.ui.screens.BillPreviewScreen
import com.example.ui.screens.BillsListScreen
import com.example.ui.screens.BusinessProfileScreen
import com.example.ui.screens.PartiesScreen
import com.example.ui.viewmodel.SupplyBillViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

private const val ROUTE_BILLS = "bills"
private const val ROUTE_PARTIES = "parties"
private const val ROUTE_BUSINESS = "business"
private const val ROUTE_EDITOR = "bill_editor"
private const val ROUTE_PREVIEW = "bill_preview/{billId}"

private data class TopTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val topTabs = listOf(
    TopTab(ROUTE_BILLS, "Bills", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong),
    TopTab(ROUTE_PARTIES, "Parties", Icons.Filled.People, Icons.Outlined.People),
    TopTab(ROUTE_BUSINESS, "Business", Icons.Filled.Business, Icons.Outlined.Business)
)

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            AuthenticatedSupplyBillShell(currentUserId = user.uid)
        }
    }
}

@Composable
private fun AuthenticatedSupplyBillShell(currentUserId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: SupplyBillViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SupplyBillViewModel(
                    repository = SupplyBillRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val billsState by viewModel.bills.collectAsStateWithLifecycle()
    val partiesState by viewModel.parties.collectAsStateWithLifecycle()
    val profileState by viewModel.businessProfile.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showTopLevelNav = currentDestination?.route in topTabs.map { it.route }

    var draftBillForEditor by remember { mutableStateOf<SupplyBill?>(null) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showTopLevelNav && !useNavigationRail) {
                    NavigationBar {
                        topTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (showTopLevelNav && useNavigationRail) {
                    NavigationRail {
                        topTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = ROUTE_BILLS,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(ROUTE_BILLS) {
                        BillsListScreen(
                            billsState = billsState,
                            profileState = profileState,
                            onCreateNewBill = {
                                draftBillForEditor = null
                                navController.navigate(ROUTE_EDITOR)
                            },
                            onOpenBusinessProfile = {
                                navController.navigate(ROUTE_BUSINESS) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onOpenBillPreview = { bill ->
                                navController.navigate("bill_preview/${bill.id}")
                            },
                            onDuplicateBill = { bill ->
                                val nextNo = ((profileState as? UiState.Success)?.data?.nextInvoiceNumber ?: 1L).toString()
                                draftBillForEditor = bill.copy(
                                    id = "",
                                    invoiceNumber = nextNo,
                                    createdAt = null,
                                    updatedAt = null
                                )
                                navController.navigate(ROUTE_EDITOR)
                            },
                            onSharePdf = { bill ->
                                scope.launch {
                                    BillExportManager.createSharePdfFile(context, bill)
                                        .onSuccess { file ->
                                            BillExportManager.launchShareIntent(
                                                context, file, "application/pdf", "Share Bill PDF"
                                            )
                                        }
                                        .onFailure { err ->
                                            snackbarHostState.showSnackbar("PDF share failed: ${err.localizedMessage}")
                                        }
                                }
                            },
                            onSharePng = { bill ->
                                scope.launch {
                                    BillExportManager.createSharePngFile(context, bill)
                                        .onSuccess { file ->
                                            BillExportManager.launchShareIntent(
                                                context, file, "image/png", "Share Bill Image"
                                            )
                                        }
                                        .onFailure { err ->
                                            snackbarHostState.showSnackbar("Image share failed: ${err.localizedMessage}")
                                        }
                                }
                            },
                            onDeleteBill = { billId ->
                                viewModel.deleteBill(billId) { res ->
                                    res.onFailure { err ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Delete failed: ${err.localizedMessage}")
                                        }
                                    }
                                }
                            }
                        )
                    }

                    composable(ROUTE_PARTIES) {
                        PartiesScreen(
                            partiesState = partiesState,
                            isSaving = isSaving,
                            onSaveParty = { party, onSuccess, onError ->
                                viewModel.saveParty(party) { res ->
                                    res.onSuccess {
                                        onSuccess()
                                        scope.launch { snackbarHostState.showSnackbar("Party saved") }
                                    }.onFailure { err ->
                                        onError(err.localizedMessage ?: "Failed to save party")
                                    }
                                }
                            },
                            onDeleteParty = { partyId ->
                                viewModel.deleteParty(partyId) { res ->
                                    res.onFailure { err ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Delete failed: ${err.localizedMessage}")
                                        }
                                    }
                                }
                            }
                        )
                    }

                    composable(ROUTE_BUSINESS) {
                        BusinessProfileScreen(
                            profileState = profileState,
                            isSaving = isSaving,
                            onSaveProfile = { updated ->
                                viewModel.saveBusinessProfile(updated) { res ->
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            res.fold(
                                                onSuccess = { "Business profile saved" },
                                                onFailure = { "Save failed: ${it.localizedMessage}" }
                                            )
                                        )
                                    }
                                }
                            },
                            onSignOut = {
                                signOutUser(credentialManager, scope)
                            }
                        )
                    }

                    composable(ROUTE_EDITOR) {
                        val profile = (profileState as? UiState.Success)?.data
                        val savedParties = (partiesState as? UiState.Success)?.data.orEmpty()
                        BillEditorScreen(
                            initialBill = draftBillForEditor,
                            businessProfile = profile,
                            savedParties = savedParties,
                            isSaving = isSaving,
                            onBack = { navController.popBackStack() },
                            onSaveAndPreview = { bill, nextCounter, saveParty ->
                                viewModel.saveBill(
                                    bill = bill,
                                    incrementNextInvoiceNumber = nextCounter,
                                    alsoSaveReceiverAsParty = saveParty
                                ) { res ->
                                    res.onSuccess { savedId ->
                                        draftBillForEditor = null
                                        navController.navigate("bill_preview/$savedId") {
                                            popUpTo(ROUTE_EDITOR) { inclusive = true }
                                        }
                                    }.onFailure { err ->
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Failed to save bill: ${err.localizedMessage}"
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }

                    composable(ROUTE_PREVIEW) { backStackEntry ->
                        val billId = backStackEntry.arguments?.getString("billId").orEmpty()
                        val billsList = (billsState as? UiState.Success)?.data.orEmpty()
                        val bill = billsList.firstOrNull { it.id == billId }
                        if (bill != null) {
                            BillPreviewScreen(
                                bill = bill,
                                onBack = { navController.popBackStack() },
                                onEdit = {
                                    draftBillForEditor = bill
                                    navController.navigate(ROUTE_EDITOR)
                                },
                                onChangeCopyType = { newCopyType ->
                                    viewModel.saveBill(
                                        bill = bill.copy(copyType = newCopyType.name),
                                        incrementNextInvoiceNumber = null,
                                        alsoSaveReceiverAsParty = false
                                    ) { res ->
                                        res.onFailure { err ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Update failed: ${err.localizedMessage}")
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
