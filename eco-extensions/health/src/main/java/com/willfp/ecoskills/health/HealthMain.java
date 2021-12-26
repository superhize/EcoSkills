package com.willfp.ecoskills.health;

import com.willfp.eco.core.EcoPlugin;
import com.willfp.eco.core.config.interfaces.Config;
import com.willfp.eco.core.extensions.Extension;
import org.jetbrains.annotations.NotNull;

public class HealthMain extends Extension {
    /**
     * The instance.
     */
    private static HealthMain instance;

    /**
     * attackspeed.yml.
     */
    private Config config = new HealthConfig(this);

    /**
     * Create a new extension for a plugin.
     *
     * @param plugin The plugin.
     */
    public HealthMain(@NotNull final EcoPlugin plugin) {
        super(plugin);
        instance = this;
    }

    @Override
    protected void onEnable() {
        new StatHealth();
    }

    @Override
    protected void onDisable() {

    }

    /**
     * Get attackspeed.yml.
     *
     * @return The config.
     */
    public Config getConfig() {
        return config;
    }

    /**
     * Get instance.
     *
     * @return The instance.
     */
    public static HealthMain getInstance() {
        return instance;
    }
}
