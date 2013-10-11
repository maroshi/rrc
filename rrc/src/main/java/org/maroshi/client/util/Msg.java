package org.maroshi.client.util;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Msg {
	private static final String BUNDLE_NAME = "org.maroshi.client.util.msg"; //$NON-NLS-1$

	private static Locale nonexistentLocale = new Locale("en", "MR");
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private Msg() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}