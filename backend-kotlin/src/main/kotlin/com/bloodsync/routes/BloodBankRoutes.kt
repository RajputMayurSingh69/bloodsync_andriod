package com.bloodsync.routes

import com.bloodsync.services.BloodBankService
import com.bloodsync.services.BloodBankService.StockUpdatePayload
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.bloodBankRoutes() {
    val bloodBankService = BloodBankService()

    route("/blood-banks") {
        get("/") {
            try {
                val banks = bloodBankService.getAllBanks()
                call.respond(HttpStatusCode.OK, mapOf("success" to true, "count" to banks.size, "data" to banks))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, mapOf("success" to false, "error" to e.message))
            }
        }
        
        post("/{id}/update-stock") {
            val id = call.parameters["id"]
            val payload = try {
                call.receive<StockUpdatePayload>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Invalid payload"))
                return@post
            }
            
            val result = bloodBankService.updateStock(id, payload)
            if (result.success) {
                call.respond(HttpStatusCode.OK, result)
            } else {
                call.respond(HttpStatusCode.BadRequest, result)
            }
        }
        
        post("/update-stock") {
            val payload = try {
                call.receive<Map<String, Any>>()
            } catch (e: Exception) {
                call.respond(HttpStatusCode.BadRequest, mapOf("success" to false, "message" to "Invalid payload"))
                return@post
            }
            
            val bankId = (payload["bloodBankId"] ?: payload["id"] ?: payload["bankId"]) as? String
            
            val updatePayload = StockUpdatePayload(
                bloodGroup = payload["bloodGroup"] as? String,
                deltaUnits = (payload["deltaUnits"] as? Number)?.toInt(),
                operation = payload["operation"] as? String ?: "MANUAL_ADJUSTMENT",
                reason = payload["reason"] as? String ?: "",
                performedBy = payload["performedBy"] as? String ?: "system_admin"
            )
            
            val result = bloodBankService.updateStock(bankId, updatePayload)
            if (result.success) {
                call.respond(HttpStatusCode.OK, result)
            } else {
                call.respond(HttpStatusCode.BadRequest, result)
            }
        }
    }
}
