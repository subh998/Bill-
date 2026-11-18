package com.example.ui.screens

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CopyType
import com.example.data.model.SupplyBill
import com.example.export.BillDocumentRenderer
import com.example.export.BillExportManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BillPreviewScreen(
    bill: SupplyBill,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onChangeCopyType: (CopyType) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isExporting by remember { mutableStateOf(false) }

    // Pinch-to-zoom state on the preview sheet
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val nextScale = (scale * zoomChange).coerceIn(1f, 3.2f)
        scale = nextScale
        offset = if (nextScale <= 1.01f) Offset.Zero else offset + offsetChange
    }

    LaunchedEffect(bill) {
        val rendered = withContext(Dispatchers.Default) {
            BillDocumentRenderer.renderBitmap(bill)
        }
        val old = previewBitmap
        previewBitmap = rendered
        if (old != null && old !== rendered) {
            old.recycle()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            previewBitmap?.recycle()
            previewBitmap = null
        }
    }

    // SAF launchers for Android 9 and below (API < 29)
    val savePdfSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isExporting = true
                val res = BillExportManager.writePdfToUri(context, uri, bill)
                isExporting = false
                snackbarHostState.showSnackbar(
                    res.fold(
                        onSuccess = { "PDF saved to phone successfully." },
                        onFailure = { "Failed to save PDF: ${it.localizedMessage}" }
                    )
                )
            }
        }
    }

    val savePngSafLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isExporting = true
                val res = BillExportManager.writePngToUri(context, uri, bill)
                isExporting = false
                snackbarHostState.showSnackbar(
                    res.fold(
                        onSuccess = { "PNG image saved to phone successfully." },
                        onFailure = { "Failed to save PNG: ${it.localizedMessage}" }
                    )
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Bill #${bill.invoiceNumber}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_bill_button")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit Bill")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 10.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isExporting = true
                                    val res = BillExportManager.createSharePdfFile(context, bill)
                                    isExporting = false
                                    res.onSuccess { file ->
                                        BillExportManager.launchShareIntent(
                                            context = context,
                                            file = file,
                                            mimeType = "application/pdf",
                                            chooserTitle = "Share Bill PDF"
                                        )
                                    }.onFailure { err ->
                                        snackbarHostState.showSnackbar("PDF share failed: ${err.localizedMessage}")
                                    }
                                }
                            },
                            enabled = !isExporting,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_pdf_button")
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Share PDF")
                        }

                        Button(
                            onClick = {
                                scope.launch {
                                    isExporting = true
                                    val res = BillExportManager.createSharePngFile(context, bill)
                                    isExporting = false
                                    res.onSuccess { file ->
                                        BillExportManager.launchShareIntent(
                                            context = context,
                                            file = file,
                                            mimeType = "image/png",
                                            chooserTitle = "Share Bill Image"
                                        )
                                    }.onFailure { err ->
                                        snackbarHostState.showSnackbar("Image share failed: ${err.localizedMessage}")
                                    }
                                }
                            },
                            enabled = !isExporting,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("share_png_button")
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Share Image")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    scope.launch {
                                        isExporting = true
                                        val res = BillExportManager.savePdfToMediaStore(context, bill)
                                        isExporting = false
                                        snackbarHostState.showSnackbar(
                                            res.fold(
                                                onSuccess = { path -> "Saved PDF to $path" },
                                                onFailure = { "Save PDF failed: ${it.localizedMessage}" }
                                            )
                                        )
                                    }
                                } else {
                                    savePdfSafLauncher.launch(BillExportManager.suggestedPdfFileName(bill))
                                }
                            },
                            enabled = !isExporting,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("save_pdf_button")
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Save PDF")
                        }

                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    scope.launch {
                                        isExporting = true
                                        val res = BillExportManager.savePngToMediaStore(context, bill)
                                        isExporting = false
                                        snackbarHostState.showSnackbar(
                                            res.fold(
                                                onSuccess = { path -> "Saved PNG to $path" },
                                                onFailure = { "Save PNG failed: ${it.localizedMessage}" }
                                            )
                                        )
                                    }
                                } else {
                                    savePngSafLauncher.launch(BillExportManager.suggestedPngFileName(bill))
                                }
                            },
                            enabled = !isExporting,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("save_png_button")
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Save PNG")
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
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Quick Copy Type Switcher (Original / Duplicate / Triplicate / Extra OC)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                val activeCopy = bill.resolvedCopyType()
                CopyType.entries.forEach { type ->
                    FilterChip(
                        selected = activeCopy == type,
                        onClick = {
                            if (activeCopy != type) onChangeCopyType(type)
                        },
                        label = { Text(type.label) }
                    )
                }
            }

            // Zoomable A4 Paper Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 700.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                val bmp = previewBitmap
                if (bmp == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(595f / 842f)
                            .background(Color.White, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Surface(
                        shadowElevation = 6.dp,
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(595f / 842f)
                            .transformable(state = transformState)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Bill of Supply Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                        )
                    }
                }
            }

            Text(
                text = "Pinch on bill preview to zoom • Exact A4 PDF & PNG output",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}
