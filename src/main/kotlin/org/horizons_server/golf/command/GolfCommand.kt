package org.horizons_server.golf.command

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.horizons_server.golf.Golf
import org.horizons_server.golf.getAdjacent

class GolfCommand(private val base: Golf) : CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (args.isEmpty()) {
            sender.sendMessage(
                """
                | Usage /golf <command>
                | Commands:
                |   reload - reloads the plugin
                |   on - enables the plugin for you
                |   off - disables the plugin for you
                |   water - places a water underneath you temporarily
                """.trimIndent()
            )
        } else {
            val (perm, func) = when (args[0]) {
                "reload" -> "golf.reload" to { base.reload() }
                "on" -> "golf.play" to {
                    if (sender !is ConsoleCommandSender) {
                        sender.sendMessage(
                            "${ChatColor.GREEN} You are good to go. Enjoy!" +
                                    "\n If the server restarts, you will need to reenable."
                        )
                        base.enabled.add((sender as Player).uniqueId)
                    }
                }
                "off" -> "golf.play" to {
                    if (sender !is ConsoleCommandSender) {
                        sender.sendMessage("${ChatColor.RED} Golf Disabled. Come back soon!")
                        base.enabled.remove((sender as Player).uniqueId)
                    }
                }
                "water" -> "golf.water" to { water(sender) }
                else -> null
            } ?: return false

            if (sender.hasPermission(perm)) {
                func()
            } else {
                sender.sendMessage("${ChatColor.RED} You don't have permission to do that!")
            }
        }

        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> listOf("reload", "on", "off", "water")
            else -> emptyList()
        }
    }


    private val allowedBlocks = listOf(Material.GRASS_BLOCK, Material.GREEN_CONCRETE_POWDER, Material.LIME_TERRACOTTA)

    private fun water(sender: CommandSender) {
        if (sender is ConsoleCommandSender) {
            sender.sendMessage("${ChatColor.RED} Only players can use this command!")
            return
        }

        val player = sender as Player
        val playerBlock = player.location.block
        val blockBelow = playerBlock.getRelative(BlockFace.DOWN)
        val oldBlockType: Material = blockBelow.type

        if (!allowedBlocks.contains(oldBlockType)) {
            player.sendMessage("${ChatColor.RED} You can't put water here!")
            return
        }

        for (block in playerBlock.getAdjacent()) {
            if (block.type != Material.AIR) continue
            block.type = Material.BLACK_CONCRETE
        }
//        val grassAbove = playerBlock.getRelative(BlockFace.UP).type == Material.GRASS

        blockBelow.type = Material.WATER
        player.sendMessage("${ChatColor.GREEN} Done! You have 15 seconds to make your shot.")

        Bukkit.getScheduler().runTaskLater(Golf.getPlugin(), Runnable {
            blockBelow.type = oldBlockType
            for (block in playerBlock.getAdjacent()) {
                if (block.type == Material.GREEN_CONCRETE) block.type = Material.GREEN_CONCRETE_POWDER
                if (block.type == Material.BLACK_CONCRETE) block.type = Material.AIR
            }
//            if (grassAbove) playerBlock.getRelative(BlockFace.UP).type = Material.GRASS
        }, 300L)

        return
    }
}
