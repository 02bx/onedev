package com.pmease.commons.loader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.pmease.commons.util.dependency.DependencyHelper;

@Singleton
public class DefaultPluginManager implements PluginManager {
	
	// use linked hash map here to keep plugins in order
	private final Map<String, AbstractPlugin> pluginMap = new LinkedHashMap<String, AbstractPlugin>();

	/**
	 * Construct plugin map in dependency order. Plugins without dependencies comes first in the 
	 * linked hash map.
	 *    
	 * @param plugins
	 */
	@Inject
	public DefaultPluginManager(final Set<AbstractPlugin> plugins) {
		for (AbstractPlugin plugin: plugins)
			pluginMap.put(plugin.getId(), plugin);
		
		for (AbstractPlugin plugin: DependencyHelper.sortDependencies(pluginMap)) {
			// make sure the plugin map is in sorted order.
			pluginMap.remove(plugin.getId());
			pluginMap.put(plugin.getId(), plugin);
		}
	}

	public void start() {
		for (AbstractPlugin plugin: pluginMap.values())
			plugin.start();
		List<AbstractPlugin> reversed = new ArrayList<AbstractPlugin>(pluginMap.values());
		Collections.reverse(reversed);
		for (AbstractPlugin plugin: reversed)
			plugin.postStart();
	}

	public void stop() {
		for (AbstractPlugin plugin: pluginMap.values())
			plugin.preStop();
		List<AbstractPlugin> reversed = new ArrayList<AbstractPlugin>(pluginMap.values());
		Collections.reverse(reversed);
		for (AbstractPlugin plugin: reversed)
			plugin.stop();
	}

	@Override
	public Collection<AbstractPlugin> getPlugins() {
		return Collections.unmodifiableCollection(pluginMap.values());
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends AbstractPlugin> T getPlugin(Class<T> pluginClass) {
		for (AbstractPlugin plugin: pluginMap.values()) {
			if (plugin.getClass() == pluginClass)
				return (T) plugin;
		}
		throw new RuntimeException("Unable to find plugin with class '" + pluginClass + "'.");
	}
	
	public AbstractPlugin getPlugin(String pluginId) {
		if (pluginMap.containsKey(pluginId))
			return pluginMap.get(pluginId);
		else
			throw new RuntimeException("Unable to find plugin with id '" + pluginId + "'.");
	}

}
