package com.nvllz.stepsy.util

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.Metadata
import android.util.Log
import java.time.Instant
import java.time.ZoneId

object HealthConnectManager {
    private const val TAG = "HealthConnectManager"

    @Volatile
    private var client: HealthConnectClient? = null

    val permissions = setOf(
        HealthPermission.getWritePermission(StepsRecord::class)
    )

    fun isAvailable(context: Context): Boolean {
        return HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    fun getClient(context: Context): HealthConnectClient {
        return client ?: HealthConnectClient.getOrCreate(context).also { client = it }
    }

    fun permissionContract() = PermissionController.createRequestPermissionResultContract()

    suspend fun hasPermissions(context: Context): Boolean {
        return try {
            val granted = getClient(context).permissionController.getGrantedPermissions()
            granted.containsAll(permissions)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to check permissions: ${e.message}")
            false
        }
    }

    suspend fun writeStepsDelta(context: Context, deltaSteps: Int, startTime: Instant, endTime: Instant) {
        if (deltaSteps <= 0) return
        if (!hasPermissions(context)) return
        if (!endTime.isAfter(startTime)) return

        try {
            val zoneId = ZoneId.systemDefault()
            val record = StepsRecord(
                count = deltaSteps.toLong(),
                startTime = startTime,
                endTime = endTime,
                startZoneOffset = zoneId.rules.getOffset(startTime),
                endZoneOffset = zoneId.rules.getOffset(endTime),
                metadata = androidx.health.connect.client.records.metadata.Metadata.autoRecorded(
                    device = androidx.health.connect.client.records.metadata.Device(
                        type = androidx.health.connect.client.records.metadata.Device.TYPE_PHONE
                    )
                )
            )
            getClient(context).insertRecords(listOf(record))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to write steps to Health Connect: ${e.message}")
        }
    }

    
}