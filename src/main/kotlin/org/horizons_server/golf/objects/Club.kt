package org.horizons_server.golf.objects

import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemStack

enum class Club {
    IRON, WOOD, DRIVER;

    private fun getByEnchantmentLevel(enchantmentLevel: Int) = when (enchantmentLevel) {
        1 -> IRON
        2 -> WOOD
        else -> DRIVER
    }

    fun getByItemStack(itemStack: ItemStack): Club {
        require(!(itemStack.type !== Material.TRIDENT)) { "Provided ItemStack is not a trident" }

        val enchantmentLevel = itemStack.enchantments[Enchantment.RIPTIDE]
        println(enchantmentLevel)

        require(enchantmentLevel != 0 && enchantmentLevel != null) { "Provided ItemStack does not have the RIPTIDE enchantment" }
        return getByEnchantmentLevel(enchantmentLevel)
    }
}
