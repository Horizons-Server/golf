package org.horizons_server.golf

import org.bukkit.block.Block
import org.bukkit.block.BlockFace

fun Block.getAdjacent() = listOf(
    this.getRelative(BlockFace.NORTH),
    this.getRelative(BlockFace.SOUTH),
    this.getRelative(BlockFace.EAST),
    this.getRelative(BlockFace.WEST),
    this.getRelative(BlockFace.DOWN)
)