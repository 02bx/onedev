package io.onedev.server.ci.job.log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;

import io.onedev.commons.launcher.loader.Listen;
import io.onedev.commons.utils.ExceptionUtils;
import io.onedev.commons.utils.LockUtils;
import io.onedev.server.event.build.BuildFinished;
import io.onedev.server.model.Build;
import io.onedev.server.persistence.annotation.Sessional;
import io.onedev.server.storage.StorageManager;
import io.onedev.server.web.websocket.WebSocketManager;

@Singleton
public class DefaultLogManager implements LogManager {

	private static final Logger logger = LoggerFactory.getLogger(DefaultLogManager.class);
	
	private static final int MIN_CACHE_ENTRIES = 5000;

	private static final int MAX_CACHE_ENTRIES = 10000;
	
	private static final String LOG_FILE = "build.log";
	
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat.forPattern("HH:mm:ss");	
	
	private static final Pattern EOL_PATTERN = Pattern.compile("\r?\n");

	private final StorageManager storageManager;
	
	private final WebSocketManager webSocketManager;
	
	private final Map<Long, LogSnippet> recentSnippets = new ConcurrentHashMap<>();
	
	@Inject
	public DefaultLogManager(StorageManager storageManager, WebSocketManager webSocketManager) {
		this.storageManager = storageManager;
		this.webSocketManager = webSocketManager;
	}
	
	private File getLogFile(Long projectId, Long buildId) {
		File buildDir = storageManager.getBuildDir(projectId, buildId);
		return new File(buildDir, LOG_FILE);
	}
	
	@Override
	public Logger getLogger(Long projectId, Long buildId, LogLevel loggerLevel) {
		return new JobLogger(loggerLevel) {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void log(LogLevel logLevel, String message, Throwable throwable) {
				try {
					if (throwable != null) {
						for (String line: Splitter.on(EOL_PATTERN).split(Throwables.getStackTraceAsString(throwable)))
							message += "\n    " + line;
					}
							
					if (logLevel.ordinal() <= loggerLevel.ordinal()) {
						Lock lock = LockUtils.getReadWriteLock(getLockKey(buildId)).writeLock();
						lock.lock();
						try {
							LogSnippet snippet = recentSnippets.get(buildId);
							if (snippet == null) {
								File logFile = getLogFile(projectId, buildId);
								if (!logFile.exists())	{
									snippet = new LogSnippet();
									recentSnippets.put(buildId, snippet);
								}
							}
							if (snippet != null) {
								snippet.entries.add(new LogEntry(new Date(), logLevel, message));
								if (snippet.entries.size() > MAX_CACHE_ENTRIES) {
									File logFile = getLogFile(projectId, buildId);
									try (ObjectOutputStream oos = newOutputStream(logFile)) {
										while (snippet.entries.size() > MIN_CACHE_ENTRIES) {
											LogEntry entry = snippet.entries.remove(0);
											oos.writeObject(entry);
											snippet.offset++;
										}
									} catch (IOException e) {
										throw new RuntimeException(e);
									}
								}
								
								webSocketManager.notifyObservableChange(Build.getLogWebSocketObservable(buildId), null);
							}
						} finally {
							lock.unlock();
						}
					}
				} catch (Exception e) {
					logger.error("Error logging", e);
				}
			}
			
		};
	}

	private String getLockKey(Long buildId) {
		return "build-log: " + buildId;
	}

	private List<LogEntry> readLogEntries(File logFile, int from, int count) {
		List<LogEntry> entries = new ArrayList<>();
		if (logFile.exists()) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(logFile)))) {
				int numOfReadEntries = 0;
				while (numOfReadEntries < from) {
					ois.readObject();
					numOfReadEntries++;
				}
				while (count == 0 || numOfReadEntries - from < count) {
					entries.add((LogEntry) ois.readObject());
					numOfReadEntries++;
				}
			} catch (EOFException e) {
			} catch (IOException | ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return entries;
	}
	
	private LogSnippet readLogSnippetReversely(File logFile, int count) {
		LogSnippet snippet = new LogSnippet();
		if (logFile.exists()) {
			try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(logFile)))) {
				while (true) {
					snippet.entries.add((LogEntry) ois.readObject());
					if (snippet.entries.size() > count) {
						snippet.entries.remove(0);
						snippet.offset ++;
					}
				}
			} catch (EOFException e) {
			} catch (IOException | ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
		return snippet;
	}
	
	private List<LogEntry> readLogEntries(List<LogEntry> cachedEntries, int from, int count) {
		if (from < cachedEntries.size()) {
			int to = from + count;
			if (to == from || to > cachedEntries.size())
				to = cachedEntries.size();
			return new ArrayList<>(cachedEntries.subList(from, to));
		} else {
			return new ArrayList<>();
		}
	}
	
	@Sessional
	@Override
	public List<LogEntry> readLogEntries(Build build, int from, int count) {
		Lock lock = LockUtils.getReadWriteLock(getLockKey(build.getId())).readLock();
		lock.lock();
		try {
			File logFile = getLogFile(build.getProject().getId(), build.getId());
			LogSnippet snippet = recentSnippets.get(build.getId());
			if (snippet != null) {
				if (from >= snippet.offset) {
					return readLogEntries(snippet.entries, from - snippet.offset, count);
				} else {
					List<LogEntry> entries = new ArrayList<>();
					entries.addAll(readLogEntries(logFile, from, count));
					if (count == 0)
						entries.addAll(snippet.entries);
					else if (entries.size() < count) 
						entries.addAll(readLogEntries(snippet.entries, 0, count - entries.size()));
					return entries;
				}
			} else {
				return readLogEntries(logFile, from, count);
			}
		} finally {
			lock.unlock();
		}
	}

	@Sessional
	@Override
	public LogSnippet readLogSnippetReversely(Build build, int count) {
		Lock lock = LockUtils.getReadWriteLock(getLockKey(build.getId())).readLock();
		lock.lock();
		try {
			File logFile = getLogFile(build.getProject().getId(), build.getId());
			LogSnippet recentSnippet = recentSnippets.get(build.getId());
			if (recentSnippet != null) {
				LogSnippet snippet = new LogSnippet();
				if (count <= recentSnippet.entries.size()) {
					snippet.entries.addAll(recentSnippet.entries.subList(
							recentSnippet.entries.size()-count, recentSnippet.entries.size()));
				} else {
					snippet.entries.addAll(readLogSnippetReversely(logFile, count - recentSnippet.entries.size()).entries);
					snippet.entries.addAll(recentSnippet.entries);
				}
				snippet.offset = recentSnippet.entries.size() + recentSnippet.offset - snippet.entries.size();
				return snippet;
			} else {
				return readLogSnippetReversely(logFile, count);
			}
		} finally {
			lock.unlock();
		}
	}
	
	private ObjectOutputStream newOutputStream(File logFile) {
		try {
			if (logFile.exists()) {
				return new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(logFile, true))) {

					@Override
					protected void writeStreamHeader() throws IOException {
						reset();
					}
					
				};
			} else {
				return new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(logFile)));
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Sessional
	@Listen
	public void on(BuildFinished event) {
		Build build = event.getBuild();
		Lock lock = LockUtils.getReadWriteLock(getLockKey(build.getId())).writeLock();
		lock.lock();
		try {
			LogSnippet snippet = recentSnippets.remove(build.getId());
			if (snippet != null) {
				File logFile = getLogFile(build.getProject().getId(), build.getId());
				try (ObjectOutputStream oos = newOutputStream(logFile)) {
					for (LogEntry entry: snippet.entries)
						oos.writeObject(entry);
				} catch (IOException e) {
					throw new RuntimeException(e);
				} 
			}
		} finally {
			lock.unlock();
		}
	}

	@Override
	public InputStream openLogStream(Build build) {
		return new LogStream(build);
	}

	class LogStream extends InputStream {

		private ObjectInputStream ois;
		
		private final Lock lock;

		private byte[] buffer = new byte[0];
		
		private byte[] recentBuffer;
		
		private int pos = 0;
		
		public LogStream(Build build) {
			lock = LockUtils.getReadWriteLock(getLockKey(build.getId())).readLock();
			lock.lock();
			try {
				File logFile = getLogFile(build.getProject().getId(), build.getId());
				
				if (logFile.exists())
					ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(logFile)));
				
				LogSnippet snippet = recentSnippets.get(build.getId());
				if (snippet != null) {
					StringBuilder builder = new StringBuilder();
					for (LogEntry entry: snippet.entries)
						builder.append(renderAsText(entry) + "\n");
					recentBuffer = builder.toString().getBytes(Charsets.UTF_8);
				}
			} catch (Exception e) {
				lock.unlock();
				throw ExceptionUtils.unchecked(e);
			}
		}
		
		private String renderAsText(LogEntry entry) {
			String prefix = DATE_FORMATTER.print(new DateTime(entry.getDate())) + " " 
					+ StringUtils.leftPad(entry.getLevel().name(), 5) + " ";
			StringBuilder builder = new StringBuilder();
			for (String line: Splitter.on(EOL_PATTERN).split(entry.getMessage())) {
				if (builder.length() == 0) {
					builder.append(prefix).append(line);
				} else {
					builder.append("\n");
					for (int i=0; i<prefix.length(); i++)
						builder.append(" ");
					builder.append(line);
				}
			}
			return builder.toString();
		}
		
		@Override
		public int read() throws IOException {
			if (pos == buffer.length) {
				if (ois != null) {
					try {
						buffer = (renderAsText((LogEntry) ois.readObject()) + "\n").getBytes(Charsets.UTF_8);
					} catch (EOFException e) {
						IOUtils.closeQuietly(ois);
						ois = null;
						if (recentBuffer != null) {
							buffer = recentBuffer;
							recentBuffer = null;
						} else {
							return -1;
						}
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				} else if (recentBuffer != null) {
					buffer = recentBuffer;
					recentBuffer = null;
				} else {
					return -1;
				}
				pos = 1;
				return buffer[0];
			} else {
				return buffer[pos++];
			}
		}
		
		@Override
		public void close() throws IOException {
			IOUtils.closeQuietly(ois);
			lock.unlock();
		}
				
	}
}
