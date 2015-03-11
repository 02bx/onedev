package com.pmease.commons.lang.tokenizers.css;

import org.junit.Test;

import com.pmease.commons.lang.tokenizers.AbstractTokenizerTest;
import com.pmease.commons.lang.tokenizers.css.SCSSTokenizer;

public class SCSSTokenizerTest extends AbstractTokenizerTest {

	@Test
	public void test() {
		verify(new SCSSTokenizer(), new String[]{"css/css.js"}, "test.scss");
	}

}
