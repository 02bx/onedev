package com.pmease.commons.editable;

import com.pmease.commons.editable.typeconverter.TypeConverter;
import com.pmease.commons.loader.AbstractPluginModule;

/**
 * NOTE: Do not forget to rename moduleClass property defined in the pom if you've renamed this class.
 *
 */
public class EditableModule extends AbstractPluginModule {

	@Override
	protected void configure() {
		super.configure();
		
		// put your guice bindings here
		addExtensionsFromPackage(TypeConverter.class, TypeConverter.class);
	}

}
