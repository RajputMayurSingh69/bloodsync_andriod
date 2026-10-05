package com.bloodsync.controllers

import com.bloodsync.services.BloodBankService
import com.bloodsync.services.BloodBankService.StockUpdatePayload
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/blood-banks")
class BloodBankController(
    private val bloodBankService: BloodBankService
) {

    @GetMapping("/")
    suspend fun getAllBanks(): ResponseEntity<Any> {
        return try {
            val banks = bloodBankService.getAllBanks()
            ResponseEntity.ok(mapOf("success" to true, "count" to banks.size, "data" to banks))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(mapOf("success" to false, "error" to e.message))
        }
    }

    @PostMapping("/{id}/update-stock")
    suspend fun updateStockById(
        @PathVariable id: String,
        @RequestBody payload: StockUpdatePayload
    ): ResponseEntity<Any> {
        val result = bloodBankService.updateStock(id, payload)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }

    @PostMapping("/update-stock")
    suspend fun updateStock(
        @RequestBody payload: Map<String, Any>
    ): ResponseEntity<Any> {
        val bankId = (payload["bloodBankId"] ?: payload["id"] ?: payload["bankId"]) as? String
        
        val updatePayload = StockUpdatePayload(
            bloodGroup = payload["bloodGroup"] as? String,
            deltaUnits = (payload["deltaUnits"] as? Number)?.toInt(),
            operation = payload["operation"] as? String ?: "MANUAL_ADJUSTMENT",
            reason = payload["reason"] as? String ?: "",
            performedBy = payload["performedBy"] as? String ?: "system_admin"
        )
        
        val result = bloodBankService.updateStock(bankId, updatePayload)
        return if (result.success) {
            ResponseEntity.ok(result)
        } else {
            ResponseEntity.badRequest().body(result)
        }
    }
}
