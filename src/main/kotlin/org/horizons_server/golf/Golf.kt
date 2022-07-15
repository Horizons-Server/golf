package org.horizons_server.golf

import org.bukkit.*
import org.bukkit.entity.EnderPearl
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerRiptideEvent
import org.bukkit.event.player.PlayerTeleportEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin
import org.horizons_server.golf.command.GolfCommand
import org.horizons_server.golf.command.WaterCommand
import org.horizons_server.golf.objects.BallOrigin
import org.horizons_server.golf.objects.BallOriginDataType
import java.time.LocalDateTime
import java.util.*
import java.util.logging.Level


class Golf : JavaPlugin(), Listener {
    private lateinit var bounces: NamespacedKey
    private lateinit var ballOrigin: NamespacedKey

    private val allowed: MutableSet<UUID> = HashSet()

    private var bounciness = 0.8
    private var maxBounces = 5
    private var bounceSoundEnabled = false
    private var bounceSound: Sound = Sound.BLOCK_SLIME_BLOCK_FALL
    private var sandDebuff = 0.2
    private var nonFairwayDebuff = 0.4
    private var bounceSoundVolume = 0f
    private var bounceSoundPitch = 0f
    private val disabledWorlds: HashSet<String> = HashSet()

//    private var worldGuardHelper: WorldGuardHelper? = null

    companion object {
        fun getPlugin() = getPlugin(Golf::class.java)
    }

    override fun onEnable() {
        bounces = NamespacedKey(this, "bounces")
        ballOrigin = NamespacedKey(this, "startingBlock")
        server.pluginManager.registerEvents(this, this)

        val golfCommand = GolfCommand(this)
        getCommand("golfreload")?.setExecutor(golfCommand)
        getCommand("golfreload")?.tabCompleter = golfCommand

        val waterCommand = WaterCommand()
        getCommand("water")?.setExecutor(waterCommand)
        getCommand("water")?.tabCompleter = waterCommand

//        worldGuardHelper = try {
//            WorldGuardHelper()
//        } catch (e: NoClassDefFoundError) {
//            null
//        }

        reload()
    }

    fun reload() {
        saveDefaultConfig()
        reloadConfig()
        config.options().copyDefaults(true)
        saveConfig()

        bounciness = config.getDouble("bounciness")
        maxBounces = config.getInt("max-bounces")
        sandDebuff = config.getDouble("sand-debuff")
        nonFairwayDebuff = config.getDouble("non-fairway-debuff")
        bounceSoundEnabled = config.getBoolean("bounce-sound-enabled")
        bounceSound = config.getString("bounce-sound")?.let { Sound.valueOf(it) } ?: Sound.BLOCK_SLIME_BLOCK_FALL

        bounceSoundVolume = config.getDouble("bounce-sound-volume").toFloat()
        bounceSoundPitch = config.getDouble("bounce-sound-pitch").toFloat()

        disabledWorlds.clear()
        disabledWorlds.addAll(config.getStringList("disabled-worlds"))
    }

    @EventHandler
    fun onTeleport(event: PlayerTeleportEvent) {
        if (event.cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
            if (allowed.contains(event.player.uniqueId)) {
                allowed.remove(event.player.uniqueId)
            } else {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onPlayerRiptide(event: PlayerRiptideEvent) {
        val p = event.player

        logger.log(Level.INFO, "${p.name} has been launched. Reptiding: ${p.isRiptiding}")

        p.persistentDataContainer.set(
            ballOrigin, BallOriginDataType(), BallOrigin(p.location, LocalDateTime.now())
        )
    }

    @EventHandler
    fun onProjectileLaunch(event: ProjectileLaunchEvent) {
        logger.log(Level.INFO, "Projectile Launch BOOP")
        if (event.entityType == EntityType.ENDER_PEARL && event.entity.shooter is Player) {
            logger.log(Level.INFO, "Ender Pearl Launch BOOP")
            // Check if it's a player firing an enderpearl
            // log out hi

            val p = event.entity.shooter as Player

            // If they don't have permission of if in a disabled world, ensure the destination teleport behaves normally
            if (!p.hasPermission("golf.bounce") || disabledWorlds.contains(p.world.name)) {
                // Make this the last teleport
                allowed.add(p.uniqueId)
                // Make this the last bounce
                event.entity.persistentDataContainer.set(bounces, PersistentDataType.INTEGER, maxBounces + 1)
            }

            val location = if (p.persistentDataContainer.has(ballOrigin, BallOriginDataType())) {
                val data = p.persistentDataContainer.get(ballOrigin, BallOriginDataType())!!

                if (data.throwTime?.plusSeconds(5L)?.isAfter(LocalDateTime.now()) == true) {
                    // we know that a player throw it and we can compute a modifier
                    data.location
                } else p.location
            } else p.location

            event.entity.persistentDataContainer.set(ballOrigin, BallOriginDataType(), BallOrigin(location))

            val block = location.block

            val adj = block.getAdjacent()

            // go down the tree from worsed to best
            if (adj.find { it.type == Material.SAND } != null) {
                event.entity.velocity = event.entity.velocity.multiply(sandDebuff)
            } else if (adj.find { it.type != Material.GREEN_CONCRETE || it.type != Material.GREEN_CONCRETE_POWDER } != null) {
                event.entity.velocity = event.entity.velocity.multiply(0.5)
            }
        }
    }

    @EventHandler
    fun onProjectileHit(event: ProjectileHitEvent) {
        if (event.entityType == EntityType.ENDER_PEARL) {

            val old = event.entity as EnderPearl

            // The vector to "bounce" off of
            val n = event.hitEntity?.let {
                // Treat the plane orthogonal to the vector between the pearl and the entity as a solid plane to bounce off of
                old.location.toVector().subtract(event.hitEntity!!.boundingBox.center).normalize()
            } ?: event.hitBlockFace!!.direction

            val data = old.persistentDataContainer
            val location = old.persistentDataContainer.get(ballOrigin, BallOriginDataType())?.location
            var bounceCount = 1

            if (data.has(bounces, PersistentDataType.INTEGER)) {
                bounceCount = data.get(bounces, PersistentDataType.INTEGER)!!
                // Allow the player to teleport if it's the last bounce
                if (bounceCount == maxBounces) {
                    if (old.shooter is Player) {
                        allowed.add((old.shooter as Player).uniqueId)
                    }
                } else if (bounceCount > maxBounces) {
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
            ).multiply(bounciness)

            val pearlNew = old.world.spawn(old.location, EnderPearl::class.java)
            pearlNew.shooter = old.shooter
            old.remove()

            // Launch the event for other plugins to use
            val launchEvent = ProjectileLaunchEvent(pearlNew)
            // TODO double check if this will refire the launch event
            Bukkit.getPluginManager().callEvent(launchEvent)
            pearlNew.velocity = reflection
            val newData = pearlNew.persistentDataContainer
            newData.set(bounces, PersistentDataType.INTEGER, bounceCount + 1)
            location?.let { newData.set(ballOrigin, BallOriginDataType(), BallOrigin(location)) }
            if (bounceSoundEnabled) {
                pearlNew.world.playSound(pearlNew.location, bounceSound, bounceSoundVolume, bounceSoundPitch)
            }
        }
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}