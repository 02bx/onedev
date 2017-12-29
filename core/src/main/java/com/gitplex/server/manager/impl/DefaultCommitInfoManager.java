package com.gitplex.server.manager.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.SerializationUtils;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.jgit.revwalk.RevWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gitplex.launcher.loader.Listen;
import com.gitplex.server.event.RefUpdated;
import com.gitplex.server.event.lifecycle.SystemStarted;
import com.gitplex.server.git.Contribution;
import com.gitplex.server.git.Contributor;
import com.gitplex.server.git.GitUtils;
import com.gitplex.server.git.NameAndEmail;
import com.gitplex.server.git.command.FileChange;
import com.gitplex.server.git.command.LogCommand;
import com.gitplex.server.git.command.LogCommit;
import com.gitplex.server.git.command.RevListCommand;
import com.gitplex.server.manager.BatchWorkManager;
import com.gitplex.server.manager.CommitInfoManager;
import com.gitplex.server.manager.ProjectManager;
import com.gitplex.server.manager.StorageManager;
import com.gitplex.server.model.Project;
import com.gitplex.server.persistence.UnitOfWork;
import com.gitplex.server.persistence.annotation.Sessional;
import com.gitplex.server.persistence.dao.Dao;
import com.gitplex.server.persistence.dao.EntityRemoved;
import com.gitplex.server.util.BatchWorker;
import com.gitplex.server.util.Day;
import com.gitplex.server.util.facade.ProjectFacade;
import com.gitplex.server.util.facade.UserFacade;
import com.gitplex.utils.FileUtils;
import com.gitplex.utils.PathUtils;
import com.gitplex.utils.StringUtils;
import com.gitplex.utils.concurrent.Prioritized;
import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;

import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.backup.BackupStrategy;
import jetbrains.exodus.backup.BackupStrategy.FileDescriptor;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalComputable;
import jetbrains.exodus.env.TransactionalExecutable;

@Singleton
public class DefaultCommitInfoManager extends AbstractEnvironmentManager implements CommitInfoManager {

	private static final Logger logger = LoggerFactory.getLogger(DefaultCommitInfoManager.class);
	
	private static final int INFO_VERSION = 4;
	
	private static final long LOG_FILE_SIZE = 256*1024;
	
	private static final int COLLECT_BATCH_SIZE = 50000;
	
	private static final int MAX_COLLECTING_FILES = 50000;
	
	private static final int MAX_HISTORY_PATHS = 100;
	
	private static final String INFO_DIR = "commit";
	
	private static final String DEFAULT_STORE = "default";
	
	private static final String COMMITS_STORE = "commits";
	
	private static final String EDITS_STORE = "edits";

	private static final String HISTORY_PATHS_STORE = "historyPaths";
	
	private static final String PATH_TO_INDEX_STORE = "pathToIndex";
	
	private static final String INDEX_TO_PATH_STORE = "indexToPath";
	
	private static final String EMAIL_TO_INDEX_STORE = "emailToIndex";
	
	private static final String INDEX_TO_USER_STORE = "indexToUser";
	
	private static final String DAILY_CONTRIBUTIONS_STORE = "dailyContributions";
	
	private static final ByteIterable NEXT_PATH_INDEX_KEY = new StringByteIterable("nextPathIndex");
	
	private static final ByteIterable NEXT_USER_INDEX_KEY = new StringByteIterable("nextUserIndex");
	
	private static final ByteIterable LAST_COMMIT_KEY = new StringByteIterable("lastCommit");
	
	private static final ByteIterable USERS_KEY = new StringByteIterable("users");
	
	private static final ByteIterable FILES_KEY = new StringByteIterable("files");
	
	private static final ByteIterable OVERALL_CONTRIBUTIONS_KEY = new StringByteIterable("overallContributions");
	
	private static final ByteIterable COMMIT_COUNT_KEY = new StringByteIterable("commitCount");
	
	private static final int PRIORITY = 100;
	
	private final StorageManager storageManager;
	
	private final BatchWorkManager batchWorkManager;
	
	private final ExecutorService executorService;
	
	private final ProjectManager projectManager;
	
	private final UnitOfWork unitOfWork;
	
	private final Map<Long, List<String>> filesCache = new ConcurrentHashMap<>();
	
	private final Map<Long, Integer> commitCountCache = new ConcurrentHashMap<>();
	
	private final Map<Long, List<NameAndEmail>> usersCache = new ConcurrentHashMap<>();
	
	@Inject
	public DefaultCommitInfoManager(ProjectManager projectManager, StorageManager storageManager, 
			BatchWorkManager batchWorkManager, UnitOfWork unitOfWork, Dao dao, ExecutorService executorService) {
		this.projectManager = projectManager;
		this.storageManager = storageManager;
		this.batchWorkManager = batchWorkManager;
		this.unitOfWork = unitOfWork;
		this.executorService = executorService;
	}
	
	private void doCollect(Project project, ObjectId commitId, boolean divide) {
		Environment env = getEnv(project.getId().toString());
		Store defaultStore = getStore(env, DEFAULT_STORE);

		Repository repository = project.getRepository();
		if (divide) {
			List<ObjectId> intermediateCommitIds = new ArrayList<>();
			List<String> revisions = new ArrayList<>();
			revisions.add(commitId.name());

			ObjectId lastCommitId = env.computeInTransaction(new TransactionalComputable<ObjectId>() {

				@Override
				public ObjectId compute(Transaction txn) {
					byte[] bytes = readBytes(defaultStore, txn, LAST_COMMIT_KEY);
					return bytes!=null? ObjectId.fromRaw(bytes): null;
				}
				
			});
			if (lastCommitId != null) {
				if (repository.hasObject(lastCommitId))
					revisions.add("^" + lastCommitId.name());
			}
			
			int count = 0;
			for (String commitHash: new RevListCommand(repository.getDirectory()).revisions(revisions).call()) {
				count++;
				if (count > COLLECT_BATCH_SIZE) {
					intermediateCommitIds.add(ObjectId.fromString(commitHash));
					count = 0;
				}
			}
			
			for (int i=intermediateCommitIds.size()-1; i>=0; i--) {
				doCollect(project, intermediateCommitIds.get(i), false);
			}
			
			doCollect(project, commitId, false);
		} else {
			Store commitsStore = getStore(env, COMMITS_STORE);
			Store editsStore = getStore(env, EDITS_STORE); 
			Store historyPathsStore = getStore(env, HISTORY_PATHS_STORE);
			Store pathToIndexStore = getStore(env, PATH_TO_INDEX_STORE);
			Store indexToPathStore = getStore(env, INDEX_TO_PATH_STORE);
			Store emailToIndexStore = getStore(env, EMAIL_TO_INDEX_STORE);
			Store indexToUserStore = getStore(env, INDEX_TO_USER_STORE);
			Store dailyContributionsStore = getStore(env, DAILY_CONTRIBUTIONS_STORE);
			
			env.executeInTransaction(new TransactionalExecutable() {
				
				@SuppressWarnings("unchecked")
				@Override
				public void execute(Transaction txn) {
					try {
						byte[] bytes = readBytes(defaultStore, txn, LAST_COMMIT_KEY);
						
						ObjectId lastCommitId;
						if (bytes != null) {
							ObjectId commitId = ObjectId.fromRaw(bytes);
							if (repository.hasObject(commitId))
								lastCommitId = commitId;
							else
								lastCommitId = null;
						} else {
							lastCommitId = null;
						}
						
						int commitCount = readInt(defaultStore, txn, COMMIT_COUNT_KEY, 0);
						
						NextIndex nextIndex = new NextIndex();
						nextIndex.user = readInt(defaultStore, txn, NEXT_USER_INDEX_KEY, 0);
						nextIndex.path = readInt(defaultStore, txn, NEXT_PATH_INDEX_KEY, 0);
						
						Map<Integer, Map<Integer, Contribution>> dailyContributionsCache = new HashMap<>();
						Map<Integer, Contribution> overallContributions = 
								deserializeContributions(readBytes(defaultStore, txn, OVERALL_CONTRIBUTIONS_KEY));
						Map<Long, Integer> editsCache = new HashMap<>();
						
						Set<NameAndEmail> users;
						bytes = readBytes(defaultStore, txn, USERS_KEY);
						if (bytes != null)
							users = (Set<NameAndEmail>) SerializationUtils.deserialize(bytes);
						else
							users = new HashSet<>();

						Map<String, Long> files;
						bytes = readBytes(defaultStore, txn, FILES_KEY);
						if (bytes != null)
							files = (Map<String, Long>) SerializationUtils.deserialize(bytes);
						else
							files = new HashMap<>();
						
						/*
						 * Use a synchronous queue to achieve below purpose:
						 * 1. Add commit to Xodus transactional store in the same thread opening the transaction 
						 * as this is required by Xodus
						 * 2. Do not pile up commits to use minimal memory 
						 */
						SynchronousQueue<Optional<LogCommit>> queue = new SynchronousQueue<>(); 
						AtomicReference<Exception> logException = new AtomicReference<>(null);
						executorService.execute(new Runnable() {

							@Override
							public void run() {
								try {
									List<String> revisions = new ArrayList<>();
									revisions.add(commitId.name());
									if (lastCommitId != null)
										revisions.add("^" + lastCommitId.name());
									
									new LogCommand(repository.getDirectory()) {

										@Override
										protected void consume(LogCommit commit) {
											try {
												queue.put(Optional.of(commit));
											} catch (InterruptedException e) {
											}
										}
										
									}.revisions(revisions).call();
									
								} catch (Exception e) {
									logException.set(e);
								} finally {
									try {
										queue.put(Optional.empty());
									} catch (InterruptedException e) {
									}
								}
							}
							
						});
						
						Optional<LogCommit> commitOptional = queue.take();
						while (commitOptional.isPresent()) {
							LogCommit commit = commitOptional.get();
							
							byte[] keyBytes = new byte[20];
							ObjectId.fromString(commit.getHash()).copyRawTo(keyBytes, 0);
							ByteIterable key = new ArrayByteIterable(keyBytes);
							byte[] valueBytes = readBytes(commitsStore, txn, key);
							
							if (valueBytes == null || valueBytes.length % 20 == 0) {
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
								
								commitCount++;
								
								for (String parentHash: commit.getParentHashes()) {
									keyBytes = new byte[20];
									ObjectId.fromString(parentHash).copyRawTo(keyBytes, 0);
									key = new ArrayByteIterable(keyBytes);
									valueBytes = readBytes(commitsStore, txn, key);
									if (valueBytes != null) {
										newValueBytes = new byte[valueBytes.length+20];
										System.arraycopy(valueBytes, 0, newValueBytes, 0, valueBytes.length);
									} else {
										newValueBytes = new byte[20];
									}
									ObjectId.fromString(commit.getHash()).copyRawTo(newValueBytes, newValueBytes.length-20);
									commitsStore.put(txn, key, new ArrayByteIterable(newValueBytes));
								}
								
								if (commit.getCommitDate() != null) {
									for (String file: commit.getChangedFiles())
										files.put(file, commit.getCommitDate().getTime());
								}
								
								if (commit.getCommitter() != null)
									users.add(new NameAndEmail(commit.getCommitter()));

								if (commit.getAuthor() != null) {
									NameAndEmail nameAndEmail = new NameAndEmail(commit.getAuthor());
									users.add(nameAndEmail);
									
									String emailAddress = commit.getAuthor().getEmailAddress();
									if (StringUtils.isNotBlank(emailAddress)) {
										key = new StringByteIterable(emailAddress);
										int userIndex = readInt(emailToIndexStore, txn, key, -1);
										if (userIndex == -1) {
											userIndex = nextIndex.user++;
											writeInt(emailToIndexStore, txn, key, userIndex);
											indexToUserStore.put(txn, 
													new IntByteIterable(userIndex), 
													new ArrayByteIterable(SerializationUtils.serialize(nameAndEmail)));
										}
										
										for (FileChange change: commit.getFileChanges()) {
											String path = change.getPath();
											int pathIndex = getPathIndex(pathToIndexStore, indexToPathStore, txn, 
													nextIndex, path);
											int edits = change.getAdditions() + change.getDeletions();
											updateEdits(editsStore, txn, editsCache, 
													userIndex, pathIndex, edits);
											while (path.contains("/")) {
												path = StringUtils.substringBeforeLast(path, "/");
												pathIndex = getPathIndex(pathToIndexStore, indexToPathStore, txn, 
														nextIndex, path);
												updateEdits(editsStore, txn, editsCache, 
														userIndex, pathIndex, edits);
											}
											pathIndex = getPathIndex(pathToIndexStore, indexToPathStore, txn, 
													nextIndex, "");
											updateEdits(editsStore, txn, editsCache, userIndex, pathIndex, edits);
										}

										if (commit.getCommitDate() != null && commit.getParentHashes().size() <= 1) {
											int dayValue = new Day(commit.getCommitDate()).getValue();
											Map<Integer, Contribution> contributionsOnDay = 
													dailyContributionsCache.get(dayValue);
											if (contributionsOnDay == null) {
												contributionsOnDay = deserializeContributions(readBytes(
														dailyContributionsStore, txn, new IntByteIterable(dayValue)));
												dailyContributionsCache.put(dayValue, contributionsOnDay);
											}
											updateContribution(contributionsOnDay, userIndex, commit);
											
											updateContribution(overallContributions, dayValue, commit);
										}
									}
								}
								
								for (FileChange change: commit.getFileChanges()) {
									if (change.getOldPath() != null) {
										int pathIndex = getPathIndex(pathToIndexStore, indexToPathStore, txn, 
												nextIndex, change.getPath());
										ByteIterable pathKey = new IntByteIterable(pathIndex);
										Set<Integer> historyPathIndexes = new HashSet<>();
										byte[] bytesOfHistoryPaths = readBytes(historyPathsStore, txn, pathKey);
										if (bytesOfHistoryPaths == null) {
											bytesOfHistoryPaths = new byte[0];
											int pos = 0;
											for (int i=0; i<bytesOfHistoryPaths.length/Integer.SIZE; i++) {
												historyPathIndexes.add(ByteBuffer.wrap(bytesOfHistoryPaths, pos, Integer.SIZE).getInt());
												pos += Integer.SIZE;
											}
										} else {
											historyPathIndexes = new HashSet<>();
										}
										if (historyPathIndexes.size() < MAX_HISTORY_PATHS) {
											int oldPathIndex = getPathIndex(pathToIndexStore, indexToPathStore, txn, 
													nextIndex, change.getOldPath());
											if (!historyPathIndexes.contains(oldPathIndex)) {
												historyPathIndexes.add(oldPathIndex);
												byte[] newBytesOfHistoryPaths = 
														new byte[bytesOfHistoryPaths.length+Integer.SIZE];
												System.arraycopy(bytesOfHistoryPaths, 0, 
														newBytesOfHistoryPaths, 0, bytesOfHistoryPaths.length);
												ByteBuffer buffer = ByteBuffer.wrap(newBytesOfHistoryPaths, 
														bytesOfHistoryPaths.length, Integer.BYTES);
												buffer.putInt(oldPathIndex);
												historyPathsStore.put(txn, pathKey, 
														new ArrayByteIterable(newBytesOfHistoryPaths));
											}
										}
									}
								}
								
							}		
							commitOptional = queue.take();
						}
						if (logException.get() != null)
							throw logException.get();
						
						writeInt(defaultStore, txn, COMMIT_COUNT_KEY, commitCount);
						commitCountCache.remove(project.getId());
						
						writeInt(defaultStore, txn, NEXT_USER_INDEX_KEY, nextIndex.user);
						writeInt(defaultStore, txn, NEXT_PATH_INDEX_KEY, nextIndex.path);
						
						bytes = SerializationUtils.serialize((Serializable) users);
						defaultStore.put(txn, USERS_KEY, new ArrayByteIterable(bytes));
						usersCache.remove(project.getId());
						
						if (files.size() > MAX_COLLECTING_FILES) {
							List<String> fileList = new ArrayList<>(files.keySet());
							fileList.sort((file1, file2)->files.get(file1).compareTo(files.get(file2)));
							for (int i=0; i<fileList.size() - MAX_COLLECTING_FILES; i++)
								files.remove(fileList.get(i));
						}
						bytes = SerializationUtils.serialize((Serializable) files);
						defaultStore.put(txn, FILES_KEY, new ArrayByteIterable(bytes));
						filesCache.remove(project.getId());
						
						for (Map.Entry<Integer, Map<Integer, Contribution>> entry: dailyContributionsCache.entrySet()) {
							byte[] bytesOfContributionsOnDay = serializeContributions(entry.getValue());
							dailyContributionsStore.put(txn, new IntByteIterable(entry.getKey()), 
									new ArrayByteIterable(bytesOfContributionsOnDay));
						}
						defaultStore.put(txn, OVERALL_CONTRIBUTIONS_KEY, 
								new ArrayByteIterable(serializeContributions(overallContributions)));
						
						for (Map.Entry<Long, Integer> entry: editsCache.entrySet()) 
							writeInt(editsStore, txn, new LongByteIterable(entry.getKey()), entry.getValue());
						
						bytes = new byte[20];
						commitId.copyRawTo(bytes, 0);
						defaultStore.put(txn, LAST_COMMIT_KEY, new ArrayByteIterable(bytes));
					} catch (Exception e) {
						Throwables.propagate(e);
					}
				}
				
			});			
		}
	}
	
	private void updateContribution(Map<Integer, Contribution> contributions, int key, LogCommit commit) {
		Contribution contribution = contributions.get(key);
		if (contribution != null) {
			contribution = new Contribution(
					contribution.getCommits()+1, 
					contribution.getAdditions()+commit.getAdditions(), 
					contribution.getDeletions()+commit.getDeletions());
		} else {
			contribution = new Contribution(1, commit.getAdditions(), commit.getDeletions());
		}
		contributions.put(key, contribution);
	}
	
	private int getPathIndex(Store pathToIndexStore, Store indexToPathStore, Transaction txn, 
			NextIndex nextIndex, String path) {
		StringByteIterable pathKey = new StringByteIterable(path);
		int pathIndex = readInt(pathToIndexStore, txn, pathKey, -1);
		if (pathIndex == -1) {
			pathIndex = nextIndex.path++;
			writeInt(pathToIndexStore, txn, pathKey, pathIndex);
			indexToPathStore.put(txn, new IntByteIterable(pathIndex), new StringByteIterable(path));
		}
		return pathIndex;
	}
	
	private void updateEdits(Store store, Transaction txn, 
			Map<Long, Integer> editsCache, int userIndex, int pathIndex, int edits) {
		long editsKey = (userIndex<<32)|pathIndex;
		
		Integer editsOfPathByUser = editsCache.get(editsKey);
		if (editsOfPathByUser == null)
			editsOfPathByUser = readInt(store, txn, new LongByteIterable(editsKey), 0);
		editsOfPathByUser += edits;
		editsCache.put(editsKey, editsOfPathByUser);
	}
	
	@Override
	public List<NameAndEmail> getUsers(Project project) {
		List<NameAndEmail> users = usersCache.get(project.getId());
		if (users == null) {
			Environment env = getEnv(project.getId().toString());
			Store store = getStore(env, DEFAULT_STORE);

			users = env.computeInReadonlyTransaction(new TransactionalComputable<List<NameAndEmail>>() {

				@SuppressWarnings("unchecked")
				@Override
				public List<NameAndEmail> compute(Transaction txn) {
					byte[] bytes = readBytes(store, txn, USERS_KEY);
					if (bytes != null) { 
						List<NameAndEmail> users = 
								new ArrayList<>((Set<NameAndEmail>) SerializationUtils.deserialize(bytes));
						Collections.sort(users);
						return users;
					} else { 
						return new ArrayList<>();
					}
				}
				
			});
			usersCache.put(project.getId(), users);
		}
		return users;	
	}

	@Override
	public List<String> getFiles(Project project) {
		List<String> files = filesCache.get(project.getId());
		if (files == null) {
			Environment env = getEnv(project.getId().toString());
			final Store store = getStore(env, DEFAULT_STORE);

			files = env.computeInReadonlyTransaction(new TransactionalComputable<List<String>>() {

				@SuppressWarnings("unchecked")
				@Override
				public List<String> compute(Transaction txn) {
					byte[] bytes = readBytes(store, txn, FILES_KEY);
					if (bytes != null) {
						List<String> files = new ArrayList<>(
								((Map<String, Long>)SerializationUtils.deserialize(bytes)).keySet());
						Map<String, List<String>> segmentsMap = new HashMap<>();
						Splitter splitter = Splitter.on("/");
						for (String file: files) {
							segmentsMap.put(file, splitter.splitToList(file));
						}
						files.sort(new Comparator<String>() {

							@Override
							public int compare(String o1, String o2) {
								return PathUtils.compare(segmentsMap.get(o1), segmentsMap.get(o2));
							}
							
						});
						return files;
					} else {
						return new ArrayList<>();
					}
				}
			});
			filesCache.put(project.getId(), files);
		}
		return files;
	}

	@Override
	public int getEdits(ProjectFacade project, UserFacade user, String path) {
		if (user.getEmail() != null) {
			Environment env = getEnv(project.getId().toString());
			Store emailToIndexStore = getStore(env, EMAIL_TO_INDEX_STORE);
			Store pathToIndexStore = getStore(env, PATH_TO_INDEX_STORE);
			Store editsStore = getStore(env, EDITS_STORE);
			return env.computeInReadonlyTransaction(new TransactionalComputable<Integer>() {

				@Override
				public Integer compute(Transaction txn) {
					int userIndex = readInt(emailToIndexStore, txn, new StringByteIterable(user.getEmail()), -1);
					if (userIndex != -1) {
						int pathIndex = readInt(pathToIndexStore, txn, new StringByteIterable(path), -1);
						if (pathIndex != -1) {
							long editsKey = (userIndex<<32)|pathIndex;
							return readInt(editsStore, txn, new LongByteIterable(editsKey), 0);
						} 
					} 
					return 0;
				}
			});
		} else {
			return 0;
		}
	}
	
	@Override
	public Set<ObjectId> getDescendants(Project project, final ObjectId ancestor) {
		Environment env = getEnv(project.getId().toString());
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
					byte[] valueBytes = readBytes(store, txn, new ArrayByteIterable(keyBytes));
					if (valueBytes != null) {
						if (valueBytes.length % 20 == 0) {
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
	public Set<ObjectId> getChildren(Project project, final ObjectId parent) {
		Environment env = getEnv(project.getId().toString());
		final Store store = getStore(env, COMMITS_STORE);

		return env.computeInReadonlyTransaction(new TransactionalComputable<Set<ObjectId>>() {

			@Override
			public Set<ObjectId> compute(Transaction txn) {
				Set<ObjectId> children = new HashSet<>();
				
				byte[] keyBytes = new byte[20];
				parent.copyRawTo(keyBytes, 0);
				byte[] valueBytes = readBytes(store, txn, new ArrayByteIterable(keyBytes));
				if (valueBytes != null) {
					if (valueBytes.length % 20 == 0) {
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

	@Listen
	public void on(EntityRemoved event) {
		if (event.getEntity() instanceof Project) {
			Long projectId = event.getEntity().getId();
			removeEnv(projectId.toString());
			filesCache.remove(projectId);
			commitCountCache.remove(projectId);
			usersCache.remove(projectId);
		}
	}
	
	private BatchWorker getBatchWorker(Long projectId) {
		return new BatchWorker("project-" + projectId + "-collectCommitInfo") {

			@Override
			public void doWorks(Collection<Prioritized> works) {
				unitOfWork.run(new Runnable() {

					@Override
					public void run() {
						Project project = projectManager.load(projectId);
						List<CollectingWork> collectingWorks = new ArrayList<>();
						for (Object work: works)
							collectingWorks.add((CollectingWork)work);
						Collections.sort(collectingWorks, new CommitTimeComparator());
						
						for (CollectingWork work: collectingWorks) {
							logger.debug("Collecting commit information up to ref '{}' in project '{}'...", 
									work.getRefName(), project.getName());
							doCollect(project, work.getCommit().copy(), true);
						}
					}
					
				});
			}
			
		};		
	}
	
	private void collect(Project project) {
		Environment env = getEnv(project.getId().toString());
		Store commitsStore = getStore(env, COMMITS_STORE);
		env.computeInReadonlyTransaction(new TransactionalComputable<Void>() {
			
			@Override
			public Void compute(Transaction txn) {
				List<CollectingWork> works = new ArrayList<>();
				try (RevWalk revWalk = new RevWalk(project.getRepository())) {
					Collection<Ref> refs = new ArrayList<>();
					refs.addAll(project.getRepository().getRefDatabase().getRefs(Constants.R_HEADS).values());
					refs.addAll(project.getRepository().getRefDatabase().getRefs(Constants.R_TAGS).values());

					for (Ref ref: refs) {
						RevObject revObj = revWalk.peel(revWalk.parseAny(ref.getObjectId()));
						if (revObj instanceof RevCommit) {
							RevCommit commit = (RevCommit) revObj;
							byte[] keyBytes = new byte[20];
							commit.copyRawTo(keyBytes, 0);
							ByteIterable key = new ArrayByteIterable(keyBytes);
							byte[] valueBytes = readBytes(commitsStore, txn, key);
							if (valueBytes == null || valueBytes.length % 20 == 0) 
								works.add(new CollectingWork(PRIORITY, commit, ref.getName()));
						}
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}

				Collections.sort(works, new CommitTimeComparator());
				
				for (CollectingWork work: works)
					batchWorkManager.submit(getBatchWorker(project.getId()), work);
				
				return null;
			}
			
		});
	}

	@Sessional
	@Listen
	public void on(SystemStarted event) {
		for (Project project: projectManager.findAll()) {
			checkVersion(project.getId().toString());
			collect(project);
		}
	}
	
	@Sessional
	@Listen
	public void on(RefUpdated event) {
		if (!event.getNewObjectId().equals(ObjectId.zeroId()) 
				&& (event.getRefName().startsWith(Constants.R_HEADS) 
						|| event.getRefName().startsWith(Constants.R_TAGS))) {
			try (RevWalk revWalk = new RevWalk(event.getProject().getRepository())) {
				RevCommit commit = GitUtils.parseCommit(revWalk, event.getNewObjectId());
				if (commit != null) {
					CollectingWork work = new CollectingWork(PRIORITY, commit, event.getRefName());
					batchWorkManager.submit(getBatchWorker(event.getProject().getId()), work);
				}
			}
		}
	}

	@Sessional
	@Override
	public int getCommitCount(Project project) {
		Integer commitCount = commitCountCache.get(project.getId());
		if (commitCount == null) {
			Environment env = getEnv(project.getId().toString());
			Store store = getStore(env, DEFAULT_STORE);

			commitCount = env.computeInReadonlyTransaction(new TransactionalComputable<Integer>() {

				@Override
				public Integer compute(Transaction txn) {
					return readInt(store, txn, COMMIT_COUNT_KEY, 0);
				}
			});
			commitCountCache.put(project.getId(), commitCount);
		}
		return commitCount;
	}

	static class CollectingWork extends Prioritized {
		
		private final String refName;
		
		private final RevCommit commit;
		
		public CollectingWork(int priority, RevCommit commit, String refName) {
			super(priority);
			this.commit = commit;
			this.refName = refName;
		}

		public RevCommit getCommit() {
			return commit;
		}

		public String getRefName() {
			return refName;
		}

	}
	
	static class CommitTimeComparator implements Comparator<CollectingWork> {

		@Override
		public int compare(CollectingWork o1, CollectingWork o2) {
			return o1.getCommit().getCommitTime() - o2.getCommit().getCommitTime();
		}
		
	}

	@Sessional
	@Override
	public void cloneInfo(Project source, Project target) {
		BackupStrategy backupStrategy = getEnv(source.getId().toString()).getBackupStrategy();
		try {
			File targetDir = getEnvDir(target.getId().toString());
			backupStrategy.beforeBackup();
			try {
				for (FileDescriptor descriptor: backupStrategy.listFiles()) {
					FileUtils.copyFileToDirectory(descriptor.getFile(), targetDir);
				}
			} finally {
				backupStrategy.afterBackup();
			}
			writeVersion(target.getId().toString());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected File getEnvDir(String envKey) {
		File infoDir = new File(storageManager.getProjectInfoDir(Long.valueOf(envKey)), INFO_DIR);
		if (!infoDir.exists()) 
			FileUtils.createDir(infoDir);
		return infoDir;
	}
	
	@Sessional
	@Override
	public Collection<String> getHistoryPaths(Project project, String path) {
		Environment env = getEnv(project.getId().toString());
		Store historyPathsStore = getStore(env, HISTORY_PATHS_STORE);
		Store pathToIndexStore = getStore(env, PATH_TO_INDEX_STORE);
		Store indexToPathStore = getStore(env, INDEX_TO_PATH_STORE);
		
		return env.computeInReadonlyTransaction(new TransactionalComputable<Collection<String>>() {

			private Collection<String> getPaths(Transaction txn, Set<Integer> pathIndexes) {
				Set<String> paths = new HashSet<>();
				for (int pathIndex: pathIndexes) {
					byte[] pathBytes = readBytes(indexToPathStore, txn, new IntByteIterable(pathIndex));
					if (pathBytes != null)
						paths.add(new String(pathBytes, Charsets.UTF_8));
				}
				return paths;
			}
			
			@Override
			public Collection<String> compute(Transaction txn) {
				int pathIndex = readInt(pathToIndexStore, txn, new StringByteIterable(path), -1);
				if (pathIndex != -1) {
					Set<Integer> pathIndexes = new HashSet<>();
					pathIndexes.add(pathIndex);
					while (true) {
						Set<Integer> newPathIndexes = new HashSet<>(pathIndexes);
						for (int eachPathIndex: pathIndexes) {
							byte[] bytesOfHistoryPaths = 
									readBytes(historyPathsStore, txn, new IntByteIterable(eachPathIndex));
							if (bytesOfHistoryPaths != null) {
								int pos = 0;
								for (int i=0; i<bytesOfHistoryPaths.length/Integer.BYTES; i++) {
									newPathIndexes.add(ByteBuffer.wrap(bytesOfHistoryPaths, pos, Integer.BYTES).getInt());
									if (newPathIndexes.size() == MAX_HISTORY_PATHS)
										return getPaths(txn, newPathIndexes);
									pos += Integer.BYTES;
								}
							}
						}
						if (pathIndexes.equals(newPathIndexes))
							break;
						else
							pathIndexes = newPathIndexes;
					}
					return getPaths(txn, pathIndexes);
				} else {
					return new HashSet<>();
				}
			}
		});
	}
	
	@Sessional
	@Override
	public Map<Day, Contribution> getOverallContributions(Project project) {
		Environment env = getEnv(project.getId().toString());
		Store store = getStore(env, DEFAULT_STORE);

		return env.computeInReadonlyTransaction(new TransactionalComputable<Map<Day, Contribution>>() {

			@Override
			public Map<Day, Contribution> compute(Transaction txn) {
				Map<Day, Contribution> overallContributions = new HashMap<>();
				for (Map.Entry<Integer, Contribution> entry: 
							deserializeContributions(readBytes(store, txn, OVERALL_CONTRIBUTIONS_KEY)).entrySet()) {
					overallContributions.put(new Day(entry.getKey()), entry.getValue());
				}
				return overallContributions;
			}
			
		});
	}
	
	@Sessional
	@Override
	public List<Contributor> getTopContributors(Project project, int top, Contribution.Type orderBy, 
			Day fromDay, Day toDay) {
		Environment env = getEnv(project.getId().toString());
		Store defaultStore = getStore(env, DEFAULT_STORE);
		Store indexToUserStore = getStore(env, INDEX_TO_USER_STORE);
		Store dailyContributionsStore = getStore(env, DAILY_CONTRIBUTIONS_STORE);
		
		return env.computeInReadonlyTransaction(new TransactionalComputable<List<Contributor>>() {

			@Override
			public List<Contributor> compute(Transaction txn) {
				Map<Integer, Contribution> overallContributions = 
						deserializeContributions(readBytes(defaultStore, txn, OVERALL_CONTRIBUTIONS_KEY));
				Map<Integer, Integer> orderByValues = new HashMap<>();
				for (int dayValue: overallContributions.keySet()) {
					if (dayValue >= fromDay.getValue() && dayValue <= toDay.getValue()) {
						ByteIterable dayKey = new IntByteIterable(dayValue);
						Map<Integer, Contribution> contributionsOnDay = 
								deserializeContributions(readBytes(dailyContributionsStore, txn, dayKey));
						for (Map.Entry<Integer, Contribution> entry: contributionsOnDay.entrySet()) {
							int orderByValue;
							if (orderBy == Contribution.Type.COMMITS)
								orderByValue = entry.getValue().getCommits();
							else if (orderBy == Contribution.Type.ADDITIONS)
								orderByValue = entry.getValue().getAdditions();
							else
								orderByValue = entry.getValue().getDeletions();
							Integer userIndex = entry.getKey();
							Integer existingOrderByValue = orderByValues.get(userIndex);
							if (existingOrderByValue != null)
								orderByValue += existingOrderByValue;
							orderByValues.put(userIndex, orderByValue);
						}
					}
				}
				
				List<Integer> topUserIndexes = new ArrayList<>(orderByValues.keySet());
				Collections.sort(topUserIndexes, new Comparator<Integer>() {

					@Override
					public int compare(Integer o1, Integer o2) {
						return orderByValues.get(o2) - orderByValues.get(o1);
					}
					
				});

				if (top < topUserIndexes.size())
					topUserIndexes = topUserIndexes.subList(0, top);
				
				Set<Integer> topUserIndexSet = new HashSet<>(topUserIndexes);
				
				Map<Integer, Map<Day, Contribution>> userContributions = new HashMap<>();
				
				for (int dayValue: overallContributions.keySet()) {
					if (dayValue >= fromDay.getValue() && dayValue <= toDay.getValue()) {
						ByteIterable dayKey = new IntByteIterable(dayValue);
						Map<Integer, Contribution> contributionsOnDay = 
								deserializeContributions(readBytes(dailyContributionsStore, txn, dayKey));
						Day day = new Day(dayValue);
						for (Map.Entry<Integer, Contribution> entry: contributionsOnDay.entrySet()) {
							Integer userIndex = entry.getKey();
							if (topUserIndexSet.contains(userIndex)) {
								Map<Day, Contribution> contributionsByUser = userContributions.get(userIndex);
								if (contributionsByUser == null) {
									contributionsByUser = new HashMap<>();
									userContributions.put(userIndex, contributionsByUser);
								}
								contributionsByUser.put(day, entry.getValue());
							}
						}
					}
				}

				List<Contributor> contributors = new ArrayList<>();
				
				for (int userIndex: topUserIndexes) {
					byte[] userBytes = readBytes(indexToUserStore, txn, new IntByteIterable(userIndex));
					Map<Day, Contribution> contributionsByUser = userContributions.get(userIndex);
					if (userBytes != null && contributionsByUser != null) {
						PersonIdent user = ((NameAndEmail)SerializationUtils.deserialize(userBytes)).asPersonIdent();
						contributors.add(new Contributor(user, contributionsByUser));
					}
				}
				
				return contributors;
			}
			
		});
	}

	private Map<Integer, Contribution> deserializeContributions(byte[] bytes) {
		if (bytes != null) {
			Map<Integer, Contribution> contributions = new HashMap<>();
			int pos = 0;
			for (int i=0; i<bytes.length/4/Integer.BYTES; i++) {
				int key = ByteBuffer.wrap(bytes, pos, Integer.BYTES).getInt();
				pos += Integer.BYTES;
				int commits = ByteBuffer.wrap(bytes, pos, Integer.BYTES).getInt(); 
				pos += Integer.BYTES;
				int additions = ByteBuffer.wrap(bytes, pos, Integer.BYTES).getInt();
				pos += Integer.BYTES;
				int deletions = ByteBuffer.wrap(bytes, pos, Integer.BYTES).getInt(); 
				pos += Integer.BYTES;
				contributions.put(key, new Contribution(commits, additions, deletions));
			}
			return contributions;
		} else {
			return new HashMap<>();
		}
	}
	
	private byte[] serializeContributions(Map<Integer, Contribution> contributions) {
		byte[] bytes = new byte[contributions.size()*Integer.BYTES*4];
		int pos = 0;
		for (Map.Entry<Integer, Contribution> entry: contributions.entrySet()) {
			byte[] keyBytes = ByteBuffer.allocate(Integer.BYTES).putInt(entry.getKey()).array(); 
			System.arraycopy(keyBytes, 0, bytes, pos, Integer.BYTES);
			pos += Integer.BYTES;
			byte[] commitsBytes = ByteBuffer.allocate(Integer.BYTES).putInt(entry.getValue().getCommits()).array();
			System.arraycopy(commitsBytes, 0, bytes, pos, Integer.BYTES);
			pos += Integer.BYTES;
			byte[] additionsBytes = ByteBuffer.allocate(Integer.BYTES).putInt(entry.getValue().getAdditions()).array();
			System.arraycopy(additionsBytes, 0, bytes, pos, Integer.BYTES);
			pos += Integer.BYTES;
			byte[] deletionsBytes = ByteBuffer.allocate(Integer.BYTES).putInt(entry.getValue().getDeletions()).array();
			System.arraycopy(deletionsBytes, 0, bytes, pos, Integer.BYTES);
			pos += Integer.BYTES;
		}
		return bytes;
	}
	
	@Override
	protected long getLogFileSize() {
		return LOG_FILE_SIZE;
	}

	@Override
	protected int getEnvVersion() {
		return INFO_VERSION;
	}

	private static class NextIndex {
		int user;
		
		int path;
	}

}
