package org.horizons_server.golf.objects

import kotlinx.serialization.KSerializer
import org.bukkit.Location
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import java.time.LocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encodeToString
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit

object LocationSerializer : KSerializer<Location> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Location", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Location) {
        val string = "${value.x},${value.y},${value.z},${value.world?.name ?: "none"}"
        encoder.encodeString(string)
    }

    override fun deserialize(decoder: Decoder): Location {
        val string = decoder.decodeString()
        val (x, y, z, world) = string.split(",")

        return Location(if (world == "none") null else Bukkit.getWorld(world), x.toDouble(), y.toDouble(), z.toDouble())
    }
}

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString())
    }
}

@Serializable
data class BallOrigin(
    @Serializable(with = LocationSerializer::class) val location: Location,
    @Serializable(with = LocalDateTimeSerializer::class) val throwTime: LocalDateTime? = null
)

class BallOriginDataType : PersistentDataType<String, BallOrigin> {
    override fun getPrimitiveType(): Class<String> {
        return String::class.java
    }

    override fun getComplexType(): Class<BallOrigin> {
        return BallOrigin::class.java
    }

    override fun toPrimitive(complex: BallOrigin, context: PersistentDataAdapterContext): String {
        return Json.encodeToString(complex)
    }

    override fun fromPrimitive(primitive: String, context: PersistentDataAdapterContext): BallOrigin {
        return Json.decodeFromString(primitive)
    }

}