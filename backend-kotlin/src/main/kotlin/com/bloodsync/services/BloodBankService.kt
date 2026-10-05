package com.bloodsync.services

import com.bloodsync.config.Constants.REQUEST_STATUS
import com.bloodsync.config.FirebaseAdmin.db
import com.google.cloud.firestore.FieldValue
import org.springframework.stereotype.Service

@Service
class BloodBankService(
    private val loggerBotService: LoggerBotService
) {
    private val validBloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    data class StockUpdatePayload(
        val bloodGroup: String?,
        val deltaUnits: Int?,
        val operation: String = "MANUAL_ADJUSTMENT",
        val reason: String = "",
        val performedBy: String = "system_admin"
    )

    data class StockUpdateResult(
        val success: Boolean,
        val previousStock: Int? = null,
        val newStock: Int? = null,
        val bankId: String? = null,
        val bloodGroup: String? = null,
        val deltaUnits: Int? = null,
        val operation: String? = null,
        val error: String? = null
    )

    suspend fun updateStock(bankId: String?, payload: StockUpdatePayload): StockUpdateResult {
        if (db == null) return StockUpdateResult(success = false, error = "Database uninitialized")
        if (bankId.isNullOrBlank()) return StockUpdateResult(success = false, error = "bloodBankId is required")

        val cleanGroup = payload.bloodGroup?.trim()?.uppercase() ?: ""
        if (!validBloodGroups.contains(cleanGroup)) {
            return StockUpdateResult(success = false, error = "Invalid blood group: ${payload.bloodGroup}. Must be one of ${validBloodGroups.joinToString(", ")}")
        }

        val delta = payload.deltaUnits ?: 0
        if (delta == 0) return StockUpdateResult(success = false, error = "deltaUnits must be a non-zero integer")

        val bankRef = db.collection("blood_banks").document(bankId)

        try {
            val result = db.runTransaction { transaction ->
                val bankDoc = transaction.get(bankRef).get()
                if (!bankDoc.exists) {
                    throw RuntimeException("Blood bank $bankId does not exist in registry")
                }

                val data = bankDoc.data ?: mapOf()
                val stocksMap = data["stocks"] as? Map<String, Any> ?: mapOf(
                    "A+" to 40, "A-" to 10, "B+" to 35, "B-" to 8,
                    "AB+" to 15, "AB-" to 4, "O+" to 50, "O-" to 12
                )
                val stocks = stocksMap.mapValues { (it.value as? Number)?.toInt() ?: 0 }.toMutableMap()

                val currentStock = stocks[cleanGroup] ?: 0
                val newStock = currentStock + delta

                if (newStock < 0) {
                    throw RuntimeException("Insufficient stock for $cleanGroup. Available: $currentStock units, requested deduction: ${Math.abs(delta)} units.")
                }

                stocks[cleanGroup] = newStock

                val totalUnits = stocks.values.sum()

                val logRef = bankRef.collection("stock_transactions").document()
                transaction.set(logRef, mapOf(
                    "id" to logRef.id,
                    "bankId" to bankId,
                    "bloodGroup" to cleanGroup,
                    "deltaUnits" to delta,
                    "previousStock" to currentStock,
                    "newStock" to newStock,
                    "operation" to payload.operation,
                    "reason" to payload.reason,
                    "performedBy" to payload.performedBy,
                    "timestamp" to FieldValue.serverTimestamp()
                ))

                val status = if (newStock <= 5) "Critical Low" else if (newStock <= 15) "Low" else "Adequate"

                transaction.set(bankRef, mapOf(
                    "stocks" to stocks,
                    "totalUnitsAvailable" to totalUnits,
                    "bloodStockStatus" to status,
                    "lastStockUpdatedAt" to FieldValue.serverTimestamp()
                ), com.google.cloud.firestore.SetOptions.merge())

                StockUpdateResult(
                    success = true,
                    bankId = bankId,
                    bloodGroup = cleanGroup,
                    previousStock = currentStock,
                    newStock = newStock,
                    deltaUnits = delta,
                    operation = payload.operation
                )
            }.get()

            loggerBotService.logOtpEvent(
                requestId = "stock_${bankId}_${System.currentTimeMillis()}",
                rawEmail = "inventory@bloodsync.org",
                requestStatus = REQUEST_STATUS.SUCCESS,
                otpGenStatus = "SKIPPED",
                emailSendingStatus = "SKIPPED",
                errorCategory = null,
                errorMessage = null,
                metadata = mapOf(
                    "action" to "STOCK_TRANSACTION",
                    "bankId" to result.bankId,
                    "bloodGroup" to result.bloodGroup,
                    "previousStock" to result.previousStock,
                    "newStock" to result.newStock,
                    "deltaUnits" to result.deltaUnits,
                    "operation" to result.operation
                )
            )

            return result
        } catch (e: Exception) {
            println("[BloodBankService] Transaction failed for bank $bankId: ${e.message}")
            return StockUpdateResult(success = false, error = e.message ?: "Transaction failed")
        }
    }

    suspend fun getAllBanks(): List<Map<String, Any>> {
        if (db == null) return emptyList()
        return try {
            val snap = db.collection("blood_banks").get().get()
            snap.documents.map { doc ->
                val data = doc.data
                data["id"] = doc.id
                data
            }
        } catch (e: Exception) {
            println("[BloodBankService] Error fetching blood banks: ${e.message}")
            emptyList()
        }
    }

    suspend fun initializeDefaults() {
        if (db == null) return
        try {
            val snap = db.collection("blood_banks").limit(1).get().get()
            if (snap.isEmpty) {
                println("[BloodBankService] Seeding default blood banks into Firestore...")
                val initialBanks = listOf(
                    mapOf(
                        "id" to "bb_metro_central",
                        "name" to "Metro Central Blood Bank",
                        "address" to "108 Healthcare Blvd, Central District",
                        "phone" to "+91 79 2656 1234",
                        "openHours" to "24/7",
                        "distanceKm" to 2.1,
                        "bloodStockStatus" to "Adequate",
                        "latitude" to 23.0225,
                        "longitude" to 72.5714,
                        "stocks" to mapOf("A+" to 55, "A-" to 12, "B+" to 48, "B-" to 9, "AB+" to 22, "AB-" to 5, "O+" to 68, "O-" to 14)
                    ),
                    mapOf(
                        "id" to "bb_redcross_civic",
                        "name" to "Red Cross Civic Blood Center",
                        "address" to "Ring Road, Civic Center Cross",
                        "phone" to "+91 79 2658 5678",
                        "openHours" to "24/7",
                        "distanceKm" to 4.5,
                        "bloodStockStatus" to "Adequate",
                        "latitude" to 23.0300,
                        "longitude" to 72.5800,
                        "stocks" to mapOf("A+" to 42, "A-" to 8, "B+" to 39, "B-" to 6, "AB+" to 18, "AB-" to 3, "O+" to 52, "O-" to 10)
                    ),
                    mapOf(
                        "id" to "bb_apollo_life",
                        "name" to "Apollo Lifeline Regional Bank",
                        "address" to "Plot 14, SG Highway Medical Zone",
                        "phone" to "+91 79 4000 9999",
                        "openHours" to "24/7",
                        "distanceKm" to 6.8,
                        "bloodStockStatus" to "Adequate",
                        "latitude" to 23.0450,
                        "longitude" to 72.5350,
                        "stocks" to mapOf("A+" to 60, "A-" to 15, "B+" to 50, "B-" to 11, "AB+" to 25, "AB-" to 6, "O+" to 75, "O-" to 18)
                    )
                )

                for (bank in initialBanks) {
                    val stocks = bank["stocks"] as Map<String, Int>
                    val total = stocks.values.sum()
                    
                    val mutableBank = bank.toMutableMap()
                    mutableBank["totalUnitsAvailable"] = total
                    mutableBank["createdAt"] = FieldValue.serverTimestamp()
                    
                    db.collection("blood_banks").document(bank["id"] as String).set(mutableBank).get()
                }
                println("[BloodBankService] Default blood banks seeded successfully.")
            }
        } catch (e: Exception) {
            println("[BloodBankService] Could not initialize default blood banks: ${e.message}")
        }
    }
}
