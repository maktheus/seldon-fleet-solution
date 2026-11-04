package com.seldon.fleet.domain

data class Device(
    val id: String,
    val model: String,
    val soc: String,
    val tier: String,
    val ramMb: Int,
    val androidApi: Int,
    val batteryCapacityMah: Int?,
    val batteryLevelPct: Int,
    val screenOnMinutes: Int,
    val energyConsumed: Double,
    val energyUnit: String,
    val nominalVoltage: Double?,
    val jankFramesPct: Double,
    val crashCount7d: Int,
    val lastSeen: String
)

enum class DeviceStatus { OK, ATENCAO, CRITICO, INDETERMINADO }
data class Decision(
    val status: DeviceStatus,
    val energyMah: Double? = null,
    val drainRelative: Double? = null,
    val reason: String
)

class ClassifyDevice {
    fun invoke(device: Device): Decision {
        if (device.batteryCapacityMah == null || device.batteryCapacityMah <= 0) return Decision(
            DeviceStatus.INDETERMINADO,
            reason = "capacidade da bateria ausente ou igual a zero"
        )
        if (device.screenOnMinutes <= 0) return Decision(
            DeviceStatus.INDETERMINADO,
            reason = "tempo de tela igual a zero"
        )
        val energy = when (device.energyUnit.lowercase()) {
            "mah" -> device.energyConsumed
            "mwh" -> device.nominalVoltage?.takeIf { it > 0 }?.let { device.energyConsumed / it }
            else -> null
        } ?: return Decision(
            DeviceStatus.INDETERMINADO,
            reason = "unidade de energia sem tensão nominal válida"
        )
        val drain = energy / (device.screenOnMinutes / 60.0)
        val relative = drain / device.batteryCapacityMah * 100.0
        val status = when {
            relative >= 12 || device.crashCount7d >= 3 || device.jankFramesPct >= 15 -> DeviceStatus.CRITICO
            relative >= 7 || device.crashCount7d >= 1 || device.jankFramesPct >= 8 -> DeviceStatus.ATENCAO
            else -> DeviceStatus.OK
        }
        val reason = when {
            relative >= 12 -> "dreno de %.1f %%/h, acima do limiar de 12 %%/h".format(relative)
            device.crashCount7d >= 3 -> "%.0f falhas nos últimos 7 dias, acima do limite de 3".format(
                device.crashCount7d.toDouble()
            )

            device.jankFramesPct >= 15 -> "%.1f%% de frames perdidos, acima do limiar de 15%%".format(
                device.jankFramesPct
            )

            relative >= 7 -> "dreno de %.1f %%/h, acima do limiar de 7 %%/h".format(relative)
            device.crashCount7d >= 1 -> "%.0f falha(s) nos últimos 7 dias".format(device.crashCount7d.toDouble())
            device.jankFramesPct >= 8 -> "%.1f%% de frames perdidos, acima do limiar de 8%%".format(
                device.jankFramesPct
            )

            else -> "nenhum sinal ultrapassou os limiares"
        }
        return Decision(status, energy, relative, reason)
    }
}
