package com.pmease.commons.loader;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.pmease.commons.util.ClassUtils;
import com.pmease.commons.util.DependencyAware;

public abstract class AbstractPluginModule extends AbstractModule implements DependencyAware<String> {

	private String pluginId;
	
	private String pluginVersion;
	
	private String pluginVendor;
	
	private String pluginName;
	
	private String pluginDescription;
	
	private Date pluginDate;
	
	private boolean product;
	
	private Set<String> pluginDependencies = new HashSet<String>();

	@Override
	protected void configure() {
		Class<? extends Plugin> pluginClass = getPluginClass();
		if (pluginClass != null) {
			contribute(Plugin.class, pluginClass);
		    
		    bindListener(new AbstractMatcher<TypeLiteral<?>>() {

				@Override
				public boolean matches(TypeLiteral<?> t) {
					return t.getRawType() == pluginClass;
				}
		    	
		    }, new TypeListener() {

				@Override
				public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
					encounter.register(new InjectionListener<I>() {

						@Override
						public void afterInjection(I injectee) {
							AbstractPlugin plugin = (AbstractPlugin) injectee;
							plugin.setId(pluginId);
							plugin.setName(pluginName);
							plugin.setVendor(pluginVendor);
							plugin.setVersion(pluginVersion);
							plugin.setDescription(pluginDescription);
							plugin.setProduct(product);
							plugin.setDate(pluginDate);
							plugin.setDependencyIds(pluginDependencies);
						}
						
					});
				}
		    	
		    });
		} else {
			contribute(Plugin.class, new Plugin() {

				@Override
				public String getId() {
					return pluginId;
				}

				@Override
				public String getName() {
					return pluginName;
				}

				@Override
				public String getVendor() {
					return pluginVendor;
				}

				@Override
				public String getVersion() {
					return pluginVersion;
				}

				@Override
				public String getDescription() {
					return pluginDescription;
				}

				@Override
				public Date getDate() {
					return pluginDate;
				}
				
				@Override
				public boolean isProduct() {
					return product;
				}

				@Override
				public Set<String> getDependencies() {
					return pluginDependencies;
				}

				@Override
				public void start() {
				}

				@Override
				public void postStart() {
				}

				@Override
				public void preStop() {
				}

				@Override
				public void stop() {
				}

			});
		}
	}

	protected Class<? extends AbstractPlugin> getPluginClass() {
		return null;
	}

	public void setPluginId(String pluginId) {
		this.pluginId = pluginId;
	}

	public void setPluginVersion(String pluginVersion) {
		this.pluginVersion = pluginVersion;
	}

	public void setPluginVendor(String pluginVendor) {
		this.pluginVendor = pluginVendor;
	}

	public void setPluginName(String pluginName) {
		this.pluginName = pluginName;
	}

	public void setPluginDescription(String pluginDescription) {
		this.pluginDescription = pluginDescription;
	}

	public void setPluginDependencies(Set<String> pluginDependencies) {
		this.pluginDependencies = pluginDependencies;
	}

	public void setPluginDate(Date pluginDate) {
		this.pluginDate = pluginDate;
	}
	
	public void setProduct(boolean product) {
		this.product = product;
	}
	
	@Override
	public String getId() {
		return pluginId;
	}

	@Override
	public Set<String> getDependencies() {
		return pluginDependencies;
	}
	
	protected <T> void contribute(Class<T> extensionPoint, Class<? extends T> extensionClass) {
		Multibinder<T> pluginBinder = Multibinder.newSetBinder(binder(), extensionPoint);
	    pluginBinder.addBinding().to(extensionClass);
	    bind(extensionClass).in(Singleton.class);
	}
	
	protected <T> void contribute(Class<T> extensionPoint, T extension) {
		Multibinder<T> pluginBinder = Multibinder.newSetBinder(binder(), extensionPoint);
	    pluginBinder.addBinding().toInstance(extension);
	}

	protected <T> void contributeFromPackage(Class<T> extensionPoint, Class<?> packageLocator) {
		for (Class<? extends T> subClass: ClassUtils.findImplementations(extensionPoint, packageLocator)) {
			contribute(extensionPoint, subClass);
		}
	}
		
}
