package org.horizons_server.golf.objects

import org.bukkit.Location
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import java.io.*
import java.time.LocalDateTime

data class BallOrigin(val location: Location, val throwTime: LocalDateTime? = null)

class BallOriginDataType : PersistentDataType<ByteArray, BallOrigin> {
    override fun getPrimitiveType(): Class<ByteArray> {
        return ByteArray::class.java
    }

    override fun getComplexType(): Class<BallOrigin> {
        return BallOrigin::class.java
    }

    override fun toPrimitive(complex: BallOrigin, context: PersistentDataAdapterContext): ByteArray {
        val bos = ByteArrayOutputStream()
        val out: ObjectOutputStream?
        val array: ByteArray

        try {
            out = ObjectOutputStream(bos)
            out.writeObject(complex)
            out.flush()

            array = bos.toByteArray()
        } finally {
            try {
                bos.close()
            } catch (e: IOException) {
                TODO("Do something")
            }
        }

        return array
    }

    override fun fromPrimitive(primitive: ByteArray, context: PersistentDataAdapterContext): BallOrigin {
        val bis = ByteArrayInputStream(primitive)
        var input: ObjectInput? = null
        val output: BallOrigin

        try {
            input = ObjectInputStream(bis)
            output = input.readObject() as BallOrigin
        } finally {
            try {
                input?.close()
            } catch (e: IOException) {
                TODO("DO SOMETHING")
            }
        }

        return output
    }

}