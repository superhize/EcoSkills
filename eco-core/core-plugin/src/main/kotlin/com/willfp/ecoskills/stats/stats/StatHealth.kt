package com.willfp.ecoskills.stats.stats

import com.willfp.ecoskills.getStatLevel
import com.willfp.ecoskills.stats.Stat
import org.bukkit.attribute.Attribute
import org.bukkit.attribute.AttributeModifier
import org.bukkit.entity.Player

class StatHealth : Stat(
    "health"
) {
    override fun updateStatLevel(player: Player) {
        val modifier = AttributeModifier(
            this.uuid,
            this.name,
            (this.config.getDouble("health-per-level") * player.getStatLevel(this)),
            AttributeModifier.Operation.MULTIPLY_SCALAR_1
        )
        val instance = player.getAttribute(Attribute.GENERIC_MAX_HEALTH) ?: return
        instance.removeModifier(modifier)

        plugin.scheduler.run {
            instance.removeModifier(modifier)
            instance.addModifier(modifier)
        }
    }
}