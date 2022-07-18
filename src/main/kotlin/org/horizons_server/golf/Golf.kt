package org.horizons_server.golf

import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.horizons_server.golf.command.GolfCommand
import org.horizons_server.golf.listener.BounceListener
import java.util.*

class Golf : JavaPlugin(), Listener {
    lateinit var bounces: NamespacedKey
    lateinit var ballOrigin: NamespacedKey

    val allowed: MutableSet<UUID> = HashSet()
    val enabled = HashSet<UUID>()

    var bounciness = 0.8
    var maxBounces = 5
    var bounceSoundEnabled = false
    var bounceSound: Sound = Sound.BLOCK_SLIME_BLOCK_FALL
    var sandDebuff = 0.2
    var nonFairwayDebuff = 0.4
    var bounceSoundVolume = 0f
    var bounceSoundPitch = 0f
    val disabledWorlds: HashSet<String> = HashSet()

    companion object {
        fun getPlugin() = getPlugin(Golf::class.java)
    }

    override fun onEnable() {
        bounces = NamespacedKey(this, "bounces")
        ballOrigin = NamespacedKey(this, "startingBlock")
        server.pluginManager.registerEvents(this, this)

        val golfCommand = GolfCommand(this)
        getCommand("golf")?.setExecutor(golfCommand)
        getCommand("golf")?.tabCompleter = golfCommand

        server.pluginManager.registerEvents(BounceListener(this), this)

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


    override fun onDisable() {
        // Plugin shutdown logic
    }
}