package io.onedev.server.ci.job;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.ValidationException;

import org.eclipse.jgit.lib.ObjectId;
import org.quartz.CronScheduleBuilder;
import org.quartz.ScheduleBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import io.onedev.commons.launcher.loader.Listen;
import io.onedev.commons.launcher.loader.ListenerRegistry;
import io.onedev.commons.utils.ExceptionUtils;
import io.onedev.commons.utils.FileUtils;
import io.onedev.commons.utils.LockUtils;
import io.onedev.commons.utils.schedule.SchedulableTask;
import io.onedev.commons.utils.schedule.TaskScheduler;
import io.onedev.server.OneException;
import io.onedev.server.ci.CISpec;
import io.onedev.server.ci.InvalidCISpecException;
import io.onedev.server.ci.JobDependency;
import io.onedev.server.ci.job.log.LogManager;
import io.onedev.server.ci.job.param.JobParam;
import io.onedev.server.ci.job.trigger.JobTrigger;
import io.onedev.server.entitymanager.BuildManager;
import io.onedev.server.entitymanager.BuildParamManager;
import io.onedev.server.entitymanager.ProjectManager;
import io.onedev.server.entitymanager.SettingManager;
import io.onedev.server.entitymanager.UserManager;
import io.onedev.server.event.BuildCommitAware;
import io.onedev.server.event.ProjectEvent;
import io.onedev.server.event.build.BuildFinished;
import io.onedev.server.event.build.BuildQueueing;
import io.onedev.server.event.build.BuildRunning;
import io.onedev.server.event.build.BuildSubmitted;
import io.onedev.server.event.entity.EntityPersisted;
import io.onedev.server.event.system.SystemStarted;
import io.onedev.server.event.system.SystemStopping;
import io.onedev.server.model.Build;
import io.onedev.server.model.BuildDependence;
import io.onedev.server.model.BuildParam;
import io.onedev.server.model.Project;
import io.onedev.server.model.Setting;
import io.onedev.server.model.Setting.Key;
import io.onedev.server.model.User;
import io.onedev.server.model.support.jobexecutor.JobExecutor;
import io.onedev.server.model.support.jobexecutor.SourceSnapshot;
import io.onedev.server.persistence.SessionManager;
import io.onedev.server.persistence.TransactionManager;
import io.onedev.server.persistence.annotation.Sessional;
import io.onedev.server.persistence.annotation.Transactional;
import io.onedev.server.util.Input;
import io.onedev.server.util.MatrixRunner;
import io.onedev.server.util.inputspec.InputSpec;
import io.onedev.server.util.inputspec.SecretInput;
import io.onedev.server.util.patternset.PatternSet;

@Singleton
public class DefaultJobScheduler implements JobScheduler, Runnable, SchedulableTask {

	private static final int CHECK_INTERVAL = 1000; // check internal in milli-seconds
	
	private static final Logger logger = LoggerFactory.getLogger(DefaultJobScheduler.class);
	
	private enum Status {STARTED, STOPPING, STOPPED};
	
	private final Map<Long, JobExecution> jobExecutions = new ConcurrentHashMap<>();
	
	private final ProjectManager projectManager;
	
	private final BuildManager buildManager;
	
	private final UserManager userManager;
	
	private final ListenerRegistry listenerRegistry;
	
	private final TransactionManager transactionManager;
	
	private final SessionManager sessionManager;
	
	private final LogManager logManager;
	
	private final SettingManager settingManager;
	
	private final ExecutorService executorService;
	
	private final Set<DependencyPopulator> dependencyPopulators;
	
	private final TaskScheduler taskScheduler;
	
	private final BuildParamManager buildParamManager;
	
	private volatile List<JobExecutor> jobExecutors;
	
	private String taskId;
	
	private volatile Status status;
	
	@Inject
	public DefaultJobScheduler(ProjectManager projectManager, BuildManager buildManager, 
			UserManager userManager, ListenerRegistry listenerRegistry, SettingManager settingManager,
			TransactionManager transactionManager, LogManager logManager, ExecutorService executorService,
			SessionManager sessionManager, Set<DependencyPopulator> dependencyPopulators, 
			TaskScheduler taskScheduler, BuildParamManager buildParamManager) {
		this.projectManager = projectManager;
		this.settingManager = settingManager;
		this.buildManager = buildManager;
		this.userManager = userManager;
		this.listenerRegistry = listenerRegistry;
		this.transactionManager = transactionManager;
		this.logManager = logManager;
		this.executorService = executorService;
		this.dependencyPopulators = dependencyPopulators;
		this.sessionManager = sessionManager;
		this.taskScheduler = taskScheduler;
		this.buildParamManager = buildParamManager;
	}

	@Sessional
	@Override
	public void submit(Project project, String commitHash, String jobName, Map<String, List<List<String>>> paramMatrix) {
		String lockKey = "job-schedule: " + project.getId() + "-" + commitHash;
		Long projectId = project.getId();
		Long submitterId = User.idOf(userManager.getCurrent());
		transactionManager.runAsyncAfterCommit(new Runnable() {

			@Override
			public void run() {
				LockUtils.call(lockKey, new Callable<Void>() {

					@Override
					public Void call() {
						transactionManager.run(new Runnable() {

							@Override
							public void run() {
								Project project = projectManager.load(projectId);
								User submitter = (submitterId != null? userManager.load(submitterId): null);
								new MatrixRunner<List<String>>(paramMatrix) {
									
									@Override
									public void run(Map<String, List<String>> params) {
										submit(project, submitter, commitHash, jobName, params, new LinkedHashSet<>()); 
									}
									
								}.run();
							}
							
						});
						return null;
					}
					
				});
			}
			
		});
	}
	
	private Map<String, List<List<String>>> getParamMatrix(List<JobParam> params) {
		Map<String, List<List<String>>> paramMatrix = new LinkedHashMap<>();
		for (JobParam param: params) 
			paramMatrix.put(param.getName(), param.getValuesProvider().getValues());
		return paramMatrix;
	}
	
	private List<Build> submit(Project project, @Nullable User user, String commitHash, String jobName, 
			Map<String, List<String>> paramMap, Set<String> checkedJobNames) {
		Build build = new Build();
		build.setProject(project);
		build.setCommitHash(commitHash);
		build.setJobName(jobName);
		build.setSubmitDate(new Date());
		build.setStatus(Build.Status.WAITING);
		build.setSubmitter(user);
		
		try {
			JobParam.validateParams(build.getJob().getParamSpecs(), JobParam.getParamMatrix(paramMap));
		} catch (ValidationException e) {
			markBuildError(build, e.getMessage());
			return Lists.newArrayList(build);
		}
		
		if (!checkedJobNames.add(jobName)) {
			markBuildError(build, "Circular job dependencies found: " + checkedJobNames);
			return Lists.newArrayList(build);
		}

		Map<String, List<String>> copyOfParamMap = new HashMap<>(paramMap);
		for (InputSpec paramSpec: build.getJob().getParamSpecs()) {
			if (paramSpec instanceof SecretInput)
				copyOfParamMap.remove(paramSpec.getName());
		}

		List<Build> builds = buildManager.query(project, commitHash, jobName, copyOfParamMap);
		if (builds.isEmpty()) {
			builds.add(build);
			
			for (Map.Entry<String, List<String>> entry: paramMap.entrySet()) {
				InputSpec paramSpec = Preconditions.checkNotNull(build.getJob().getParamSpecMap().get(entry.getKey()));
				if (!entry.getValue().isEmpty()) {
					for (String string: entry.getValue()) {
						BuildParam param = new BuildParam();
						param.setBuild(build);
						param.setName(entry.getKey());
						param.setType(paramSpec.getType());
						param.setValue(string);
						build.getParams().add(param);
					}
				} else {
					BuildParam param = new BuildParam();
					param.setBuild(build);
					param.setName(entry.getKey());
					param.setType(paramSpec.getType());
					build.getParams().add(param);
				}
			}
			
			for (JobDependency dependency: build.getJob().getDependencies()) {
				new MatrixRunner<List<String>>(getParamMatrix(dependency.getJobParams())) {
					
					@Override
					public void run(Map<String, List<String>> params) {
						List<Build> dependencyBuilds = submit(project, null, commitHash, dependency.getJobName(), 
								params, new LinkedHashSet<>(checkedJobNames));
						for (Build dependencyBuild: dependencyBuilds) {
							BuildDependence dependence = new BuildDependence();
							dependence.setDependency(dependencyBuild);
							dependence.setDependent(build);
							build.getDependencies().add(dependence);
						}
					}
					
				}.run();
			}

			buildManager.create(build);
			listenerRegistry.post(new BuildSubmitted(build));
		}
		return builds;
	}
	
	@Nullable
	private JobExecutor getJobExecutor(Project project, ObjectId commitId, String jobName, String image) {
		for (JobExecutor executor: jobExecutors) {
			if (executor.isApplicable(project, commitId, jobName, image))
				return executor;
		}
		return null;
	}

	private void run(Build build) {
		try {
			Job job = build.getJob();
			ObjectId commitId = ObjectId.fromString(build.getCommitHash());
			JobExecutor executor = getJobExecutor(build.getProject(), commitId, job.getName(), job.getEnvironment());
			if (executor != null) {
				if (executor.hasCapacity()) {
					build.setStatus(Build.Status.RUNNING);
					build.setRunningDate(new Date());
					buildManager.save(build);
					listenerRegistry.post(new BuildRunning(build));
					
					SourceSnapshot snapshot;
					if (job.isCloneSource()) 
						snapshot = new SourceSnapshot(build.getProject(), commitId);
					else 
						snapshot = null;
					
					Logger logger = logManager.getLogger(build, job.getLogLevel()); 
					
					Long buildId = build.getId();
					JobExecution execution = new JobExecution(executorService.submit(new Runnable() {

						@Override
						public void run() {
							logger.info("Creating workspace...");
							File workspace = FileUtils.createTempDir("workspace");
							try {
								Map<String, String> envVars = new HashMap<>();
								Set<String> includeFiles = new HashSet<>();
								Set<String> excludeFiles = new HashSet<>();
								
								sessionManager.run(new Runnable() {

									@Override
									public void run() {
										Build build = buildManager.load(buildId);
										logger.info("Populating dependencies...");
										for (BuildDependence dependence: build.getDependencies()) {
											for (DependencyPopulator populator: dependencyPopulators)
												populator.populate(dependence.getDependency(), workspace, logger);
										}
										envVars.put("ONEDEV_PROJECT", build.getProject().getName());
										envVars.put("ONEDEV_COMMIT", commitId.name());
										envVars.put("ONEDEV_JOB", job.getName());
										envVars.put("ONEDEV_BUILD_NUMBER", String.valueOf(build.getNumber()));
										for (Entry<String, Input> entry: build.getParamInputs().entrySet()) {
											String paramName = entry.getKey();
											if (build.isParamVisible(paramName)) {
												String paramType = entry.getValue().getType();
												List<String> paramValues = entry.getValue().getValues();
												if (paramValues.size() > 1) {
													int index = 1;
													for (String value: paramValues) {
														if (paramType.equals(InputSpec.SECRET)) 
															value = build.getSecretValue(value);
														envVars.put(paramName + "_" + index++, value);
													}
												} else if (paramValues.size() == 1) {
													String value = paramValues.iterator().next();
													if (paramType.equals(InputSpec.SECRET)) 
														value = build.getSecretValue(value);
													envVars.put(paramName, value);
												}
											}
										}
										
										for (JobOutcome outcome: job.getOutcomes()) {
											PatternSet patternSet = PatternSet.fromString(outcome.getFilePatterns());
											includeFiles.addAll(patternSet.getIncludes());
											excludeFiles.addAll(patternSet.getExcludes());
										}
									}
									
								});

								logger.info("Executing job with executor '" + executor.getName() + "'...");
								
								List<String> commands = Splitter.on("\n").trimResults(CharMatcher.is('\r')).splitToList(job.getCommands());
								executor.execute(job.getEnvironment(), workspace, envVars, commands, snapshot, 
										job.getCaches(), new PatternSet(includeFiles, excludeFiles), logger);
								
								sessionManager.run(new Runnable() {

									@Override
									public void run() {
										logger.info("Collecting job outcomes...");
										Build build = buildManager.load(buildId);
										for (JobOutcome outcome: job.getOutcomes())
											outcome.process(build, workspace, logger);
									}
									
								});
							} catch (Exception e) {
								if (ExceptionUtils.find(e, InterruptedException.class) == null)
									logger.error("Error running build", e);
								throw e;
							} finally {
								logger.info("Deleting workspace...");
								executor.cleanDir(workspace);
								FileUtils.deleteDir(workspace);
								logger.info("Workspace deleted");
							}
						}
						
					}), job.getTimeout() * 1000L);
					
					JobExecution prevExecution = jobExecutions.put(build.getId(), execution);
					
					if (prevExecution != null)
						prevExecution.cancel(null);
				}
			} else {
				markBuildError(build, "No applicable job executor");
			}
		} catch (InvalidCISpecException e) {
			markBuildError(build, e.getMessage());
		}
	}
	
	private void markBuildError(Build build, String errorMessage) {
		build.setStatus(Build.Status.IN_ERROR, errorMessage);
		build.setFinishDate(new Date());
		if (build.isNew())
			buildManager.create(build);
		listenerRegistry.post(new BuildFinished(build));
	}
	
	@Sessional
	@Listen
	public void on(ProjectEvent event) {
		if (event instanceof BuildCommitAware) {
			ObjectId commitId = ((BuildCommitAware) event).getBuildCommit();
			if (!commitId.equals(ObjectId.zeroId())) {
				try {
					CISpec ciSpec = event.getProject().getCISpec(commitId);
					if (ciSpec != null) {
						for (Job job: ciSpec.getJobs()) {
							JobTrigger trigger = job.getMatchedTrigger(event);
							if (trigger != null)
								submit(event.getProject(), commitId.name(), job.getName(), getParamMatrix(trigger.getParams()));
						}
					}
				} catch (Exception e) {
					String message = String.format("Error checking job triggers (project: %s, commit: %s)", 
							event.getProject().getName(), commitId.name());
					logger.error(message, e);
				}
			}
		}
	}
	
	@Transactional
	@Override
	public void resubmit(Build build, Map<String, List<String>> paramMap) {
		if (build.isFinished()) {
			build.setStatus(Build.Status.WAITING);
			build.setFinishDate(null);
			build.setQueueingDate(null);
			build.setRunningDate(null);
			build.setSubmitDate(new Date());
			build.setSubmitter(userManager.getCurrent());
			buildParamManager.deleteParams(build);
			for (Map.Entry<String, List<String>> entry: paramMap.entrySet()) {
				InputSpec paramSpec = build.getJob().getParamSpecMap().get(entry.getKey());
				Preconditions.checkNotNull(paramSpec);
				String type = paramSpec.getType();
				List<String> values = entry.getValue();
				if (!values.isEmpty()) {
					for (String value: values) {
						BuildParam param = new BuildParam();
						param.setBuild(build);
						param.setName(entry.getKey());
						param.setType(type);
						param.setValue(value);
						build.getParams().add(param);
						buildParamManager.save(param);
					}
				} else {
					BuildParam param = new BuildParam();
					param.setBuild(build);
					param.setName(paramSpec.getName());
					param.setType(type);
					build.getParams().add(param);
					buildParamManager.save(param);
				}
			}
			buildManager.save(build);
			listenerRegistry.post(new BuildSubmitted(build));
		} else {
			throw new OneException("Build #" + build.getNumber() + " not finished yet");
		}
	}

	@Sessional
	@Override
	public void cancel(Build build) {
		JobExecution execution = jobExecutions.get(build.getId());
		if (execution != null)
			execution.cancel(User.getCurrentId());
	}
	
	@SuppressWarnings("unchecked")
	@Listen
	public void on(EntityPersisted event) {
		if (event.getEntity() instanceof Setting) {
			Setting setting = (Setting) event.getEntity();
			if (setting.getKey() == Key.JOB_EXECUTORS)
				jobExecutors = (List<JobExecutor>) setting.getValue();
		}
	}
	
	@Listen
	public void on(SystemStarted event) {
		status = Status.STARTED;
		jobExecutors = settingManager.getJobExecutors();
		new Thread(this).start();		
		taskId = taskScheduler.schedule(this);
	}
	
	@Listen
	public void on(SystemStopping event) {
		taskScheduler.unschedule(taskId);
		if (status == Status.STARTED) {
			status = Status.STOPPING;
			while (status == Status.STOPPING) {
				synchronized (this) {
					notify();
				}
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	@Override
	public synchronized void run() {
		while (true) {
			try {
				wait(CHECK_INTERVAL);
			} catch (InterruptedException e) {
			}
			try {
				boolean hasRunnings = transactionManager.call(new Callable<Boolean>() {
	
					@Override
					public Boolean call() {
						for (Build build: buildManager.queryUnfinished()) {
							if (build.getStatus() == Build.Status.QUEUEING) {
								if (status == Status.STARTED) 
									run(build);									
							} else if (build.getStatus() == Build.Status.RUNNING) {
								JobExecution execution = jobExecutions.get(build.getId());
								if (execution != null) {
									if (execution.isTimedout())
										execution.cancel(null);
								} else {
									markBuildError(build, "Stopped for unknown reason");
								}
							} else if (build.getStatus() == Build.Status.WAITING) {
								boolean hasUnsuccessful = false;
								boolean hasUnfinished = false;
								
								for (BuildDependence dependence: build.getDependencies()) {
									Build dependency = dependence.getDependency();
									
									if (dependency.getStatus() == Build.Status.SUCCESSFUL)
										continue;
									else if (dependency.isFinished())
										hasUnsuccessful = true;
									else
										hasUnfinished = true;
								}
								
								if (hasUnsuccessful) {
									markBuildError(build, "There are failed dependency jobs");
								} else if (!hasUnfinished) {
									build.setStatus(Build.Status.QUEUEING);
									build.setQueueingDate(new Date());
									listenerRegistry.post(new BuildQueueing(build));
								}
							}
						}
						for (Iterator<Map.Entry<Long, JobExecution>> it = jobExecutions.entrySet().iterator(); it.hasNext();) {
							Map.Entry<Long, JobExecution> entry = it.next();
							Build build = buildManager.get(entry.getKey());
							JobExecution execution = entry.getValue();
							if (build == null || build.getStatus() != Build.Status.RUNNING) {
								it.remove();
								execution.cancel(null);
							} else if (execution.isDone()) {
								it.remove();
								try {
									execution.check();
									build.setStatus(Build.Status.SUCCESSFUL);
								} catch (TimeoutException e) {
									build.setStatus(Build.Status.TIMED_OUT);
								} catch (CancellationException e) {
									if (e instanceof CancellerAwareCancellationException) {
										Long cancellerId = ((CancellerAwareCancellationException) e).getCancellerId();
										if (cancellerId != null)
											build.setCanceller(userManager.load(cancellerId));
									}
									build.setStatus(Build.Status.CANCELLED);
								} catch (Exception e) {
									build.setStatus(Build.Status.FAILED, e.getMessage());
								} finally {
									build.setFinishDate(new Date());
									listenerRegistry.post(new BuildFinished(build));
								}
							}
						}
						return !jobExecutions.isEmpty();
					}
					
				});
				if (!hasRunnings && status == Status.STOPPING)
					break;
			} catch (Exception e) {
				logger.error("Error checking unfinished builds", e);
			}
		}	
		status = Status.STOPPED;
	}
	
	@Listen
	public void on(BuildFinished event) {
		for (BuildParam param: event.getBuild().getParams()) {
			if (param.getType().equals(InputSpec.SECRET)) 
				param.setValue(null);
		}
	}
	
	@Override
	public void execute() {
		for (JobExecutor executor: jobExecutors)
			executor.checkCaches();
	}

	@Override
	public ScheduleBuilder<?> getScheduleBuilder() {
		return CronScheduleBuilder.dailyAtHourAndMinute(0, 0);
	}
	
}
