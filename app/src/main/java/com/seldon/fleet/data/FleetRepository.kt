package com.seldon.fleet.data

import android.content.Context
import com.seldon.fleet.domain.Device
import com.seldon.fleet.domain.DeviceRepository
import org.json.JSONObject

object EnergyNormalizer {
    fun toMah(consumed: Double, unit: String, nominalVoltage: Double?): Double? = when (unit.lowercase()) {
        "mah" -> consumed
        "mwh" -> nominalVoltage?.takeIf { it > 0 }?.let { consumed / it }
        else -> null
    }
}

class FleetRepository(private val context: Context) : DeviceRepository {
    override fun load(): List<Device> {
        val json = context.assets.open("frota.json").bufferedReader().use { it.readText() }
        val items = JSONObject(json).getJSONArray("devices")
        return (0 until items.length()).map { i ->
            val o = items.getJSONObject(i)
            Device(
                id = o.getString("id"), model = o.getString("model"), soc = o.getString("soc"), tier = o.getString("tier"),
                ramMb = o.getInt("ramMb"), androidApi = o.getInt("androidApi"),
                batteryCapacityMah = if (o.has("batteryCapacityMah") && !o.isNull("batteryCapacityMah")) o.getInt("batteryCapacityMah") else null,
                batteryLevelPct = o.getInt("batteryLevelPct"), screenOnMinutes = o.optIntOrNull("screenOnMinutes"),
                energyMah = EnergyNormalizer.toMah(o.getDouble("energyConsumed"), o.getString("energyUnit"), if (o.has("nominalVoltage") && !o.isNull("nominalVoltage")) o.getDouble("nominalVoltage") else null),
                jankFramesPct = o.optDoubleOrNull("jankFramesPct"), crashCount7d = o.optIntOrNull("crashCount7d"), lastSeen = o.getString("lastSeen")
            )
        }
    }

    private fun JSONObject.optIntOrNull(name: String): Int? = if (has(name) && !isNull(name)) optInt(name) else null
    private fun JSONObject.optDoubleOrNull(name: String): Double? = if (has(name) && !isNull(name)) optDouble(name) else null
}
