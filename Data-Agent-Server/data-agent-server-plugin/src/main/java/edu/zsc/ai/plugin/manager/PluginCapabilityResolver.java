package edu.zsc.ai.plugin.manager;

import edu.zsc.ai.plugin.Plugin;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves capability providers from plugins.
 *
 * @author Data-Agent
 * @since 0.0.1
 */
public final class PluginCapabilityResolver {

    private PluginCapabilityResolver() {
        // Utility class, prevent instantiation
    }

    /**
     * Get all plugins that implement the specified capability, filtered from the given plugin list.
     *
     * @param plugins         sorted list of plugins
     * @param capabilityClass the capability interface/class to filter by
     * @param dbTypeCode      database type code for error messages
     * @param <T>             capability type
     * @return list of capability providers (unmodifiable)
     * @throws IllegalArgumentException if no plugin implements the capability
     */
    public static <T> List<T> getProviders(List<Plugin> plugins, Class<T> capabilityClass, String dbTypeCode) {
        List<T> providers = plugins.stream()
                .filter(capabilityClass::isInstance)
                .map(capabilityClass::cast)
                .toList();
        if (providers.isEmpty()) {
            throw new IllegalArgumentException("No " + capabilityClass.getSimpleName() + " available for database type: " + dbTypeCode);
        }
        return providers;
    }

    /**
     * Get a capability provider by plugin ID.
     *
     * @param pluginMap       map of plugin ID to plugin
     * @param pluginId        plugin ID
     * @param capabilityClass the capability interface/class
     * @param <T>             capability type
     * @return the capability provider
     * @throws NullPointerException     if plugin not found
     * @throws IllegalArgumentException if plugin does not implement the capability
     */
    public static <T> T getProviderByPluginId(Map<String, Plugin> pluginMap, String pluginId, Class<T> capabilityClass) {
        Plugin plugin = Objects.requireNonNull(pluginMap.get(pluginId), "No plugin found with ID: " + pluginId);
        if (!capabilityClass.isInstance(plugin)) {
            throw new IllegalArgumentException("Plugin " + pluginId + " does not implement " + capabilityClass.getSimpleName());
        }
        return capabilityClass.cast(plugin);
    }

    /**
     * Get a capability provider by database type and version.
     *
     * @param plugins         sorted list of plugins
     * @param databaseVersion target database version (may be null for latest)
     * @param capabilityClass the capability interface/class
     * @param <T>             capability type
     * @return the capability provider
     * @throws IllegalArgumentException if selected plugin does not implement the capability
     */
    public static <T> T getProviderByDbTypeAndVersion(List<Plugin> plugins, String databaseVersion, Class<T> capabilityClass) {
        Plugin plugin = PluginVersionSelector.select(plugins, databaseVersion);
        if (!capabilityClass.isInstance(plugin)) {
            throw new IllegalArgumentException("Plugin " + plugin.getPluginId() + " does not implement " + capabilityClass.getSimpleName());
        }
        return capabilityClass.cast(plugin);
    }
}
