// File: app/src/main/java/com/viplove/licadvisornative/util/PdfGenerator.kt
package com.viplove.licadvisornative.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory
import com.viplove.licadvisornative.model.ClientDataSheet
import com.viplove.licadvisornative.model.PersonDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

object PdfGenerator {

    // A data class to hold the state of the PDF generation process.
    // This makes the code cleaner by not having to pass multiple parameters around.
    private data class PdfGenerationState(
        var document: PDDocument,
        var contentStream: PDPageContentStream,
        var yPosition: Float,
        val pageHeight: Float = PDRectangle.A4.height,
        val topMargin: Float = 780f,
        val bottomMargin: Float = 50f,
        val leftMargin: Float = 50f,
        val lineSpacing: Float = 16f
    )

    suspend fun generatePdfs(context: Context, lead: ClientDataSheet, combined: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                if (combined) {
                    generateCombinedPdf(context, lead)
                } else {
                    generateDataSheetPdf(context, lead)
                    generateDocumentsPdf(context, lead)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "PDF(s) saved to Downloads folder.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error creating PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun generateCombinedPdf(context: Context, lead: ClientDataSheet) {
        val document = PDDocument()
        try {
            addDataSheetPage(document, lead)
            val allUris = getAllDocumentUris(lead)
            allUris.forEach { (label, uriString) ->
                addDocumentImagePage(document, context, label, uriString)
            }
            val fileName = "COMBINED_${lead.proposerDetails.name.replace(" ", "_")}.pdf"
            savePdfToDownloads(context, document, fileName)
        } finally {
            document.close()
        }
    }

    private fun generateDataSheetPdf(context: Context, lead: ClientDataSheet) {
        val document = PDDocument()
        try {
            addDataSheetPage(document, lead)
            val fileName = "DATASHEET_${lead.proposerDetails.name.replace(" ", "_")}.pdf"
            savePdfToDownloads(context, document, fileName)
        } finally {
            document.close()
        }
    }

    private fun generateDocumentsPdf(context: Context, lead: ClientDataSheet) {
        val allUris = getAllDocumentUris(lead)
        if (allUris.isEmpty()) return

        val document = PDDocument()
        try {
            allUris.forEach { (label, uriString) ->
                addDocumentImagePage(document, context, label, uriString)
            }
            val fileName = "DOCUMENTS_${lead.proposerDetails.name.replace(" ", "_")}.pdf"
            savePdfToDownloads(context, document, fileName)
        } finally {
            document.close()
        }
    }

    /**
     * Core function to add a new page if the current one is full.
     * This is the main fix to prevent content from being cut off.
     */
    private fun checkAndAddNewPage(state: PdfGenerationState) {
        if (state.yPosition < state.bottomMargin) {
            state.contentStream.close()
            val newPage = PDPage(PDRectangle.A4)
            state.document.addPage(newPage)
            state.contentStream = PDPageContentStream(state.document, newPage)
            state.yPosition = state.topMargin
        }
    }

    private fun addDataSheetPage(document: PDDocument, lead: ClientDataSheet) {
        val page = PDPage(PDRectangle.A4)
        document.addPage(page)
        val contentStream = PDPageContentStream(document, page)
        val state = PdfGenerationState(document, contentStream, 780f)

        try {
            // All drawing functions now take the 'state' object and update it.
            writeLine(state, "Client Data Sheet", isBold = true)
            writeLine(state, "Proposer: ${lead.proposerDetails.name}", isBold = true)
            state.yPosition -= state.lineSpacing // Add extra space

            writeLine(state, "PLAN DETAILS", isBold = true)

            val planText = lead.selectedPlan?.let { "${it.name} (${it.planNumber})" }
            val termText = lead.selectedTerm?.toString()
            val pptText = lead.selectedPpt?.toString()

            writePair(state, "Plan", planText)
            writePair(state, "Policy Term (Years)", termText)
            writePair(state, "Premium Paying Term (Years)", pptText)
            writePair(state, "Sum Assured", lead.sumAssured)
            writePair(state, "Mode", lead.mode)
            val doc = lead.dateOfCommencement?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it)) }
            writePair(state, "Date of Commencement", doc)
            if (lead.isNachMandatory == "Yes") {
                val nachDate = lead.nachDate?.let { SimpleDateFormat("dd/MM/yyyy", Locale.US).format(Date(it)) }
                writePair(state, "NACH Date", nachDate)
            }
            state.yPosition -= state.lineSpacing

            val proposerTitle = if (lead.initialQuestions.proposerIsLA) "PROPOSER & LIFE ASSURED DETAILS" else "PROPOSER DETAILS"
            writePersonDetails(state, proposerTitle, lead.proposerDetails)

            if (!lead.initialQuestions.proposerIsLA) {
                val laTitle = if (lead.initialQuestions.lifeAssuredIs == "Spouse") "LIFE ASSURED (SPOUSE) DETAILS" else "LIFE ASSURED (CHILD) DETAILS"
                writePersonDetails(state, laTitle, lead.lifeAssuredDetails)
            }
        } finally {
            state.contentStream.close()
        }
    }

    // Refactored drawing functions to use the state object and the page-break check.

    private fun writeLine(state: PdfGenerationState, text: String, isBold: Boolean = false) {
        checkAndAddNewPage(state)
        state.contentStream.beginText()
        state.contentStream.setFont(if (isBold) PDType1Font.HELVETICA_BOLD else PDType1Font.HELVETICA, 10f)
        state.contentStream.newLineAtOffset(state.leftMargin, state.yPosition)
        state.contentStream.showText(text)
        state.contentStream.endText()
        state.yPosition -= state.lineSpacing
    }

    private fun writePair(state: PdfGenerationState, label: String, value: String?) {
        if (value.isNullOrBlank()) return
        checkAndAddNewPage(state)
        val indent = 15f
        state.contentStream.beginText()
        state.contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10f)
        state.contentStream.newLineAtOffset(state.leftMargin + indent, state.yPosition)
        state.contentStream.showText("$label:")
        state.contentStream.endText()

        state.contentStream.beginText()
        state.contentStream.setFont(PDType1Font.HELVETICA, 10f)
        state.contentStream.newLineAtOffset(state.leftMargin + indent + 150f, state.yPosition)
        state.contentStream.showText(value)
        state.contentStream.endText()
        state.yPosition -= state.lineSpacing
    }

    private fun writePersonDetails(state: PdfGenerationState, title: String, person: PersonDetails) {
        writeLine(state, title, isBold = true)
        writePair(state, "Name", person.name)
        writePair(state, "Contact No", person.contactNo)
        writePair(state, "Email ID", person.emailId)
        writePair(state, "Aadhar No", person.adharCardNo)
        writePair(state, "PAN No", person.panCardNo)
        writePair(state, "Marital Status", person.maritalStatus)
        if (person.maritalStatus == "Married") {
            writePair(state, "Spouse Name", person.spouseName)
        }
        writePair(state, "Father's Name", person.fatherName)
        writePair(state, "Mother's Name", person.motherName)
        writePair(state, "IT Assessee", person.taxAssessee)
        state.yPosition -= (state.lineSpacing * 0.5f) // Spacer

        writePair(state, "Occupation", person.occupation)
        writePair(state, "Nature of Duties", person.natureOfDuties)
        writePair(state, "Length of Service", person.lengthOfService)
        writePair(state, "Employer", person.employerName)
        writePair(state, "Annual Income", person.annualIncome)
        writePair(state, "Education", person.education)
        state.yPosition -= (state.lineSpacing * 0.5f) // Spacer

        writePair(state, "Height (cm)", person.heightCm)
        writePair(state, "Weight (kg)", person.weightKg)
        writePair(state, "State of Health", person.stateOfHealth)
        state.yPosition -= state.lineSpacing // Extra space after section
    }

    private fun addDocumentImagePage(document: PDDocument, context: Context, label: String, uriString: String) {
        val uri = Uri.parse(uriString)
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val page = PDPage(PDRectangle.A4)
            document.addPage(page)
            val contentStream = PDPageContentStream(document, page)

            try {
                contentStream.beginText()
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 14f)
                contentStream.newLineAtOffset(50f, 800f)
                contentStream.showText(label)
                contentStream.endText()

                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                val jpeg = JPEGFactory.createFromStream(document, out.toByteArray().inputStream())

                val pageWidth = page.mediaBox.width - 100
                val pageHeight = 750f
                val scale = min(pageWidth / jpeg.width, pageHeight / jpeg.height)
                val imgWidth = jpeg.width * scale
                val imgHeight = jpeg.height * scale

                contentStream.drawImage(jpeg, 50f, 750f - imgHeight, imgWidth, imgHeight)
            } finally {
                contentStream.close()
            }
        }
    }

    private fun getAllDocumentUris(lead: ClientDataSheet): List<Pair<String, String>> {
        val proposer = lead.proposerDetails
        val lifeAssured = lead.lifeAssuredDetails
        val docs = mutableListOf<Pair<String, String>>()

        proposer.aadhaarFrontUri?.let { docs.add("Proposer Aadhaar (Front)" to it) }
        proposer.aadhaarBackUri?.let { docs.add("Proposer Aadhaar (Back)" to it) }
        proposer.panCardUri?.let { docs.add("Proposer PAN Card" to it) }
        proposer.passportPhotoUri?.let { docs.add("Proposer Passport Photo" to it) }
        proposer.bankProofUri?.let { docs.add("Proposer Bank Proof" to it) }
        proposer.incomeProofUri?.let { docs.add("Proposer Income Proof" to it) }

        if (!lead.initialQuestions.proposerIsLA) {
            lifeAssured.aadhaarFrontUri?.let { docs.add("Life Assured Aadhaar (Front)" to it) }
            lifeAssured.aadhaarBackUri?.let { docs.add("Life Assured Aadhaar (Back)" to it) }
            lifeAssured.panCardUri?.let { docs.add("Life Assured PAN Card" to it) }
            lifeAssured.passportPhotoUri?.let { docs.add("Life Assured Passport Photo" to it) }
            lifeAssured.bankProofUri?.let { docs.add("Life Assured Bank Proof" to it) }
            lifeAssured.incomeProofUri?.let { docs.add("Life Assured Income Proof" to it) }
            lifeAssured.birthCertificateUri?.let { docs.add("Life Assured Birth Certificate" to it) }
        }

        return docs
    }

    private fun savePdfToDownloads(context: Context, document: PDDocument, fileName: String) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/LIC_Udaan")
            }
        }
        val uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
        if (uri != null) {
            resolver.openOutputStream(uri).use { outputStream ->
                document.save(outputStream)
            }
        }
    }
}
