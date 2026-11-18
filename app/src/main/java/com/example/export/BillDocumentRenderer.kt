package com.example.export

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.model.CopyType
import com.example.data.model.SupplyBill
import java.io.OutputStream
import kotlin.math.max

/**
 * Renders a Tally-faithful A4 "Bill of Supply" / "Tax Invoice" onto any Android Canvas,
 * backing both vector-crisp PdfDocument output and high-DPI PNG Bitmap output.
 */
object BillDocumentRenderer {

    const val A4_WIDTH_PT = 595
    const val A4_HEIGHT_PT = 842

    // 200 DPI A4 raster size for crisp PNG export & WhatsApp sharing
    const val PNG_WIDTH_PX = 1654
    const val PNG_HEIGHT_PX = 2339

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 1.0f
    }

    private val thinBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(60, 60, 60)
        style = Paint.Style.STROKE
        strokeWidth = 0.7f
    }

    private val shadePaint = Paint().apply {
        color = Color.rgb(218, 222, 228)
        style = Paint.Style.FILL
    }

    private val lightShadePaint = Paint().apply {
        color = Color.rgb(238, 240, 244)
        style = Paint.Style.FILL
    }

    private val darkBadgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(22, 26, 32)
        style = Paint.Style.FILL
    }

    private fun textPaint(
        size: Float,
        bold: Boolean = false,
        color: Int = Color.BLACK,
        align: Paint.Align = Paint.Align.LEFT
    ) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        textAlign = align
        typeface = Typeface.create(
            Typeface.SANS_SERIF,
            if (bold) Typeface.BOLD else Typeface.NORMAL
        )
    }

    fun writePdf(bill: SupplyBill, outputStream: OutputStream) {
        val document = PdfDocument()
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH_PT, A4_HEIGHT_PT, 1).create()
            val page = document.startPage(pageInfo)
            drawBillPage(page.canvas, bill)
            document.finishPage(page)
            document.writeTo(outputStream)
        } finally {
            document.close()
        }
    }

    fun renderBitmap(bill: SupplyBill): Bitmap {
        val bitmap = Bitmap.createBitmap(PNG_WIDTH_PX, PNG_HEIGHT_PX, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val scaleX = PNG_WIDTH_PX.toFloat() / A4_WIDTH_PT.toFloat()
        val scaleY = PNG_HEIGHT_PX.toFloat() / A4_HEIGHT_PT.toFloat()
        canvas.save()
        canvas.scale(scaleX, scaleY)
        drawBillPage(canvas, bill)
        canvas.restore()
        return bitmap
    }

    fun drawBillPage(canvas: Canvas, bill: SupplyBill) {
        canvas.drawColor(Color.WHITE)

        val left = 24f
        val top = 24f
        val right = A4_WIDTH_PT - 24f // 571f
        val bottom = A4_HEIGHT_PT - 24f // 818f
        val contentWidth = right - left // 547f

        // 1. HEADER SECTION (Seller Company Info + Copy Checkboxes)
        val headerHeight = 86f
        val copyBoxWidth = 118f
        val companyRight = right - copyBoxWidth

        canvas.drawRect(left, top, companyRight, top + headerHeight, shadePaint)
        canvas.drawRect(left, top, right, top + headerHeight, borderPaint)
        canvas.drawLine(companyRight, top, companyRight, top + headerHeight, borderPaint)

        // Company info centered in left block
        val companyCenterX = left + (companyRight - left) / 2f
        val companyTitlePaint = textPaint(13f, bold = true, align = Paint.Align.CENTER)
        val companySubPaint = textPaint(8.5f, bold = true, align = Paint.Align.CENTER)
        val companyLinePaint = textPaint(8.5f, bold = false, align = Paint.Align.CENTER)

        var cy = top + 20f
        canvas.drawText(
            "COMPANY NAME:- ${bill.sellerName.uppercase()}",
            companyCenterX,
            cy,
            companyTitlePaint
        )
        cy += 15f
        if (bill.sellerAddress.isNotBlank()) {
            val addrText = "ADDRESS:- ${bill.sellerAddress.uppercase()}"
            drawCenteredWrappedText(canvas, addrText, companyCenterX, cy, companyRight - left - 16f, companySubPaint)
            cy += 22f
        } else {
            cy += 12f
        }
        val stateLine = buildString {
            append("STATE:- ${bill.sellerState.uppercase()}")
            if (bill.sellerPinCode.isNotBlank()) {
                append("   PIN:- ${bill.sellerPinCode}")
            }
        }
        canvas.drawText(stateLine, companyCenterX, cy, companyLinePaint)
        cy += 14f
        if (bill.sellerGstin.isNotBlank()) {
            canvas.drawText("GSTIN :- ${bill.sellerGstin.uppercase()}", companyCenterX, cy, companySubPaint)
        }

        // Copy Checkboxes in top-right box
        val selectedCopy = bill.resolvedCopyType()
        val copyRows = listOf(
            CopyType.ORIGINAL to "Original",
            CopyType.DUPLICATE to "Duplicate",
            CopyType.TRIPLICATE to "Triplicate",
            CopyType.EXTRA_OC to "Extra OC"
        )
        val copyRowH = headerHeight / 4f
        val copyLabelPaint = textPaint(8.5f, bold = false)
        val copyBoldPaint = textPaint(8.5f, bold = true)
        copyRows.forEachIndexed { idx, (type, label) ->
            val rowTop = top + idx * copyRowH
            if (idx > 0) {
                canvas.drawLine(companyRight, rowTop, right, rowTop, thinBorderPaint)
            }
            val cbLeft = companyRight + 8f
            val cbTop = rowTop + (copyRowH - 9f) / 2f
            canvas.drawRect(cbLeft, cbTop, cbLeft + 9f, cbTop + 9f, borderPaint)
            if (type == selectedCopy) {
                canvas.drawLine(cbLeft + 2f, cbTop + 4.5f, cbLeft + 4f, cbTop + 7f, borderPaint)
                canvas.drawLine(cbLeft + 4f, cbTop + 7f, cbLeft + 7.5f, cbTop + 2f, borderPaint)
                canvas.drawText(label, cbLeft + 15f, rowTop + copyRowH - 6.5f, copyBoldPaint)
            } else {
                canvas.drawText(label, cbLeft + 15f, rowTop + copyRowH - 6.5f, copyLabelPaint)
            }
        }

        // 2. DOCUMENT TITLE BAR ("Bill of Supply" / "Tax Invoice")
        var y = top + headerHeight
        val titleBarH = 22f
        canvas.drawRect(left, y, right, y + titleBarH, lightShadePaint)
        canvas.drawRect(left, y, right, y + titleBarH, borderPaint)
        canvas.drawText(
            bill.documentType.ifBlank { "Bill of Supply" },
            left + contentWidth / 2f,
            y + 15f,
            textPaint(11f, bold = true, align = Paint.Align.CENTER)
        )
        y += titleBarH

        // 3. INVOICE & TRANSPORT DETAILS ROW
        val metaH = 66f
        val midX = left + contentWidth * 0.5f
        canvas.drawRect(left, y, right, y + metaH, borderPaint)
        canvas.drawLine(midX, y, midX, y + metaH, borderPaint)

        val rowH = metaH / 4f
        for (i in 1..3) {
            canvas.drawLine(left, y + i * rowH, right, y + i * rowH, thinBorderPaint)
        }
        val leftLabelX = left + 5f
        val leftValX = left + 104f
        val labelPaint = textPaint(8.5f, bold = false)
        val boldValPaint = textPaint(8.5f, bold = true)
        val valPaint = textPaint(8.5f, bold = false)

        // Left metadata rows
        canvas.drawText("Invoice No :-", leftLabelX, y + 12f, labelPaint)
        val invText = bill.invoiceNumber.ifBlank { "-" }
        val invBadgeW = max(28f, boldValPaint.measureText(invText) + 12f)
        canvas.drawRoundRect(
            RectF(leftValX, y + 2.5f, leftValX + invBadgeW, y + rowH - 2.5f),
            3f,
            3f,
            darkBadgePaint
        )
        canvas.drawText(
            invText,
            leftValX + 6f,
            y + 12f,
            textPaint(8.5f, bold = true, color = Color.WHITE)
        )

        canvas.drawText("Invoice date :-", leftLabelX, y + rowH + 12f, labelPaint)
        canvas.drawText(bill.invoiceDate, leftValX, y + rowH + 12f, boldValPaint)

        canvas.drawText("Reverse Charge (Y/N):-", leftLabelX, y + rowH * 2 + 12f, labelPaint)
        canvas.drawText(bill.reverseCharge, leftValX + 12f, y + rowH * 2 + 12f, boldValPaint)

        canvas.drawText("State :-", leftLabelX, y + rowH * 3 + 12f, labelPaint)
        canvas.drawText(bill.placeState, left + 42f, y + rowH * 3 + 12f, valPaint)
        val pinSplitX = midX - 96f
        canvas.drawLine(pinSplitX, y + rowH * 3, pinSplitX, y + metaH, thinBorderPaint)
        canvas.drawText("Pin Code:", pinSplitX + 4f, y + rowH * 3 + 12f, labelPaint)
        canvas.drawText(bill.placePinCode, pinSplitX + 46f, y + rowH * 3 + 12f, boldValPaint)

        // Right metadata rows
        val rightLabelX = midX + 5f
        val rightValX = midX + 95f
        canvas.drawText("Transport Mode:-", rightLabelX, y + 12f, labelPaint)
        canvas.drawText(bill.transportMode, rightValX, y + 12f, boldValPaint)

        canvas.drawText("Vehicle number:-", rightLabelX, y + rowH + 12f, labelPaint)
        canvas.drawText(bill.vehicleNumber.uppercase(), rightValX, y + rowH + 12f, boldValPaint)

        if (bill.transporterName.isNotBlank()) {
            canvas.drawText("Transporter:-", rightLabelX, y + rowH * 2 + 12f, labelPaint)
            canvas.drawText(bill.transporterName, rightValX, y + rowH * 2 + 12f, valPaint)
        }
        if (bill.driverPhone.isNotBlank()) {
            canvas.drawText("Driver Ph:-", rightLabelX, y + rowH * 3 + 12f, labelPaint)
            canvas.drawText(bill.driverPhone, rightValX, y + rowH * 3 + 12f, valPaint)
        }
        y += metaH

        // 4. RECEIVER & CONSIGNEE HEADER + GRID
        val partyHdrH = 18f
        canvas.drawRect(left, y, right, y + partyHdrH, lightShadePaint)
        canvas.drawRect(left, y, right, y + partyHdrH, borderPaint)
        canvas.drawLine(midX, y, midX, y + partyHdrH, borderPaint)
        canvas.drawText(
            "Detail of Receiver (Billed to)",
            left + (midX - left) / 2f,
            y + 12.5f,
            textPaint(8.8f, bold = true, align = Paint.Align.CENTER)
        )
        canvas.drawText(
            "Detail of Consignee (Shipped to)",
            midX + (right - midX) / 2f,
            y + 12.5f,
            textPaint(8.8f, bold = true, align = Paint.Align.CENTER)
        )
        y += partyHdrH

        val partyBoxH = 82f
        canvas.drawRect(left, y, right, y + partyBoxH, borderPaint)
        canvas.drawLine(midX, y, midX, y + partyBoxH, borderPaint)

        // Receiver rows
        drawPartyGrid(
            canvas = canvas,
            startX = left,
            startY = y,
            width = midX - left,
            height = partyBoxH,
            name = bill.receiverName,
            address = bill.receiverAddress,
            thirdLabel = "PO No.:-",
            thirdValue = bill.receiverPoNo,
            gstinLabel = "ITC GSTIN:-",
            gstinValue = bill.receiverGstin,
            state = bill.receiverState,
            pinCode = bill.receiverPinCode
        )
        // Consignee rows
        drawPartyGrid(
            canvas = canvas,
            startX = midX,
            startY = y,
            width = right - midX,
            height = partyBoxH,
            name = bill.consigneeName,
            address = bill.consigneeAddress,
            thirdLabel = null,
            thirdValue = "",
            gstinLabel = "GSTIN:-",
            gstinValue = bill.consigneeGstin,
            state = bill.consigneeState,
            pinCode = bill.consigneePinCode
        )
        y += partyBoxH

        // 5. LINE ITEMS TABLE
        val colWidths = floatArrayOf(32f, 185f, 54f, 56f, 70f, 60f, 90f) // sum = 547f
        val colX = FloatArray(colWidths.size + 1)
        colX[0] = left
        for (i in colWidths.indices) {
            colX[i + 1] = colX[i] + colWidths[i]
        }

        val itemHdrH = 28f
        canvas.drawRect(left, y, right, y + itemHdrH, shadePaint)
        canvas.drawRect(left, y, right, y + itemHdrH, borderPaint)
        for (i in 1 until colWidths.size) {
            canvas.drawLine(colX[i], y, colX[i], y + itemHdrH, borderPaint)
        }

        val qtyHeader = if (bill.qtyUnitLabel.isNotBlank()) "Qty(${bill.qtyUnitLabel})" else "Qty"
        val headers = listOf(
            "S. No.",
            "Material Description",
            "HSN\ncode",
            "Number\nof Bags",
            qtyHeader,
            "Rate",
            "Amount"
        )
        val hdrPaint = textPaint(8.2f, bold = true, align = Paint.Align.CENTER)
        headers.forEachIndexed { idx, title ->
            val cx = colX[idx] + colWidths[idx] / 2f
            val lines = title.split("\n")
            if (lines.size == 1) {
                canvas.drawText(lines[0], cx, y + 17f, hdrPaint)
            } else {
                canvas.drawText(lines[0], cx, y + 11.5f, hdrPaint)
                canvas.drawText(lines[1], cx, y + 22f, hdrPaint)
            }
        }
        y += itemHdrH

        // Reserve space for bottom blocks:
        // TotalBeforeTax (20) + Tax&Words (106) + TruckAdvance (18) + Bank/Sign (108) + CIN (18) = 270pt
        val footerTotalHeight = 270f
        val tableBottom = bottom - footerTotalHeight
        val minRowH = 20f
        val availableTableH = tableBottom - y

        val itemsToDraw = bill.items
        val itemRowH = if (itemsToDraw.size > 8) {
            max(16f, availableTableH / itemsToDraw.size.toFloat())
        } else {
            minRowH
        }

        var rowY = y
        val rowCountToRender = max(itemsToDraw.size, (availableTableH / itemRowH).toInt().coerceAtLeast(1))
        for (rowIdx in 0 until rowCountToRender) {
            if (rowY + itemRowH > tableBottom + 0.5f) break
            val isAlternate = rowIdx % 2 == 1
            if (isAlternate) {
                canvas.drawRect(left, rowY, right, rowY + itemRowH, lightShadePaint)
            }
            canvas.drawLine(left, rowY + itemRowH, right, rowY + itemRowH, thinBorderPaint)

            if (rowIdx < itemsToDraw.size) {
                val item = itemsToDraw[rowIdx]
                val baseline = rowY + itemRowH * 0.68f
                canvas.drawText(
                    (rowIdx + 1).toString(),
                    colX[0] + colWidths[0] / 2f,
                    baseline,
                    textPaint(8.5f, align = Paint.Align.CENTER)
                )
                drawLeftEllipsizedText(
                    canvas,
                    item.materialDescription,
                    colX[1] + 5f,
                    baseline,
                    colWidths[1] - 10f,
                    textPaint(8.5f, bold = true)
                )
                canvas.drawText(
                    item.hsnCode,
                    colX[2] + colWidths[2] / 2f,
                    baseline,
                    textPaint(8.2f, align = Paint.Align.CENTER)
                )
                canvas.drawText(
                    item.numberOfBags,
                    colX[3] + colWidths[3] / 2f,
                    baseline,
                    textPaint(8.2f, align = Paint.Align.CENTER)
                )
                canvas.drawText(
                    NumberFormatters.formatQuantity(item.qty),
                    colX[4] + colWidths[4] - 5f,
                    baseline,
                    textPaint(8.5f, align = Paint.Align.RIGHT)
                )
                canvas.drawText(
                    NumberFormatters.formatQuantity(item.rate),
                    colX[5] + colWidths[5] - 5f,
                    baseline,
                    textPaint(8.5f, align = Paint.Align.RIGHT)
                )
                canvas.drawText(
                    NumberFormatters.formatAmount(item.amount),
                    colX[6] + colWidths[6] - 5f,
                    baseline,
                    textPaint(8.5f, bold = true, align = Paint.Align.RIGHT)
                )
            }
            rowY += itemRowH
        }

        // Draw crisp outer & column borders for the full table body height
        canvas.drawRect(left, y, right, tableBottom, borderPaint)
        for (i in 1 until colWidths.size) {
            canvas.drawLine(colX[i], y, colX[i], tableBottom, borderPaint)
        }
        y = tableBottom

        // 6. TOTAL BEFORE TAX ROW
        val totalBeforeH = 20f
        val taxLeftX = colX[4] // aligns tax box cleanly under Qty / Rate / Amount
        canvas.drawRect(left, y, right, y + totalBeforeH, shadePaint)
        canvas.drawRect(left, y, right, y + totalBeforeH, borderPaint)
        canvas.drawLine(taxLeftX, y, taxLeftX, y + totalBeforeH, borderPaint)
        canvas.drawLine(colX[6], y, colX[6], y + totalBeforeH, borderPaint)
        canvas.drawText(
            "Total Amount before Tax:",
            taxLeftX - 8f,
            y + 13.5f,
            textPaint(8.8f, bold = true, align = Paint.Align.RIGHT)
        )
        canvas.drawText("₹", taxLeftX + 6f, y + 13.5f, textPaint(8.8f, bold = true))
        canvas.drawText(
            NumberFormatters.formatAmount(bill.totalBeforeTax),
            right - 5f,
            y + 13.5f,
            textPaint(9f, bold = true, align = Paint.Align.RIGHT)
        )
        y += totalBeforeH

        // 7. TAX SUMMARY & AMOUNT IN WORDS
        val taxSectionH = 106f
        val taxRowH = taxSectionH / 6f
        val rateColX = colX[5]
        val amtColX = colX[6]

        canvas.drawRect(left, y, right, y + taxSectionH, borderPaint)
        canvas.drawLine(taxLeftX, y, taxLeftX, y + taxSectionH, borderPaint)
        canvas.drawLine(rateColX, y, rateColX, y + taxSectionH, thinBorderPaint)
        canvas.drawLine(amtColX, y, amtColX, y + taxSectionH, thinBorderPaint)

        // Left pane: Amount in Words
        canvas.drawText("Total Invoice Amount in Words:", left + 6f, y + 15f, textPaint(8.3f, bold = true))
        val words = IndianCurrencyToWords.convert(bill.totalAfterTax)
        drawWrappedText(
            canvas = canvas,
            text = words,
            x = left + 8f,
            y = y + 24f,
            maxWidth = taxLeftX - left - 16f,
            paint = textPaint(9.2f, bold = true)
        )

        // Right pane: 6 tax rows
        val cgstAmt = bill.totalBeforeTax * bill.cgstRate / 100.0
        val sgstAmt = bill.totalBeforeTax * bill.sgstRate / 100.0
        val igstAmt = bill.totalBeforeTax * bill.igstRate / 100.0

        val taxRows = listOf(
            Triple("", "Rate(%)", "Amount(₹)"),
            Triple("CGST", "${formatRate(bill.cgstRate)}%", if (cgstAmt > 0) NumberFormatters.formatAmount(cgstAmt) else "-"),
            Triple("SGST", "${formatRate(bill.sgstRate)}%", if (sgstAmt > 0) NumberFormatters.formatAmount(sgstAmt) else "-"),
            Triple("IGST", "${formatRate(bill.igstRate)}%", if (igstAmt > 0) NumberFormatters.formatAmount(igstAmt) else "0"),
            Triple("Total Tax Amount", "", NumberFormatters.formatAmount(bill.totalTaxAmount)),
            Triple("Total Amount after Tax:", "₹", NumberFormatters.formatAmount(bill.totalAfterTax))
        )

        taxRows.forEachIndexed { idx, (label, midVal, endVal) ->
            val rTop = y + idx * taxRowH
            if (idx == 5) {
                canvas.drawRect(left, rTop, right, rTop + taxRowH, shadePaint)
                canvas.drawRect(taxLeftX, rTop, right, rTop + taxRowH, borderPaint)
            } else if (idx > 0) {
                canvas.drawLine(taxLeftX, rTop, right, rTop, thinBorderPaint)
            }
            val base = rTop + taxRowH * 0.7f
            val isBold = idx == 0 || idx >= 4
            if (label.isNotBlank()) {
                canvas.drawText(label, taxLeftX + 5f, base, textPaint(8.2f, bold = isBold))
            }
            if (midVal.isNotBlank()) {
                canvas.drawText(
                    midVal,
                    rateColX + (amtColX - rateColX) / 2f,
                    base,
                    textPaint(8.2f, bold = isBold, align = Paint.Align.CENTER)
                )
            }
            if (endVal.isNotBlank()) {
                val align = if (idx == 0) Paint.Align.CENTER else Paint.Align.RIGHT
                val ex = if (idx == 0) amtColX + (right - amtColX) / 2f else right - 5f
                canvas.drawText(endVal, ex, base, textPaint(8.4f, bold = isBold, align = align))
            }
        }
        y += taxSectionH

        // 8. TRUCK ADVANCE STRIP
        val truckAdvH = 18f
        canvas.drawRect(left, y, right, y + truckAdvH, lightShadePaint)
        canvas.drawRect(left, y, right, y + truckAdvH, borderPaint)
        val truckAdvLabel = if (bill.truckAdvance > 0.0) {
            "TRUCK ADVANCE:- ₹ ${NumberFormatters.formatAmount(bill.truckAdvance)}"
        } else {
            "TRUCK ADVANCE:- -"
        }
        canvas.drawText(
            truckAdvLabel,
            left + contentWidth / 2f,
            y + 12.5f,
            textPaint(8.5f, bold = true, align = Paint.Align.CENTER)
        )
        y += truckAdvH

        // 9. BANK DETAILS, TRANSPORT FOOTER, TERMS & AUTHORISED SIGNATORY
        val signSectionH = 108f
        val bankRightX = left + contentWidth * 0.57f
        canvas.drawRect(left, y, right, y + signSectionH, borderPaint)
        canvas.drawLine(bankRightX, y, bankRightX, y + signSectionH, borderPaint)

        // Left side: Bank Details (top 54pt) + Driver/Transport/Bhada (18pt) + Terms (36pt)
        val bankH = 52f
        val driverH = 18f
        val bankLabelW = 68f
        canvas.drawLine(left, y + bankH, bankRightX, y + bankH, borderPaint)
        canvas.drawLine(left, y + bankH + driverH, bankRightX, y + bankH + driverH, borderPaint)
        canvas.drawLine(left + bankLabelW, y + 14f, left + bankLabelW, y + bankH, thinBorderPaint)

        val bankTitlePaint = textPaint(8.3f, bold = true).apply { isUnderlineText = true }
        canvas.drawText("Bank Details.", left + 5f, y + 11f, bankTitlePaint)

        val bankRowH = (bankH - 14f) / 3f
        val bankRows = listOf(
            "Bank Name:" to bill.bankName.uppercase(),
            "Bank A/C:" to bill.bankAccountNumber,
            "Bank IFSC:" to bill.bankIfsc.uppercase()
        )
        bankRows.forEachIndexed { idx, (k, v) ->
            val ry = y + 14f + idx * bankRowH
            canvas.drawLine(left, ry, bankRightX, ry, thinBorderPaint)
            canvas.drawText(k, left + 5f, ry + 9.5f, textPaint(8f, bold = true))
            canvas.drawText(
                v,
                left + bankLabelW + (bankRightX - left - bankLabelW) / 2f,
                ry + 9.5f,
                textPaint(8.2f, bold = true, align = Paint.Align.CENTER)
            )
        }

        // Driver / Transport / Bhada bar
        val dY = y + bankH
        canvas.drawRect(left, dY, bankRightX, dY + driverH, shadePaint)
        canvas.drawRect(left, dY, bankRightX, dY + driverH, thinBorderPaint)
        val bhadaText = if (bill.bhadaAmount > 0.0) NumberFormatters.formatQuantity(bill.bhadaAmount) else "-"
        val driverSummary = buildString {
            append("Driver:- ${bill.driverPhone.ifBlank { "-" }}")
            append("  |  Transport:- ${bill.transporterName.ifBlank { "-" }}")
            append("  |  Bhada:- $bhadaText")
        }
        drawLeftEllipsizedText(
            canvas,
            driverSummary,
            left + 5f,
            dY + 12f,
            bankRightX - left - 10f,
            textPaint(7.6f, bold = true)
        )

        // Terms & conditions
        val tY = dY + driverH
        canvas.drawText("Terms & conditions:-", left + 5f, tY + 11f, textPaint(8f, bold = true))
        if (bill.termsAndConditions.isNotBlank()) {
            drawWrappedText(
                canvas,
                bill.termsAndConditions,
                left + 5f,
                tY + 14f,
                bankRightX - left - 10f,
                textPaint(7.5f)
            )
        }

        // Right side: Certification & Authorised Signatory
        val signCenterX = bankRightX + (right - bankRightX) / 2f
        canvas.drawText(
            "Certified that the particulars given above are",
            signCenterX,
            y + 14f,
            textPaint(8f, align = Paint.Align.CENTER)
        )
        canvas.drawText(
            "true and correct",
            signCenterX,
            y + 24f,
            textPaint(8f, align = Paint.Align.CENTER)
        )
        canvas.drawText(
            "COMPANY NAME:- ${bill.sellerName.uppercase()}",
            signCenterX,
            y + 40f,
            textPaint(8.2f, bold = true, align = Paint.Align.CENTER)
        )
        canvas.drawText(
            bill.authorisedSignatory,
            signCenterX,
            y + signSectionH - 22f,
            textPaint(9f, bold = true, align = Paint.Align.CENTER)
        )
        canvas.drawLine(
            bankRightX + 24f,
            y + signSectionH - 16f,
            right - 24f,
            y + signSectionH - 16f,
            thinBorderPaint
        )
        canvas.drawText(
            "Authorised signatory",
            signCenterX,
            y + signSectionH - 6f,
            textPaint(8.3f, bold = true, align = Paint.Align.CENTER)
        )
        y += signSectionH

        // 10. BOTTOM CIN STRIP
        val cinH = bottom - y
        canvas.drawRect(left, y, right, bottom, shadePaint)
        canvas.drawRect(left, y, right, bottom, borderPaint)
        val cinText = if (bill.sellerCinNo.isNotBlank()) "CIN NO :- ${bill.sellerCinNo}" else "CIN NO :-"
        canvas.drawText(
            cinText,
            left + contentWidth / 2f,
            y + cinH * 0.7f,
            textPaint(8f, bold = false, align = Paint.Align.CENTER)
        )
    }

    private fun drawPartyGrid(
        canvas: Canvas,
        startX: Float,
        startY: Float,
        width: Float,
        height: Float,
        name: String,
        address: String,
        thirdLabel: String?,
        thirdValue: String,
        gstinLabel: String,
        gstinValue: String,
        state: String,
        pinCode: String
    ) {
        val labelW = 58f
        val rowHeights = floatArrayOf(15f, 24f, 14f, 14f, 15f)
        var ry = startY
        val lblPaint = textPaint(8f, bold = false)
        val valPaint = textPaint(8.2f, bold = true)
        val smallPaint = textPaint(8f, bold = false)

        // Row 0: Name
        canvas.drawText("Name:-", startX + 5f, ry + 11f, lblPaint)
        drawLeftEllipsizedText(canvas, name, startX + labelW, ry + 11f, width - labelW - 6f, valPaint)
        ry += rowHeights[0]
        canvas.drawLine(startX, ry, startX + width, ry, thinBorderPaint)

        // Row 1: Address (wrapped)
        canvas.drawText("Address:-", startX + 5f, ry + 11f, lblPaint)
        drawWrappedText(canvas, address, startX + labelW, ry + 2f, width - labelW - 6f, smallPaint, maxLines = 2)
        ry += rowHeights[1]
        canvas.drawLine(startX, ry, startX + width, ry, thinBorderPaint)

        // Row 2: PO No or blank
        if (thirdLabel != null) {
            canvas.drawText(thirdLabel, startX + 5f, ry + 10.5f, lblPaint)
            drawLeftEllipsizedText(canvas, thirdValue, startX + labelW, ry + 10.5f, width - labelW - 6f, smallPaint)
        }
        ry += rowHeights[2]
        canvas.drawLine(startX, ry, startX + width, ry, thinBorderPaint)

        // Row 3: GSTIN
        canvas.drawText(gstinLabel, startX + 5f, ry + 10.5f, lblPaint)
        drawLeftEllipsizedText(canvas, gstinValue.uppercase(), startX + labelW, ry + 10.5f, width - labelW - 6f, valPaint)
        ry += rowHeights[3]
        canvas.drawLine(startX, ry, startX + width, ry, thinBorderPaint)

        // Row 4: State & Pin Code
        val pinX = startX + width - 92f
        canvas.drawLine(pinX, ry, pinX, startY + height, thinBorderPaint)
        canvas.drawText("State:-", startX + 5f, ry + 11f, lblPaint)
        drawLeftEllipsizedText(canvas, state, startX + labelW, ry + 11f, pinX - startX - labelW - 4f, smallPaint)
        canvas.drawText("Pin Code:-", pinX + 4f, ry + 11f, lblPaint)
        drawLeftEllipsizedText(canvas, pinCode, pinX + 46f, ry + 11f, startX + width - pinX - 48f, valPaint)
    }

    private fun drawCenteredWrappedText(
        canvas: Canvas,
        text: String,
        centerX: Float,
        topY: Float,
        maxWidth: Float,
        paint: TextPaint
    ) {
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, maxWidth.toInt().coerceAtLeast(10))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(2)
            .build()
        canvas.save()
        canvas.translate(centerX, topY - 8f)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        maxWidth: Float,
        paint: TextPaint,
        maxLines: Int = 3
    ) {
        if (text.isBlank()) return
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, maxWidth.toInt().coerceAtLeast(10))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(maxLines)
            .build()
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawLeftEllipsizedText(
        canvas: Canvas,
        text: String,
        x: Float,
        baselineY: Float,
        maxWidth: Float,
        paint: TextPaint
    ) {
        if (text.isBlank() || maxWidth <= 4f) return
        val clipped = android.text.TextUtils.ellipsize(
            text,
            paint,
            maxWidth,
            android.text.TextUtils.TruncateAt.END
        ).toString()
        canvas.drawText(clipped, x, baselineY, paint)
    }

    private fun formatRate(rate: Double): String {
        return if (rate % 1.0 == 0.0) rate.toInt().toString() else rate.toString()
    }
}
