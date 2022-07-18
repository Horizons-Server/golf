package org.horizons_server.golf.listener

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.EnderPearl
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerRiptideEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.persistence.PersistentDataType
import org.horizons_server.golf.Golf
import org.horizons_server.golf.getAdjacent
import org.horizons_server.golf.objects.BallOrigin
import org.horizons_server.golf.objects.BallOriginDataType
import java.time.LocalDateTime

class BounceListener(private val base: Golf) : Listener {
    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        if (event.entityType == EntityType.ENDER_PEARL && event.entity.shooter is Player) {

            val p = event.entity.shooter as Player

            if (!base.enabled.contains(p.uniqueId) || base.disabledWorlds.contains(p.world.name)) return

            val old = event.entity as EnderPearl

            // The vector to "bounce" off of
            val n = event.hitEntity?.let {
                // Treat the plane orthogonal to the vector between the pearl and the entity as a solid plane to bounce off of
                old.location.toVector().subtract(event.hitEntity!!.boundingBox.center).normalize()
            } ?: event.hitBlockFace!!.direction

            val data = old.persistentDataContainer
            val location = old.persistentDataContainer.get(base.ballOrigin, BallOriginDataType())?.location
            var bounceCount = 1

            if (data.has(base.bounces, PersistentDataType.INTEGER)) {
                bounceCount = data.get(base.bounces, PersistentDataType.INTEGER)!!
                // Allow the player to teleport if it's the last bounce
                if (bounceCount == base.maxBounces) {
                    if (old.shooter is Player) {
                        base.allowed.add((old.shooter as Player).uniqueId)
                    }
                } else if (bounceCount > base.maxBounces) {
                    if (old.location.block.isLiquid && old.shooter is Player) {
                        (old.shooter as Player).sendTitle(
                            "${ChatColor.DARK_BLUE} Water!", "${ChatColor.DARK_AQUA} Position Reset", 10, 70, 20
                        )

                        old.remove()
                    }

                    location?.let {
                        (old.shooter as Player).teleport(location)
                    }

                    return
                }
            }

            val reflection = old.velocity.clone().subtract(
                n.multiply(2 * old.velocity.dot(n))
            ).multiply(base.bounciness)

            val pearlNew = old.world.spawn(old.location, EnderPearl::class.java)
            pearlNew.shooter = old.shooter
            old.remove()

            pearlNew.velocity = reflection
            val newData = pearlNew.persistentDataContainer
            newData.set(base.bounces, PersistentDataType.INTEGER, bounceCount + 1)
            location?.let { newData.set(base.ballOrigin, BallOriginDataType(), BallOrigin(location)) }
            if (base.bounceSoundEnabled) {
                pearlNew.world.playSound(
                    pearlNew.location,
                    base.bounceSound,
                    base.bounceSoundVolume,
                    base.bounceSoundPitch
                )
            }
            // Launch the event for other plugins to use
            val launchEvent = ProjectileLaunchEvent(pearlNew)
            Bukkit.getPluginManager().callEvent(launchEvent)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPlayerRiptide(event: PlayerRiptideEvent) {
        val p = event.player

        if (base.enabled.contains(p.uniqueId)) {
            p.persistentDataContainer.set(
                base.ballOrigin, BallOriginDataType(), BallOrigin(p.location, LocalDateTime.now())
            )
        }
    }

    @EventHandler
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        if (event.entityType == EntityType.ENDER_PEARL && event.entity.shooter is Player) {
            val p = event.entity.shooter as Player

            // If they don't have permission of if in a disabled world, ensure the destination teleport behaves normally
            if (!base.enabled.contains(p.uniqueId) || base.disabledWorlds.contains(p.world.name)) {
                // Prevent further teleports and bounces
                base.allowed.add(p.uniqueId)
                event.entity.persistentDataContainer.set(base.bounces, PersistentDataType.INTEGER, base.maxBounces + 1)
                return
            }

            // if the person has the golf persistent data, and it is within 5 seconds, then we use that that location.
            // otherwise we use the location of the pearl throw, we don't want to have midair throws


            val throwLocation = if (p.persistentDataContainer.has(base.ballOrigin, BallOriginDataType())) {
                val data = p.persistentDataContainer.get(base.ballOrigin, BallOriginDataType())!!
                val recent = data.throwTime?.plusSeconds(5L)?.isAfter(LocalDateTime.now())
                p.persistentDataContainer.remove(base.ballOrigin)

                if (recent == true) data.location
                else p.location
            } else p.location

            val location = if (event.entity.persistentDataContainer.has(
                    base.bounces, PersistentDataType.INTEGER
                )
            ) event.entity.location else throwLocation

            event.entity.persistentDataContainer.set(base.ballOrigin, BallOriginDataType(), BallOrigin(throwLocation))

            val block = location.block
            val adj = block.getAdjacent()

            val totalNotAir = adj.count { it.type != Material.AIR }
            val totalNotSand = adj.count { it.type != Material.SAND }
            val totalFairway =
                adj.count { it.type == Material.WATER || it.type == Material.LIME_TERRACOTTA || it.type == Material.GREEN_CONCRETE_POWDER }

            // go down the tree from worst to best
            if (totalNotSand.toDouble() < totalNotAir.toDouble() / 2.0) {
                event.entity.velocity = event.entity.velocity.multiply(base.sandDebuff)
            } else if (totalFairway.toDouble() < totalNotAir.toDouble() / 2.0) {
                event.entity.velocity = event.entity.velocity.multiply(base.nonFairwayDebuff)
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun onTeleport(event: PlayerTeleportEvent) {
        if (event.cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL && base.enabled.contains(event.player.uniqueId)) {
            if (base.allowed.contains(event.player.uniqueId)) {
                base.allowed.remove(event.player.uniqueId)
            } else {
                event.isCancelled = true
            }
        }
    }
}