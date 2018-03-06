package io.onedev.server.manager.impl;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.SerializationUtils;

import io.onedev.launcher.loader.Listen;
import io.onedev.launcher.loader.ListenerRegistry;
import io.onedev.server.event.pullrequest.PullRequestVerificationEvent;
import io.onedev.server.event.pullrequest.PullRequestVerificationFailed;
import io.onedev.server.event.pullrequest.PullRequestVerificationInError;
import io.onedev.server.event.pullrequest.PullRequestVerificationRunning;
import io.onedev.server.event.pullrequest.PullRequestVerificationSucceeded;
import io.onedev.server.manager.PullRequestManager;
import io.onedev.server.manager.StorageManager;
import io.onedev.server.manager.VerificationManager;
import io.onedev.server.model.Project;
import io.onedev.server.model.PullRequest;
import io.onedev.server.persistence.annotation.Transactional;
import io.onedev.server.persistence.dao.EntityRemoved;
import io.onedev.server.util.Verification;
import io.onedev.server.util.Verification.Status;
import io.onedev.utils.FileUtils;
import jetbrains.exodus.ArrayByteIterable;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Environment;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalComputable;
import jetbrains.exodus.env.TransactionalExecutable;

@Singleton
public class DefaultVerificationManager extends AbstractEnvironmentManager implements VerificationManager {

	private static final int INFO_VERSION = 4;
	
	private static final String INFO_DIR = "verification";
	
	private static final String DEFAULT_STORE = "default";
	
	private static final String VERIFICATIONS_STORE = "verifications";
	
	private static final String VERIFICATION_NAMES_KEY = "verificationNames";
	
	private static final long MAX_VERIFICATION_NAMES = 1000;
	
	private final StorageManager storageManager;
	
	private final PullRequestManager pullRequestManager;
	
	private final ListenerRegistry listenerRegistry;
	
	@Inject
	public DefaultVerificationManager(StorageManager storageManager, PullRequestManager pullRequestManager, 
			ListenerRegistry listenerRegistry) {
		this.storageManager = storageManager;
		this.pullRequestManager = pullRequestManager;
		this.listenerRegistry = listenerRegistry;
	}
	
	@Transactional
	@Override
	public void saveVerification(Project project, String commit, String name, Verification verification) {
		Environment env = getEnv(project.getId().toString());
		Store defaultStore = getStore(env, DEFAULT_STORE);
		Store verificationsStore = getStore(env, VERIFICATIONS_STORE);
		env.executeInTransaction(new TransactionalExecutable() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void execute(Transaction txn) {
				ByteIterable key = new StringByteIterable(VERIFICATION_NAMES_KEY);
				byte[] bytes = readBytes(defaultStore, txn, key);
				Map<String, Date> verificationNames;
				if (bytes != null)
					verificationNames = (Map<String, Date>) SerializationUtils.deserialize(bytes);
				else
					verificationNames = new HashMap<>();
				
				verificationNames.put(name, verification.getDate());

				if (verificationNames.size() > MAX_VERIFICATION_NAMES) {
					List<String> nameList = new ArrayList<>(verificationNames.keySet());
					nameList.sort((o1, o2)->verificationNames.get(o1).compareTo(verificationNames.get(o2)));
					for (int i=0; i<nameList.size()-MAX_VERIFICATION_NAMES; i++)
						verificationNames.remove(nameList.get(i));
				}
				
				defaultStore.put(txn, key, 
						new ArrayByteIterable(SerializationUtils.serialize((Serializable) verificationNames)));
				
				key = new StringByteIterable(commit);
				bytes = readBytes(verificationsStore, txn, key);
				Map<String, Verification> verifications;
				if (bytes != null)
					verifications = (Map<String, Verification>) SerializationUtils.deserialize(bytes);
				else
					verifications = new HashMap<>();

				verifications.put(name, verification);
				
				verificationsStore.put(txn, key, new ArrayByteIterable(SerializationUtils.serialize((Serializable) verifications)));
			}
			
		});
		
		for (PullRequest request: pullRequestManager.findOpenByVerifyCommit(commit)) {
			PullRequestVerificationEvent event;
			if (verification.getStatus() == Status.ERROR)
				event = new PullRequestVerificationInError(request);
			else if (verification.getStatus() == Status.FAILURE)
				event = new PullRequestVerificationFailed(request);
			else if (verification.getStatus() == Status.RUNNING)
				event = new PullRequestVerificationRunning(request);
			else
				event = new PullRequestVerificationSucceeded(request);
			
			listenerRegistry.post(event);
			
			request.setLastEvent(event);
			pullRequestManager.save(request);
		}
		
	}

	@Override
	public Map<String, Verification> getVerifications(Project project, String commit) {
		Environment env = getEnv(project.getId().toString());
		Store store = getStore(env, VERIFICATIONS_STORE);
		return env.computeInTransaction(new TransactionalComputable<Map<String, Verification>>() {
			
			@SuppressWarnings("unchecked")
			@Override
			public Map<String, Verification> compute(Transaction txn) {
				byte[] bytes = readBytes(store, txn, new StringByteIterable(commit));
				if (bytes != null)
					return (Map<String, Verification>) SerializationUtils.deserialize(bytes);
				else
					return new HashMap<>();
			}
			
		});
	}

	@Transactional
	@Listen
	public void on(EntityRemoved event) {
		if (event.getEntity() instanceof Project)
			removeEnv(event.getEntity().getId().toString());
	}
	
	@Override
	protected File getEnvDir(String envKey) {
		File infoDir = new File(storageManager.getProjectInfoDir(Long.valueOf(envKey)), INFO_DIR);
		if (!infoDir.exists()) 
			FileUtils.createDir(infoDir);
		return infoDir;
	}

	@Override
	protected int getEnvVersion() {
		return INFO_VERSION;
	}

	@Override
	public Collection<String> getVerificationNames(Project project) {
		Environment env = getEnv(project.getId().toString());
		Store store = getStore(env, DEFAULT_STORE);
		return env.computeInTransaction(new TransactionalComputable<Collection<String>>() {
			
			@SuppressWarnings("unchecked")
			@Override
			public Collection<String> compute(Transaction txn) {
				byte[] bytes = readBytes(store, txn, new StringByteIterable(VERIFICATION_NAMES_KEY));
				if (bytes != null)
					return ((Map<String, Date>) SerializationUtils.deserialize(bytes)).keySet();
				else
					return new HashSet<>();
			}
			
		});
	}

}
