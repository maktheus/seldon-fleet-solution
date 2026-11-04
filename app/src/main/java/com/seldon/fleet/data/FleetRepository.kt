package com.seldon.fleet.data

import android.content.Context
import com.seldon.fleet.domain.Device
import org.json.JSONObject

class FleetRepository(private val context: Context) {
    fun load(): List<Device> {
        val json = context.assets.open("frota.json").bufferedReader().use { it.readText() }
        val items = JSONObject(json).getJSONArray("devices")
        return (0 until items.length()).map { i ->
            val o = items.getJSONObject(i)
            Device(
                o.getString("id"),
                o.getString("model"),
                o.getString("soc"),
                o.getString("tier"),
                o.getInt("ramMb"),
                o.getInt("androidApi"),
                if (o.has("batteryCapacityMah") && !o.isNull("batteryCapacityMah")) o.getInt("batteryCapacityMah") else null,
                o.getInt("batteryLevelPct"),
                o.getInt("screenOnMinutes"),
                o.getDouble("energyConsumed"),
                o.getString("energyUnit"),
                if (o.has("nominalVoltage") && !o.isNull("nominalVoltage")) o.getDouble("nominalVoltage") else null,
                o.getDouble("jankFramesPct"),
                o.getInt("crashCount7d"),
                o.getString("lastSeen")
            )
        }
    }
}
