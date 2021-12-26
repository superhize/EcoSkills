package com.willfp.ecoskills.health;

import com.willfp.eco.core.PluginLike;
import com.willfp.eco.core.config.yaml.YamlBaseConfig;
import org.jetbrains.annotations.NotNull;

public class HealthConfig extends YamlBaseConfig {
    public HealthConfig(@NotNull final PluginLike plugin) {
        super("health", true, plugin);
    }
}
