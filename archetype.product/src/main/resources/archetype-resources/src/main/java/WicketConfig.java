package ${package};

import org.apache.wicket.Page;

import com.pmease.commons.wicket.AbstractWicketConfig;
import ${package}.web.HomePage;

public class WicketConfig extends AbstractWicketConfig {

	@Override
	public Class<? extends Page> getHomePage() {
		return HomePage.class;
	}

}
