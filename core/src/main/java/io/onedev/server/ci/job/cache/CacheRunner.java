package io.onedev.server.ci.job.cache;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

import org.slf4j.Logger;

import io.onedev.commons.utils.ExceptionUtils;
import io.onedev.commons.utils.FileUtils;

public class CacheRunner {

	private final File cacheHome;
	
	private final Collection<JobCache> caches;
	
	public CacheRunner(File cacheHome, Collection<JobCache> caches) {
		this.cacheHome = cacheHome;
		this.caches = caches;
	}
	
	public void run(CacheRunnable runnable, Logger logger) {
		Collection<CacheAllocation> allocations = new ArrayList<>();
		try {
			if (!cacheHome.exists())
				FileUtils.createDir(cacheHome);
			
			for (JobCache cache: caches)
				allocations.add(cache.allocate(cacheHome));
			runnable.run(allocations);
		} catch (Exception e) {
			throw ExceptionUtils.unchecked(e);
		} finally {
			for (CacheAllocation allocation: allocations) {
				try {
					allocation.release();
				} catch (Exception e) {
					logger.error("Error releasing allocated cache", e);
				}
			}
		}
	}
	
}
