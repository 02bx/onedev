package com.turbodev.server.manager.impl;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import com.turbodev.launcher.loader.Listen;
import com.turbodev.utils.FileUtils;
import com.google.common.base.Charsets;
import com.turbodev.server.event.lifecycle.SystemStopping;

import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentConfig;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalComputable;

public abstract class AbstractEnvironmentManager {
	
	private static final String VERSION_FILE = "version.txt";
	
	private static final long DEFAULT_LOG_FILE_SIZE = 8192;
	
	private static final int MEMORY_USAGE_PERCENT = 25;
	
	private final Map<String, Environment> envs = new ConcurrentHashMap<>();
	
	protected void checkVersion(String envKey) {
		File versionFile = new File(getEnvDir(envKey), VERSION_FILE);
		int versionFromFile;
		if (versionFile.exists()) {
			try {
				versionFromFile = Integer.parseInt(FileUtils.readFileToString(versionFile).trim());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			versionFromFile = 0;
		}
		if (versionFromFile != getEnvVersion()) {
			FileUtils.cleanDir(versionFile.getParentFile());
			FileUtils.writeFile(versionFile, String.valueOf(getEnvVersion()));
		} 
	}
	
	protected void writeVersion(String envKey) {
		File versionFile = new File(getEnvDir(envKey), VERSION_FILE);
		FileUtils.writeFile(versionFile, String.valueOf(getEnvVersion()));
	}

	protected abstract File getEnvDir(String envKey);
	
	protected abstract int getEnvVersion();
	
	protected long getLogFileSize() {
		return DEFAULT_LOG_FILE_SIZE;
	}
	
	protected Environment getEnv(String envKey) {
		Environment env = envs.get(envKey);
		if (env == null) synchronized (envs) {
			env = envs.get(envKey);
			if (env == null) {
				checkVersion(envKey);
				EnvironmentConfig config = new EnvironmentConfig();
				config.setEnvCloseForcedly(true);
				config.setMemoryUsagePercentage(MEMORY_USAGE_PERCENT);
				config.setLogFileSize(getLogFileSize());
				env = Environments.newInstance(getEnvDir(envKey), config);
				envs.put(envKey, env);
			}
		}
		return env;
	}
	
	protected Store getStore(Environment env, String storeName) {
		return env.computeInTransaction(new TransactionalComputable<Store>() {
		    @Override
		    public Store compute(Transaction txn) {
		        return env.openStore(storeName, StoreConfig.WITHOUT_DUPLICATES, txn);
		    }
		});		
	}

	protected void removeEnv(String envKey) {
		synchronized (envs) {
			Environment env = envs.remove(envKey);
			if (env != null)
				env.close();
		}
	}

	@Listen
	public void on(SystemStopping event) {
		synchronized (envs) {
			for (Environment env: envs.values())
				env.close();
			envs.clear();
		}
	}

	protected byte[] getBytes(@Nullable ByteIterable byteIterable) {
		if (byteIterable != null)
			return Arrays.copyOf(byteIterable.getBytesUnsafe(), byteIterable.getLength());
		else
			return null;
	}
	
	protected byte[] longToBytes(long value) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.putLong(value);
	    return buffer.array();
	}

	protected long bytesToLong(byte[] bytes) {
	    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	    buffer.put(bytes);
	    buffer.flip(); 
	    return buffer.getLong();
	}
	
	static class StringByteIterable extends ArrayByteIterable {
		StringByteIterable(String value) {
			super(value.getBytes(Charsets.UTF_8));
		}
	}

	static class StringPairByteIterable extends ArrayByteIterable {
		
		StringPairByteIterable(String value1, String value2) {
			super(getBytes(value1, value2));
		}
		
		private static byte[] getBytes(String value1, String value2) {
			byte[] bytes1 = value1.getBytes();
			byte[] bytes2 = value2.getBytes();
			byte[] bytes = new byte[bytes1.length+bytes2.length];
			System.arraycopy(bytes1, 0, bytes, 0, bytes1.length);
			System.arraycopy(bytes2, 0, bytes, bytes1.length, bytes2.length);
			return bytes;
		}
	}

}
