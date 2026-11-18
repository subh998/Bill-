package com.example

import android.graphics.Bitmap
import android.graphics.Canvas
import com.example.data.model.BillLineItem
import com.example.data.model.CopyType
import com.example.data.model.SupplyBill
import com.example.export.BillDocumentRenderer
import com.example.export.IndianCurrencyToWords
import com.example.export.NumberFormatters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BillExportTest {

    @Test
    fun `indian currency formatting and words match reference bill`() {
        assertEquals("9,60,750.00", NumberFormatters.formatAmount(960750.0))
        assertEquals(
            "Rupees Nine Lakh Sixty Thousand Seven Hundred Fifty Only",
            IndianCurrencyToWords.convert(960750.0)
        )
    }

    @Test
    fun `renders 1654x2339 PNG bitmap and A4 canvas layout cleanly`() {
        val sampleBill = SupplyBill(
            id = "bill_114",
            userId = "test_uid",
            documentType = "Bill of Supply",
            copyType = CopyType.ORIGINAL.name,
            invoiceNumber = "114",
            invoiceDate = "10/11/2026",
            reverseCharge = "N",
            placeState = "BIHAR",
            placePinCode = "824124",
            transportMode = "by road",
            vehicleNumber = "UP53ET-3170",
            transporterName = "Chandal Transport",
            driverPhone = "7869765225",
            bhadaAmount = 140.0,
            truckAdvance = 30000.0,
            sellerName = "BALAJEE TRADERS",
            sellerAddress = "BEL ROAD OBRA , CHAPARI AURANGABAD",
            sellerState = "BIHAR",
            sellerPinCode = "824124",
            sellerGstin = "10BMCPG9469E1Z9",
            bankName = "BALAJEE TRADERS",
            bankAccountNumber = "132505500431",
            bankIfsc = "ICIC0001325",
            authorisedSignatory = "Chandan Kr Gupta",
            receiverName = "Janki Traders",
            receiverAddress = "Aurangabad, delivery at - pahleja Dehri",
            receiverGstin = "10AEOPJ9299Q1ZJ",
            receiverState = "BIHAR",
            consigneeState = "Uttar Pradesh",
            qtyUnitLabel = "in MT",
            items = listOf(
                BillLineItem(
                    serialNo = 1,
                    materialDescription = "Rice",
                    qty = 30500.0,
                    rate = 31.5,
                    amount = 960750.0
                )
            ),
            totalBeforeTax = 960750.0,
            totalTaxAmount = 0.0,
            totalAfterTax = 960750.0
        )

        // Verify exact A4 page canvas drawing pipeline used by both PDF and PNG exporters
        val a4Bitmap = Bitmap.createBitmap(
            BillDocumentRenderer.A4_WIDTH_PT,
            BillDocumentRenderer.A4_HEIGHT_PT,
            Bitmap.Config.ARGB_8888
        )
        try {
            BillDocumentRenderer.drawBillPage(Canvas(a4Bitmap), sampleBill)
        } finally {
            a4Bitmap.recycle()
        }

        // Verify high-DPI PNG render and compression
        val bitmap = BillDocumentRenderer.renderBitmap(sampleBill)
        try {
            assertEquals(BillDocumentRenderer.PNG_WIDTH_PX, bitmap.width)
            assertEquals(BillDocumentRenderer.PNG_HEIGHT_PX, bitmap.height)
            val pngOut = ByteArrayOutputStream()
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, pngOut))
            assertTrue(pngOut.size() > 100)
        } finally {
            bitmap.recycle()
        }
    }
}
