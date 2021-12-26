package com.willfp.ecoskills.health;

import com.willfp.eco.core.config.interfaces.Config;
import com.willfp.ecoskills.api.EcoSkillsAPI;
import com.willfp.ecoskills.stats.Stat;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StatHealth extends Stat {
    /**
     * Create attack speed stat.
     */
    public StatHealth() {
        super("health");
    }

    @NotNull
    @Override
    public Config loadConfig() {
        return HealthMain.getInstance().getConfig();
    }

    @Override
    public void updateStatLevel(@NotNull final Player player) {
        AttributeModifier modifier = new AttributeModifier(
                this.getUuid(),
                this.getName(),
                (
                        this.config.getDouble("health-per-level")
                                * EcoSkillsAPI.getInstance().getStatLevel(player, this)
                ),
                AttributeModifier.Operation.ADD_NUMBER
        );

        AttributeInstance instance = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (instance == null) {
            return;
        }

        instance.removeModifier(modifier);

        this.getPlugin().getScheduler().run(() -> {
            instance.removeModifier(modifier);
            instance.addModifier(modifier);
        });
    }
}
