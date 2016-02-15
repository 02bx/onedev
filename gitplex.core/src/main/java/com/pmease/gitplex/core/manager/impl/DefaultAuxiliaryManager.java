package com.pmease.gitplex.core.manager.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pmease.commons.git.Commit;
import com.pmease.commons.git.Git;
import com.pmease.commons.git.NameAndEmail;
import com.pmease.commons.git.command.CommitConsumer;
import com.pmease.commons.git.command.LogCommand;
import com.pmease.commons.hibernate.UnitOfWork;
import com.pmease.commons.hibernate.dao.Dao;
import com.pmease.commons.util.FileUtils;
import com.pmease.commons.util.concurrent.PrioritizedRunnable;
import com.pmease.gitplex.core.listeners.LifecycleListener;
import com.pmease.gitplex.core.listeners.RefListener;
import com.pmease.gitplex.core.listeners.DepotListener;
import com.pmease.gitplex.core.manager.AuxiliaryManager;
import com.pmease.gitplex.core.manager.SequentialWorkManager;
import com.pmease.gitplex.core.manager.StorageManager;
import com.pmease.gitplex.core.manager.WorkManager;
import com.pmease.gitplex.core.model.Depot;

import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.EnvironmentConfig;
import jetbrains.exodus.env.Environments;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.StoreConfig;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalComputable;
import jetbrains.exodus.env.TransactionalExecutable;

@Singleton
public class DefaultAuxiliaryManager implements AuxiliaryManager, DepotListener, RefListener, LifecycleListener {

	private static final Logger logger = LoggerFactory.getLogger(DefaultAuxiliaryManager.class);
	
	private static final String AUXILIARY_DIR = "auxiliary";
	
	private static final String DEFAULT_STORE = "default";
	
	private static final String COMMITS_STORE = "commmits";
	
	private static final String CONTRIBUTIONS_STORE = "contributions";
	
	private static final ByteIterable LAST_COMMIT_KEY = new StringByteIterable("lastCommit");
	
	private static final ByteIterable CONTRIBUTORS_KEY = new StringByteIterable("contributors");
	
	private static final ByteIterable FILES_KEY = new StringByteIterable("files");
	
	private static final int PRIORITY = 100;
	
	private final StorageManager storageManager;
	
	private final WorkManager workManager;
	
	private final SequentialWorkManager sequentialWorkManager;
	
	private final UnitOfWork unitOfWork;
	
	private final Dao dao;
	
	private final Map<Long, Environment> envs = new HashMap<>();
	
	private final Map<Long, List<String>> files = new ConcurrentHashMap<>();
	
	private final Map<Long, List<NameAndEmail>> contributors = new ConcurrentHashMap<>();
	
	@Inject
	public DefaultAuxiliaryManager(StorageManager storageManager, WorkManager workManager, 
			SequentialWorkManager sequentialWorkManager, Dao dao, UnitOfWork unitOfWork) {
		this.storageManager = storageManager;
		this.workManager = workManager;
		this.sequentialWorkManager = sequentialWorkManager;
		this.dao = dao;
		this.unitOfWork = unitOfWork;
	}
	
	private String getSequentialExecutorKey(Depot depot) {
		return "repository-" + depot.getId() + "-checkAuxiliary";
	}
	
	private void doCollect(final Depot depot, final String revision) {
		logger.debug("Collecting auxiliary information of repository {}...", depot.getFQN());
		Environment env = getEnv(depot);
		final Store defaultStore = getStore(env, DEFAULT_STORE);
		final Store commitsStore = getStore(env, COMMITS_STORE);
		final Store contributionsStore = getStore(env, CONTRIBUTIONS_STORE);

		final AtomicReference<String> lastCommit = new AtomicReference<>();
		env.executeInTransaction(new TransactionalExecutable() {
			
			@Override
			public void execute(final Transaction txn) {
				byte[] value = getBytes(defaultStore.get(txn, LAST_COMMIT_KEY));
				lastCommit.set(value!=null?new String(value):null);									
			}
		});
		
		env.executeInTransaction(new TransactionalExecutable() {
			
			@Override
			public void execute(final Transaction txn) {
				byte[] bytes = getBytes(defaultStore.get(txn, LAST_COMMIT_KEY));
				final AtomicReference<String> lastCommit;
				if (bytes != null)
					lastCommit = new AtomicReference<>(new String(bytes));
				else
					lastCommit = new AtomicReference<>(null);
				Git git = depot.git();

				LogCommand log = new LogCommand(git.depotDir());
				List<String> revisions = new ArrayList<>();
				if (lastCommit.get() != null) {
					revisions.add(lastCommit.get() + ".." + revision);
					lastCommit.set(null);
				} else { 
					revisions.add(revision);
				}
				
				final AtomicReference<Set<NameAndEmail>> contributors = new AtomicReference<>(null);
				final AtomicBoolean contributorsChanged = new AtomicBoolean(false);
				
				final AtomicReference<Set<String>> files = new AtomicReference<>(null);
				final AtomicBoolean filesChanged = new AtomicBoolean(false);
				
				log.revisions(revisions).listChangedFiles(true).run(new CommitConsumer() {

					@SuppressWarnings("unchecked")
					@Override
					public void consume(Commit commit) {
						byte[] keyBytes = new byte[20];
						ObjectId commitId = ObjectId.fromString(commit.getHash());
						commitId.copyRawTo(keyBytes, 0);
						ByteIterable key = new ArrayByteIterable(keyBytes);
						byte[] valueBytes = getBytes(commitsStore.get(txn, key));
						
						if (valueBytes == null || valueBytes.length%2 == 0) {
							/*
							 * Length of stored bytes of a commit is either 20*nChild
							 * (20 is length of ObjectId), or 1+20*nChild, as we need 
							 * an extra leading byte to differentiate commits being 
							 * processed and commits with child information attached 
							 * but not processed. 
							 */
							byte[] newValueBytes;
							if (valueBytes == null) {
								newValueBytes = new byte[1];
							} else {
								newValueBytes = new byte[valueBytes.length+1];
								System.arraycopy(valueBytes, 0, newValueBytes, 1, valueBytes.length);
							}
							commitsStore.put(txn, key, new ArrayByteIterable(newValueBytes));
							
							for (String parentHash: commit.getParentHashes()) {
								keyBytes = new byte[20];
								ObjectId.fromString(parentHash).copyRawTo(keyBytes, 0);
								key = new ArrayByteIterable(keyBytes);
								valueBytes = getBytes(commitsStore.get(txn, key));
								if (valueBytes != null) {
									newValueBytes = new byte[valueBytes.length+20];
									System.arraycopy(valueBytes, 0, newValueBytes, 0, valueBytes.length);
								} else {
									newValueBytes = new byte[20];
								}
								commitId.copyRawTo(newValueBytes, newValueBytes.length-20);
								commitsStore.put(txn, key, new ArrayByteIterable(newValueBytes));
							}
							if (contributors.get() == null) {
								byte[] bytes = getBytes(defaultStore.get(txn, CONTRIBUTORS_KEY));
								if (bytes != null)
									contributors.set((Set<NameAndEmail>) SerializationUtils.deserialize(bytes));
								else
									contributors.set(new HashSet<NameAndEmail>());
							}
							if (StringUtils.isNotBlank(commit.getAuthor().getName()) 
									|| StringUtils.isNotBlank(commit.getAuthor().getEmailAddress())) {
								NameAndEmail contributor = new NameAndEmail(commit.getAuthor());
								if (!contributors.get().contains(contributor)) {
									contributors.get().add(contributor);
									contributorsChanged.set(true);
								}
							}
							if (StringUtils.isNotBlank(commit.getCommitter().getName()) 
									|| StringUtils.isNotBlank(commit.getCommitter().getEmailAddress())) {
								NameAndEmail contributor = new NameAndEmail(commit.getCommitter());
								if (!contributors.get().contains(contributor)) {
									contributors.get().add(contributor);
									contributorsChanged.set(true);
								}
							}
							
							if (files.get() == null) {
								byte[] bytes = getBytes(defaultStore.get(txn, FILES_KEY));
								if (bytes != null)
									files.set((Set<String>) SerializationUtils.deserialize(bytes));
								else
									files.set(new HashSet<String>());
							}
							
							for (String file: commit.getChangedFiles()) {
								ByteIterable fileKey = new StringByteIterable(file);
								byte[] bytes = getBytes(contributionsStore.get(txn, fileKey));
								Map<NameAndEmail, Long> fileContributions;
								if (bytes != null)
									fileContributions = (Map<NameAndEmail, Long>) SerializationUtils.deserialize(bytes);
								else
									fileContributions = new HashMap<>();
								if (StringUtils.isNotBlank(commit.getAuthor().getName()) 
										|| StringUtils.isNotBlank(commit.getAuthor().getEmailAddress())) {
									NameAndEmail contributor = new NameAndEmail(commit.getAuthor());
									long contributionTime = commit.getAuthor().getWhen().getTime();
									Long lastContributionTime = fileContributions.get(contributor);
									if (lastContributionTime == null || lastContributionTime.longValue() < contributionTime)
										fileContributions.put(contributor, contributionTime);
								}													

								if (StringUtils.isNotBlank(commit.getCommitter().getName()) 
										|| StringUtils.isNotBlank(commit.getCommitter().getEmailAddress())) {
									NameAndEmail contributor = new NameAndEmail(commit.getCommitter());
									long contributionTime = commit.getCommitter().getWhen().getTime();
									Long lastContributionTime = fileContributions.get(contributor);
									if (lastContributionTime == null || lastContributionTime.longValue() < contributionTime)
										fileContributions.put(contributor, contributionTime);
								}
								
								bytes = SerializationUtils.serialize((Serializable) fileContributions);
								contributionsStore.put(txn, fileKey, new ArrayByteIterable(bytes));
								
								if (!files.get().contains(file)) {
									files.get().add(file);
									filesChanged.set(true);
								}
							}
							
							if (lastCommit.get() == null)
								lastCommit.set(commit.getHash());
						}
					}
					
				});
				
				if (contributorsChanged.get()) {
					bytes = SerializationUtils.serialize((Serializable) contributors.get());
					defaultStore.put(txn, CONTRIBUTORS_KEY, new ArrayByteIterable(bytes));
					DefaultAuxiliaryManager.this.contributors.remove(depot.getId());
				}
				if (filesChanged.get()) {
					bytes = SerializationUtils.serialize((Serializable) files.get());
					defaultStore.put(txn, FILES_KEY, new ArrayByteIterable(bytes));
					DefaultAuxiliaryManager.this.files.remove(depot.getId());
				}
				if (lastCommit.get() != null) {
					bytes = lastCommit.get().getBytes();
					defaultStore.put(txn, LAST_COMMIT_KEY, new ArrayByteIterable(bytes));
				}
			}
		});
		
		logger.debug("Auxiliary information collected for repository {}.", depot.getFQN());		
	}
	
	@Override
	public void collect(Depot depot, final String revision) {
		final Long repoId = depot.getId();
		sequentialWorkManager.execute(getSequentialExecutorKey(depot), new PrioritizedRunnable(PRIORITY) {

			@Override
			public void run() {
				try {
					workManager.submit(new PrioritizedRunnable(PRIORITY) {

						@Override
						public void run() {
							unitOfWork.call(new Callable<Void>() {

								@Override
								public Void call() throws Exception {
									doCollect(dao.load(Depot.class, repoId), revision);
									return null;
								}
								
							});
						}

					}).get();
				} catch (InterruptedException | ExecutionException e) {
					logger.error("Error collecting auxiliary information", e);
				}
			}

		});
	}
	
	private synchronized Environment getEnv(final Depot depot) {
		Environment env = envs.get(depot.getId());
		if (env == null) {
			EnvironmentConfig config = new EnvironmentConfig();
			config.setLogCacheShared(false);
			config.setMemoryUsage(1024*1024*64);
			config.setLogFileSize(64*1024);
			env = Environments.newInstance(getAuxiliaryDir(depot), config);
			envs.put(depot.getId(), env);
		}
		return env;
	}
	
	private File getAuxiliaryDir(Depot depot) {
		File auxiliaryDir = new File(storageManager.getCacheDir(depot), AUXILIARY_DIR);
		if (!auxiliaryDir.exists()) 
			FileUtils.createDir(auxiliaryDir);
		return auxiliaryDir;
	}
	
	private Store getStore(final Environment env, final String storeName) {
		return env.computeInTransaction(new TransactionalComputable<Store>() {
		    @Override
		    public Store compute(Transaction txn) {
		        return env.openStore(storeName, StoreConfig.WITHOUT_DUPLICATES, txn);
		    }
		});		
	}

	@Override
	public List<NameAndEmail> getContributors(Depot depot) {
		List<NameAndEmail> repoContributors = contributors.get(depot.getId());
		if (repoContributors == null) {
			Environment env = getEnv(depot);
			final Store store = getStore(env, DEFAULT_STORE);

			repoContributors = env.computeInReadonlyTransaction(new TransactionalComputable<List<NameAndEmail>>() {

				@SuppressWarnings("unchecked")
				@Override
				public List<NameAndEmail> compute(Transaction txn) {
					byte[] bytes = getBytes(store.get(txn, CONTRIBUTORS_KEY));
					if (bytes != null) { 
						List<NameAndEmail> repoContributors = 
								new ArrayList<>((Set<NameAndEmail>) SerializationUtils.deserialize(bytes));
						Collections.sort(repoContributors);
						return repoContributors;
					} else { 
						return new ArrayList<>();
					}
				}
			});
			contributors.put(depot.getId(), repoContributors);
		}
		return repoContributors;	
	}

	@Override
	public List<String> getFiles(Depot depot) {
		List<String> repoFiles = files.get(depot.getId());
		if (repoFiles == null) {
			Environment env = getEnv(depot);
			final Store store = getStore(env, DEFAULT_STORE);

			repoFiles = env.computeInReadonlyTransaction(new TransactionalComputable<List<String>>() {

				@SuppressWarnings("unchecked")
				@Override
				public List<String> compute(Transaction txn) {
					byte[] bytes = getBytes(store.get(txn, FILES_KEY));
					if (bytes != null) {
						List<Path> paths = new ArrayList<>();
						for (String file: (Set<String>)SerializationUtils.deserialize(bytes))
							paths.add(Paths.get(file));
						Collections.sort(paths, new Comparator<Path>() {

							@Override
							public int compare(Path path1, Path path2) {
								return path1.compareTo(path2);
							}
							
						});
						List<String> files = new ArrayList<>();
						for (Path path: paths)
							files.add(path.toString().replace('\\', '/'));
						return files;
					} else {
						return new ArrayList<>();
					}
				}
			});
			files.put(depot.getId(), repoFiles);
		}
		return repoFiles;
	}
	
	@Override
	public Map<String, Map<NameAndEmail, Long>> getContributions(Depot depot, final Set<String> files) {
		Environment env = getEnv(depot);
		final Store store = getStore(env, CONTRIBUTIONS_STORE);

		return env.computeInReadonlyTransaction(new TransactionalComputable<Map<String, Map<NameAndEmail, Long>>>() {

			@SuppressWarnings("unchecked")
			@Override
			public Map<String, Map<NameAndEmail, Long>> compute(Transaction txn) {
				Map<String, Map<NameAndEmail, Long>> fileContributors = new HashMap<>();
				for (String file: files) {
					ByteIterable fileKey = new StringByteIterable(file);
					Map<NameAndEmail, Long> contributions;
					byte[] value = getBytes(store.get(txn, fileKey));
					if (value != null)
						contributions = (Map<NameAndEmail, Long>) SerializationUtils.deserialize(value);
					else
						contributions = new HashMap<>();
					fileContributors.put(file, contributions);
				}
				return fileContributors;
			}
		});
	}

	@Override
	public Set<ObjectId> getDescendants(Depot depot, final ObjectId ancestor) {
		Environment env = getEnv(depot);
		final Store store = getStore(env, COMMITS_STORE);

		return env.computeInReadonlyTransaction(new TransactionalComputable<Set<ObjectId>>() {

			@Override
			public Set<ObjectId> compute(Transaction txn) {
				Set<ObjectId> descendants = new HashSet<>();
				
				// use stack instead of recursion to avoid StackOverflowException
				Stack<ObjectId> stack = new Stack<>();
				descendants.add(ancestor);
				stack.add(ancestor);
				while (!stack.isEmpty()) {
					ObjectId current = stack.pop();
					byte[] keyBytes = new byte[20];
					current.copyRawTo(keyBytes, 0);
					byte[] valueBytes = getBytes(store.get(txn, new ArrayByteIterable(keyBytes)));
					if (valueBytes != null) {
						if (valueBytes.length % 2 == 0) {
							for (int i=0; i<valueBytes.length/20; i++) {
								ObjectId child = ObjectId.fromRaw(valueBytes, i*20);
								if (!descendants.contains(child)) {
									descendants.add(child);
									stack.push(child);
								}
							}
						} else { 
							/*
							 * skip the leading byte, which tells whether or not the commit 
							 * has been processed
							 */
							for (int i=0; i<(valueBytes.length-1)/20; i++) {
								ObjectId child = ObjectId.fromRaw(valueBytes, i*20+1);
								if (!descendants.contains(child)) {
									descendants.add(child);
									stack.push(child);
								}
							}
						}
					}
				}
				
				return descendants;
			}
			
		});
	}

	@Override
	public Set<ObjectId> getChildren(Depot depot, final ObjectId parent) {
		Environment env = getEnv(depot);
		final Store store = getStore(env, COMMITS_STORE);

		return env.computeInReadonlyTransaction(new TransactionalComputable<Set<ObjectId>>() {

			@Override
			public Set<ObjectId> compute(Transaction txn) {
				Set<ObjectId> children = new HashSet<>();
				
				byte[] keyBytes = new byte[20];
				parent.copyRawTo(keyBytes, 0);
				byte[] valueBytes = getBytes(store.get(txn, new ArrayByteIterable(keyBytes)));
				if (valueBytes != null) {
					if (valueBytes.length % 2 == 0) {
						for (int i=0; i<valueBytes.length/20; i++) {
							children.add(ObjectId.fromRaw(valueBytes, i*20));
						}
					} else { 
						/*
						 * skip the leading byte, which tells whether or not the commit 
						 * has been processed
						 */
						for (int i=0; i<(valueBytes.length-1)/20; i++) {
							children.add(ObjectId.fromRaw(valueBytes, i*20+1));
						}
					}
				}
				return children;
			}
			
		});
	}

	@Override
	public void beforeDelete(Depot depot) {
	}

	@Override
	public synchronized void afterDelete(Depot depot) {
		sequentialWorkManager.removeExecutor(getSequentialExecutorKey(depot));
		Environment env = envs.remove(depot.getId());
		if (env != null)
			env.close();
		files.remove(depot.getId());
		FileUtils.deleteDir(getAuxiliaryDir(depot));
	}
	
	private byte[] getBytes(@Nullable ByteIterable byteIterable) {
		if (byteIterable != null)
			return Arrays.copyOf(byteIterable.getBytesUnsafe(), byteIterable.getLength());
		else
			return null;
	}
	
	static class StringByteIterable extends ArrayByteIterable {
		StringByteIterable(String value) {
			super(value.getBytes());
		}
	}

	@Override
	public void systemStarting() {
	}

	@Override
	public void systemStarted() {
	}
	
	@Override
	public void collect(Depot depot) {
		try (	Repository repository = depot.openRepository();
				RevWalk revWalk = new RevWalk(repository);) {
			Collection<Ref> refs = new ArrayList<>();
			refs.addAll(repository.getRefDatabase().getRefs(Constants.R_HEADS).values());
			refs.addAll(repository.getRefDatabase().getRefs(Constants.R_TAGS).values());
			
			for (Ref ref: refs) {
				RevObject revObj = revWalk.parseAny(ref.getObjectId());
				revObj = revWalk.peel(revObj);
				if (revObj instanceof RevCommit) {
					final String commitHash = revObj.name();
					Environment env = getEnv(depot);
					final Store commitsStore = getStore(env, COMMITS_STORE);
					boolean collected = env.computeInReadonlyTransaction(new TransactionalComputable<Boolean>() {
						
						@Override
						public Boolean compute(Transaction txn) {
							byte[] keyBytes = new byte[20];
							ObjectId commitId = ObjectId.fromString(commitHash);
							commitId.copyRawTo(keyBytes, 0);
							ByteIterable key = new ArrayByteIterable(keyBytes);
							byte[] valueBytes = getBytes(commitsStore.get(txn, key));
							return valueBytes != null && valueBytes.length%2 != 0;
						}
						
					});
					if (!collected) 
						collect(depot, commitHash);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public synchronized void systemStopping() {
		for (Environment env: envs.values())
			env.close();
	}

	@Override
	public void systemStopped() {
	}

	@Override
	public void onRefUpdate(Depot depot, String refName, String newCommitHash) {
		if (newCommitHash != null)
			collect(depot, newCommitHash);
	}

}
