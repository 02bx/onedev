package com.pmease.commons.bootstrap;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class Bootstrap {

	private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);
	
	public static final String APP_LOADER_PROPERTY_NAME = "appLoader";
	
	public static final String LOGBACK_CONFIG_FILE_PROPERTY_NAME = "logback.configurationFile";
	
	public static final String DEFAULT_APP_LOADER = "com.pmease.commons.loader.AppLoader";
	
	public static File installDir;

	public static List<File> libFiles;
	
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		File loadedFrom = new File(Bootstrap.class.getProtectionDomain()
				.getCodeSource().getLocation().getFile());

		if (new File(loadedFrom.getParentFile(), "bootstrap.keys").exists())
			installDir = loadedFrom.getParentFile().getParentFile();
		else if (new File("target/sandbox").exists())
			installDir = new File("target/sandbox");
		else 
			throw new RuntimeException("Unable to find product directory.");
		
		try {
			installDir = installDir.getCanonicalFile();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		
		configureLogback();
		
		System.setProperty("logback.configurationFile", new File(installDir, "conf/logback.xml").getAbsolutePath());
		logger.info("Launching application from '" + installDir.getAbsolutePath() + "'...");

		libFiles = new ArrayList<File>();
		
		File classpathFile = new File(installDir, "bin/system.classpath");
		if (classpathFile.exists()) {
			Map<String, File> systemClasspath = (Map<String, File>) BootstrapUtils.readObject(classpathFile);

			Set<String> bootstrapKeys = (Set<String>) BootstrapUtils
					.readObject(new File(installDir, "bin/bootstrap.keys"));
			for (Map.Entry<String, File> entry: systemClasspath.entrySet()) {
				if (!bootstrapKeys.contains(entry.getKey()))
					libFiles.add(entry.getValue());
			}
		} else {
			if (getLibCacheDir().exists()) {
				BootstrapUtils.cleanDir(getLibCacheDir());
			} else if (!getLibCacheDir().mkdir()) {
				throw new RuntimeException("Failed to create lib cache directory '"
						+ getLibCacheDir().getAbsolutePath() + "'");
			}

			for (File file: getLibDir().listFiles()) {
				if (file.getName().endsWith(".jar"))
					libFiles.add(file);
				else if (file.getName().endsWith(".zip"))
					BootstrapUtils.unzip(file, getLibCacheDir(), null);
			}
			
			for (File file: getLibCacheDir().listFiles()) {
				if (file.getName().endsWith(".jar"))
					libFiles.add(file);
			}
			
		}

		// load our jars first so that we can override classes in third party jars if necessary. 
		Collections.sort(libFiles, new Comparator<File>() {

			@Override
			public int compare(File file1, File file2) {
				if (file1.isDirectory())
					return -1;
				else if (file1.getName().startsWith("com.pmease"))
					return -1;
				else
					return 1;
			}
			
		});
		
		List<URL> urls = new ArrayList<URL>();
		
		for (File file: libFiles) {
			try {
				urls.add(file.toURI().toURL());
			} catch (MalformedURLException e) {
				throw new RuntimeException(e);
			}
		}
		
		ClassLoader appClassLoader = new URLClassLoader(urls.toArray(new URL[0]), Bootstrap.class.getClassLoader()) {

			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {
				return super.findClass(name);
			}

			@Override
			public Class<?> loadClass(String name) throws ClassNotFoundException {
				return super.loadClass(name);
			}

			@Override
			public URL findResource(String name) {
				return super.findResource(name);
			}
			
		};
		ClassLoader originClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(appClassLoader);
		
		try {
			String appLoaderClassName = System.getProperty(APP_LOADER_PROPERTY_NAME);
			if (appLoaderClassName == null)
				appLoaderClassName = DEFAULT_APP_LOADER;
			
			final Lifecycle appLoader;
			try {
				Class<?> appLoaderClass = appClassLoader.loadClass(appLoaderClassName);
				appLoader = (Lifecycle) appLoaderClass.newInstance();
				appLoader.start();
			} catch (Exception e) {
				throw BootstrapUtils.unchecked(e);
			}
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					try {
						appLoader.stop();
					} catch (Exception e) {
						throw BootstrapUtils.unchecked(e);
					}
				}
			});
		} finally {
			Thread.currentThread().setContextClassLoader(originClassLoader);
		}
	}
	
	private static void configureLogback() {
		System.setProperty("installDir", installDir.getAbsolutePath());
		
		LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
	    
	    try {
	      JoranConfigurator configurator = new JoranConfigurator();
	      configurator.setContext(lc);
	      lc.reset(); 
	      if (System.getProperty(LOGBACK_CONFIG_FILE_PROPERTY_NAME) != null)
	    	  configurator.doConfigure(new File(System.getProperty(LOGBACK_CONFIG_FILE_PROPERTY_NAME)));
	      else
	    	  configurator.doConfigure(new File(installDir, "conf/logback.xml"));
	    } catch (JoranException je) {
	       je.printStackTrace();
	    }
	    StatusPrinter.printInCaseOfErrorsOrWarnings(lc);	
	}

	public static File getLibDir() {
		return new File(installDir, "lib");
	}
	
	public static File getTempDir() {
		return new File(installDir, "temp");
	}

	public static File getLibCacheDir() {
		return new File(getLibDir(), ".cache");
	}

	public static File getConfDir() {
		return new File(installDir, "conf");
	}

	public static boolean isSandboxMode() {
		return new File("target/sandbox").exists();
	}

	public static boolean isProdMode() {
		return System.getProperty("prod") != null;
	}
}
