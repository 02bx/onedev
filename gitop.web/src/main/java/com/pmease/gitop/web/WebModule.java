package com.pmease.gitop.web;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.tika.mime.MimeTypes;
import org.glassfish.jersey.server.ResourceConfig;

import com.pmease.commons.jersey.JerseyConfigurator;
import com.pmease.commons.jetty.ServletConfigurator;
import com.pmease.commons.loader.AbstractPluginModule;
import com.pmease.commons.loader.ImplementationProvider;
import com.pmease.commons.wicket.AbstractWicketConfig;
import com.pmease.commons.wicket.editable.EditSupport;
import com.pmease.gitop.model.validation.UserNameReservation;
import com.pmease.gitop.web.editable.EditSupportLocator;
import com.pmease.gitop.web.page.TestPage.Cat;
import com.pmease.gitop.web.page.TestPage.Dog;
import com.pmease.gitop.web.page.TestPage.Pet;
import com.pmease.gitop.web.page.repository.source.blob.renderer.BlobRendererFactory;
import com.pmease.gitop.web.resource.ResourceLocator;

/**
 * NOTE: Do not forget to rename moduleClass property defined in the pom if you've renamed this class.
 *
 */
public class WebModule extends AbstractPluginModule {

	@Override
	protected void configure() {
		super.configure();
		
		// put your guice bindings here
		bind(AbstractWicketConfig.class).to(WicketConfig.class);		
		contribute(ServletConfigurator.class, WebServletConfigurator.class);
		contribute(UserNameReservation.class, WebUserNameReservation.class);
		
		contribute(JerseyConfigurator.class, new JerseyConfigurator() {
			
			@Override
			public void configure(ResourceConfig resourceConfig) {
				resourceConfig.packages(true, ResourceLocator.class.getPackage().getName());
			}
			
		});
		
		contribute(ImplementationProvider.class, new ImplementationProvider() {

			@Override
			public Class<?> getAbstractClass() {
				return Pet.class;
			}

			@Override
			public Collection<Class<?>> getImplementations() {
				Collection<Class<?>> implementations = new ArrayList<>();
				implementations.add(Cat.class);
				implementations.add(Dog.class);
				return implementations;
			}
			
		});
		
		contributeFromPackage(EditSupport.class, EditSupportLocator.class);

		bind(MimeTypes.class).toInstance(MimeTypes.getDefaultMimeTypes());
		
		bind(BlobRendererFactory.class);
	}

}
