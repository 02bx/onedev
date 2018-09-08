package io.onedev.server.migration;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.inject.Singleton;

import org.dom4j.Element;

import com.google.common.base.Charsets;

import io.onedev.utils.FileUtils;
import io.onedev.utils.StringUtils;

@Singleton
@SuppressWarnings("unused")
public class DatabaseMigrator {
	
	private void migrate1(File dataDir, Stack<Integer> versions) {
		for (File file: dataDir.listFiles()) {
			if (file.getName().startsWith("CodeComments.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					Element branchRefElement = element.element("branchRef");
					if (branchRefElement != null)
						branchRefElement.detach();
				}
				dom.writeToFile(file, false);
			}
		}	
	}

	private void migrate2(File dataDir, Stack<Integer> versions) {
		for (File file: dataDir.listFiles()) {
			if (file.getName().startsWith("Depots.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					Element gateKeeperElement = element.element("gateKeeper");
					gateKeeperElement.detach();
					element.addElement("gateKeepers");
				}
				dom.writeToFile(file, false);
			}
		}	
	}

	private void migrate3(File dataDir, Stack<Integer> versions) {
		for (File file: dataDir.listFiles()) {
			VersionedDocument dom = VersionedDocument.fromFile(file);
			for (Element element: dom.getRootElement().elements()) {
				String name = element.getName();
				name = StringUtils.replace(name, "com.pmease.commons", "com.gitplex.commons");
				name = StringUtils.replace(name, "com.pmease.gitplex", "com.gitplex.server");
				element.setName(name);
			}
			if (file.getName().startsWith("Configs.xml")) {
				for (Element element: dom.getRootElement().elements()) {
					Element settingElement = element.element("setting");
					if (settingElement != null) {
						String clazz = settingElement.attributeValue("class");
						settingElement.addAttribute("class", StringUtils.replace(clazz, "com.pmease.gitplex", "com.gitplex.server"));
						Element gitConfigElement = settingElement.element("gitConfig");
						if (gitConfigElement != null) {
							clazz = gitConfigElement.attributeValue("class");
							gitConfigElement.addAttribute("class", StringUtils.replace(clazz, "com.pmease.gitplex", "com.gitplex.server"));
						}
					}
				}
			}
			dom.writeToFile(file, false);
		}	
	}
	
	private void migrate4(File dataDir, Stack<Integer> versions) {
		for (File file: dataDir.listFiles()) {
			if (file.getName().startsWith("Accounts.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					Element avatarUploadDateElement = element.element("avatarUploadDate");
					if (avatarUploadDateElement != null)
						avatarUploadDateElement.detach();
				}
				dom.writeToFile(file, false);
			}
		}	
	}
	
	private void migrate5(File dataDir, Stack<Integer> versions) {
		for (File file: dataDir.listFiles()) {
			if (file.getName().startsWith("Configs.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					if (element.elementTextTrim("key").equals("MAIL")) {
						Element settingElement = element.element("setting");
						if (settingElement != null)
							settingElement.addElement("enableSSL").setText("false");
					}
				}
				dom.writeToFile(file, false);
			}
		}	
	}
	
	private void migrate6(File dataDir, Stack<Integer> versions) {
	}
	
	private void migrate7(File dataDir, Stack<Integer> versions) {
		for (File file: dataDir.listFiles()) {
			try {
				String content = FileUtils.readFileToString(file, Charsets.UTF_8);
				content = StringUtils.replace(content, 
						"com.gitplex.commons.hibernate.migration.VersionTable", 
						"com.gitplex.server.model.ModelVersion");
				content = StringUtils.replace(content, 
						"com.gitplex.server.core.entity.support.IntegrationPolicy", 
						"com.gitplex.server.model.support.IntegrationPolicy");
				content = StringUtils.replace(content, 
						"com.gitplex.server.core.entity.PullRequest_-IntegrationStrategy", 
						"com.gitplex.server.model.PullRequest_-IntegrationStrategy");
				content = StringUtils.replace(content, 
						"com.gitplex.server.core.entity.", "com.gitplex.server.model.");
				content = StringUtils.replace(content, 
						"com.gitplex.server.core.setting.SpecifiedGit", "com.gitplex.server.git.config.SpecifiedGit");
				content = StringUtils.replace(content, 
						"com.gitplex.server.core.setting.SystemGit", "com.gitplex.server.git.config.SystemGit");
				content = StringUtils.replace(content, 
						"com.gitplex.server.core.setting.", "com.gitplex.server.model.support.setting.");
				content = StringUtils.replace(content, 
						"com.gitplex.server.core.gatekeeper.", "com.gitplex.server.gatekeeper.");
				FileUtils.writeStringToFile(file, content, Charsets.UTF_8);
				
				if (file.getName().equals("VersionTables.xml")) {
					FileUtils.moveFile(file, new File(file.getParentFile(), "ModelVersions.xml"));
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}	
	}
	
	private void migrateIntegrationStrategy(Element integrationStrategyElement) {
		if (integrationStrategyElement != null) {
			integrationStrategyElement.setName("mergeStrategy");
			switch (integrationStrategyElement.getText()) {
			case "MERGE_ALWAYS":
				integrationStrategyElement.setText("ALWAYS_MERGE");
				break;
			case "MERGE_WITH_SQUASH":
				integrationStrategyElement.setText("SQUASH_MERGE");
				break;
			case "REBASE_SOURCE_ONTO_TARGET":
				integrationStrategyElement.setText("REBASE_MERGE");
				break;
			case "REBASE_TARGET_ONTO_SOURCE":
				integrationStrategyElement.setText("MERGE_IF_NECESSARY");
				break;
			}
		}
	}
	
	private void migrate8(File dataDir, Stack<Integer> versions) {
		for (File file: dataDir.listFiles()) {
			if (file.getName().startsWith("Configs.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					if (element.elementTextTrim("key").equals("SYSTEM")) {
						Element settingElement = element.element("setting");
						settingElement.addElement("curlConfig")
								.addAttribute("class", "com.gitplex.server.git.config.SystemCurl");
					}
				}
				dom.writeToFile(file, false);
			} else if (file.getName().startsWith("Accounts.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					element.element("reviewEffort").detach();
				}
				dom.writeToFile(file, false);
			} else if (file.getName().startsWith("Depots.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					element.element("gateKeepers").detach();
					element.element("integrationPolicies").detach();
					element.addElement("branchProtections");
					element.addElement("tagProtections");
				}
				dom.writeToFile(file, false);
			} else if (file.getName().startsWith("PullRequests.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					Element assigneeElement = element.element("assignee");
					if (assigneeElement != null)
						assigneeElement.detach();
					migrateIntegrationStrategy(element.element("integrationStrategy"));
					Element lastIntegrationPreviewElement = element.element("lastIntegrationPreview");
					if (lastIntegrationPreviewElement != null) {
						lastIntegrationPreviewElement.setName("lastMergePreview");
						Element integratedElement = lastIntegrationPreviewElement.element("integrated");
						if (integratedElement != null)
							integratedElement.setName("merged");
						migrateIntegrationStrategy(lastIntegrationPreviewElement.element("integrationStrategy"));
					}
					Element closeInfoElement = element.element("closeInfo");
					if (closeInfoElement != null) {
						Element closeStatusElement = closeInfoElement.element("closeStatus");
						if (closeStatusElement.getText().equals("INTEGRATED"))
							closeStatusElement.setText("MERGED");
					}
				}
				dom.writeToFile(file, false);
			} else if (file.getName().startsWith("PullRequestReviews.xml") 
					|| file.getName().startsWith("PullRequestReviewInvitations.xml")
					|| file.getName().startsWith("PullRequestStatusChanges.xml")
					|| file.getName().startsWith("PullRequestTasks.xml")
					|| file.getName().startsWith("PullRequestVerifications.xml")
					|| file.getName().startsWith("CodeComments.xml")
					|| file.getName().startsWith("CodeCommentRelations.xml")
					|| file.getName().startsWith("CodeCommentReplys.xml") 
					|| file.getName().startsWith("CodeCommentStatusChanges.xml")) {
				FileUtils.deleteFile(file);
			} else if (file.getName().startsWith("PullRequestUpdates.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					Element mergeCommitHashElement = element.element("mergeCommitHash");
					mergeCommitHashElement.setName("mergeBaseCommitHash");
				}				
				dom.writeToFile(file, false);
			}
		}	
	}
	
	private void migrate9(File dataDir, Stack<Integer> versions) {
		try {
			Map<String, String> accountIdToName = new HashMap<>();
			Set<String> userIds = new HashSet<>();
			for (File file: dataDir.listFiles()) {
				if (file.getName().startsWith("Accounts.xml")) {
					File renamedFile = new File(dataDir, file.getName().replace("Accounts.xml", "Users.xml"));
					FileUtils.moveFile(file, renamedFile);
					String content = FileUtils.readFileToString(renamedFile, Charsets.UTF_8);
					content = StringUtils.replace(content, "com.gitplex.server.model.Account", 
							"com.gitplex.server.model.User");
					VersionedDocument dom = VersionedDocument.fromXML(content);
					for (Element element: dom.getRootElement().elements()) {
						accountIdToName.put(element.elementText("id"), element.elementText("name"));
						if (element.elementTextTrim("organization").equals("true")) {
							element.detach();
						} else {
							userIds.add(element.elementText("id"));
							element.element("organization").detach();
							element.element("defaultPrivilege").detach();
							element.element("noSpaceName").detach();
							if (element.element("noSpaceFullName") != null)
								element.element("noSpaceFullName").detach();
						}
					}
					dom.writeToFile(renamedFile, false);
				}
			}
			
			long lastUserAuthorizationId = 0;
			VersionedDocument userAuthorizationsDom = new VersionedDocument();
			Element userAuthorizationListElement = userAuthorizationsDom.addElement("list");
			
			for (File file: dataDir.listFiles()) {
				if (file.getName().startsWith("Depots.xml")) {
					File renamedFile = new File(dataDir, file.getName().replace("Depots.xml", "Projects.xml"));
					FileUtils.moveFile(file, renamedFile);
					String content = FileUtils.readFileToString(renamedFile, Charsets.UTF_8);
					content = StringUtils.replace(content, "com.gitplex.server.model.Depot", 
							"com.gitplex.server.model.Project");
					VersionedDocument dom = VersionedDocument.fromXML(content);
					for (Element element: dom.getRootElement().elements()) {
						String accountId = element.elementText("account");
						element.element("account").detach();
						String depotName = element.elementText("name");
						element.element("name").setText(accountIdToName.get(accountId) + "." + depotName);
						if (element.element("defaultPrivilege") != null	)
							element.element("defaultPrivilege").detach();
						
						String adminId;
						if (userIds.contains(accountId)) {
							adminId = accountId;
						} else {
							adminId = "1";
						}
						Element userAuthorizationElement = 
								userAuthorizationListElement.addElement("com.gitplex.server.model.UserAuthorization");
						userAuthorizationElement.addAttribute("revision", "0.0");
						userAuthorizationElement.addElement("id").setText(String.valueOf(++lastUserAuthorizationId));
						userAuthorizationElement.addElement("user").setText(adminId);
						userAuthorizationElement.addElement("project").setText(element.elementText("id"));
						userAuthorizationElement.addElement("privilege").setText("ADMIN");
					}
					
					dom.writeToFile(renamedFile, false);
				} else if (file.getName().startsWith("BranchWatchs.xml")) {
					VersionedDocument dom = VersionedDocument.fromFile(file);
					for (Element element: dom.getRootElement().elements()) {
						if (!userIds.contains(element.elementText("user"))) {
							element.detach();
						} else {
							element.element("depot").setName("project");
						}
					}
					dom.writeToFile(file, false);
				} else if (file.getName().startsWith("Teams.xml") 
						|| file.getName().startsWith("TeamMemberships.xml")
						|| file.getName().startsWith("TeamAuthorizations.xml")
						|| file.getName().startsWith("OrganizationMemberships.xml")
						|| file.getName().startsWith("UserAuthorizations.xml")
						|| file.getName().startsWith("PullRequest")
						|| file.getName().startsWith("Review")
						|| file.getName().startsWith("ReviewInvitation")) {
					FileUtils.deleteFile(file);
				} else if (file.getName().startsWith("Configs.xml")) {
					VersionedDocument dom = VersionedDocument.fromFile(file);
					for (Element element: dom.getRootElement().elements()) {
						if (element.elementText("key").equals("SYSTEM")) {
							String storagePath = element.element("setting").elementText("storagePath");
							File storageDir = new File(storagePath);
							File repositoriesDir = new File(storageDir, "repositories");
							if (repositoriesDir.exists()) {
								File projectsDir = new File(storageDir, "projects");
								FileUtils.moveDirectory(repositoriesDir, projectsDir);
								for (File projectDir: projectsDir.listFiles()) {
									File infoDir = new File(projectDir, "info");
									if (infoDir.exists())
										FileUtils.deleteDir(infoDir);
								}
							}
						} else if (element.elementText("key").equals("SECURITY")) {
							element.element("setting").addElement("enableAnonymousAccess").setText("false");
						}
					}		
					dom.writeToFile(file, false);
				}
			}	
			userAuthorizationsDom.writeToFile(new File(dataDir, "UserAuthorizations.xml"), false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private void migrate10(File dataDir, Stack<Integer> versions) {
		for (File file: dataDir.listFiles()) {
			if (file.getName().startsWith("CodeComments.xml") || file.getName().startsWith("CodeCommentReplys.xml") 
					|| file.getName().startsWith("CodeCommentStatusChanges.xml")) {
				FileUtils.deleteFile(file);
			} else if (file.getName().startsWith("Projects.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					for (Element branchProtectionElement: element.element("branchProtections").elements()) {
						Element exprElement = branchProtectionElement.element("reviewAppointmentExpr");
						if (exprElement != null)
							exprElement.setName("reviewRequirementSpec");
						for (Element fileProtectionElement: branchProtectionElement.element("fileProtections").elements()) {
							exprElement = fileProtectionElement.element("reviewAppointmentExpr");
							if (exprElement != null)
								exprElement.setName("reviewRequirementSpec");
						}
					}
				}
				dom.writeToFile(file, false);
			}
		}
		VersionedDocument dom = VersionedDocument.fromFile(new File(dataDir, "Configs.xml"));
		for (Element element: dom.getRootElement().elements()) {
			if (element.elementText("key").equals("SYSTEM")) {
				String storagePath = element.element("setting").elementText("storagePath");
				File codeCommentsFromWeiFeng = new File(storagePath, "CodeComments.xml");
				if (codeCommentsFromWeiFeng.exists()) {
					dom = VersionedDocument.fromFile(codeCommentsFromWeiFeng);
					for (Element commentElement: dom.getRootElement().elements()) {
						commentElement.setName("com.gitplex.server.model.CodeComment");
						commentElement.element("depot").setName("project");
						commentElement.element("resolved").detach();
						commentElement.element("commentPos").setName("markPos");
					}
					dom.writeToFile(new File(dataDir, "CodeComments.xml"), false);
				}
			}
		}		
	}
	
	private void migrate11(File dataDir, Stack<Integer> versions) {
		for (File file: dataDir.listFiles()) {
			if (file.getName().startsWith("Configs.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				long maxId = 0;
				for (Element element: dom.getRootElement().elements()) {
					Long id = Long.parseLong(element.elementTextTrim("id"));
					if (maxId < id)
						maxId = id;
				}
				Element licenseConfigElement = dom.getRootElement().addElement("com.gitplex.server.model.Config");
				licenseConfigElement.addElement("id").setText(String.valueOf(maxId+1));
				licenseConfigElement.addElement("key").setText("LICENSE");
				dom.writeToFile(file, false);
			} 
		}
	}
	
	private void migrate12(File dataDir, Stack<Integer> versions) {
		for (File file: dataDir.listFiles()) {
			if (file.getName().startsWith("Projects.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element projectElement: dom.getRootElement().elements()) {
					for (Element branchProtectionElement: projectElement.element("branchProtections").elements()) {
						branchProtectionElement.addElement("enabled").setText("true");
					}
					for (Element tagProtectionElement: projectElement.element("tagProtections").elements()) {
						tagProtectionElement.addElement("enabled").setText("true");
					}
				}
				dom.writeToFile(file, false);
			} 
		}
	}
	
	private void migrate13(File dataDir, Stack<Integer> versions) {
		for (File file: dataDir.listFiles()) {
			try {
				String content = FileUtils.readFileToString(file, Charsets.UTF_8);
				content = StringUtils.replace(content, "gitplex", "turbodev");
				content = StringUtils.replace(content, "GitPlex", "TurboDev");
				FileUtils.writeFile(file, content, Charsets.UTF_8.name());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	private void migrate14(File dataDir, Stack<Integer> versions) {
		for (File file: dataDir.listFiles()) {
			if (file.getName().startsWith("Projects.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element projectElement: dom.getRootElement().elements()) {
					for (Element branchProtectionElement: projectElement.element("branchProtections").elements()) {
						Element submitterElement = branchProtectionElement.addElement("submitter");
						submitterElement.addAttribute("class", "com.turbodev.server.model.support.submitter.Anyone");
						branchProtectionElement.addElement("noCreation").setText("true");
					}
					for (Element tagProtectionElement: projectElement.element("tagProtections").elements()) {
						tagProtectionElement.detach();
					}
				}
				dom.writeToFile(file, false);
			} 
		}
	}
	
	private void migrate15(File dataDir, Stack<Integer> versions) {
		for (File file: dataDir.listFiles()) {
			try {
				String content = FileUtils.readFileToString(file, Charsets.UTF_8);
				content = StringUtils.replace(content, "com.turbodev", "io.onedev");
				content = StringUtils.replace(content, "com/turbodev", "io/onedev");
				content = StringUtils.replace(content, "turbodev.com", "onedev.io");
				content = StringUtils.replace(content, "turbodev", "onedev");
				content = StringUtils.replace(content, "TurboDev", "OneDev");
				FileUtils.writeFile(file, content, Charsets.UTF_8.name());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	/*
	 * Migrate from 1.0 to 2.0
	 */
	private void migrate16(File dataDir, Stack<Integer> versions) {
		Map<String, Integer> codeCommentReplyCounts = new HashMap<>();
		Map<String, String> userNames = new HashMap<>();
		Map<String, Set<String>> requestCodeComments = new HashMap<>();
		Map<String, Integer> requestCommentCounts = new HashMap<>();
		Set<String> openRequests = new HashSet<>();
		Map<String, String> reviewRequirements = new HashMap<>(); 
		Map<String, String[]> groups= new HashMap<>();
		Map<String, String[]> groupMemberships = new HashMap<>();
		Map<String, String[]> userAuthorizations = new HashMap<>();
		Map<String, String[]> groupAuthorizations = new HashMap<>();
		 
		for (File file: dataDir.listFiles()) {
			if (file.getName().startsWith("Users.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					Element fullNameElement = element.element("fullName");
					if (fullNameElement != null)
						userNames.put(element.elementTextTrim("id"), fullNameElement.getText());
					else
						userNames.put(element.elementTextTrim("id"), element.elementText("name"));
				}				
			} else if (file.getName().startsWith("CodeCommentReplys.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					String commentId = element.elementTextTrim("comment");
					Integer replyCount = codeCommentReplyCounts.get(commentId);
					if (replyCount == null)
						replyCount = 0;
					replyCount++;
					codeCommentReplyCounts.put(commentId, replyCount);
				}				
			} else if (file.getName().startsWith("CodeCommentRelations.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					String commentId = element.elementTextTrim("comment");
					String requestId = element.elementTextTrim("request");
					Set<String> codeComments = requestCodeComments.get(requestId);
					if (codeComments == null) {
						codeComments = new HashSet<>();
						requestCodeComments.put(requestId, codeComments);
					}
					codeComments.add(commentId);
				}				
			} else if (file.getName().startsWith("PullRequestComments.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					String commentId = element.elementTextTrim("request");
					Integer commentCount = requestCommentCounts.get(commentId);
					if (commentCount == null)
						commentCount = 0;
					commentCount++;
					requestCommentCounts.put(commentId, commentCount);
				}				
			} else if (file.getName().startsWith("PullRequests.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					if (element.element("closeInfo") == null) {
						openRequests.add(element.elementTextTrim("id"));
					}
				}				
			} else if (file.getName().startsWith("Groups.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					String id = element.elementTextTrim("id");
					groups.put(id, new String[] {
							element.elementTextTrim("name"), 
							element.elementTextTrim("administrator"), 
							element.elementTextTrim("canCreateProjects")});
				}				
			} else if (file.getName().startsWith("GroupAuthorizations.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					String id = element.elementTextTrim("id");
					groupAuthorizations.put(id, new String[] {
							element.elementTextTrim("project"), 
							element.elementTextTrim("group"), 
							element.elementTextTrim("privilege")});
				}				
			} else if (file.getName().startsWith("UserAuthorizations.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					String id = element.elementTextTrim("id");
					userAuthorizations.put(id, new String[] {
							element.elementTextTrim("project"),
							element.elementTextTrim("user"),
							element.elementTextTrim("privilege")							
					});
				}				
			} else if (file.getName().startsWith("Memberships.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					String id = element.elementTextTrim("id");
					groupMemberships.put(id, new String[] {
							element.elementTextTrim("group"),
							element.elementTextTrim("user")							
					});
				}				
			} else if (file.getName().startsWith("Projects.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					String projectId = element.elementTextTrim("id");
					StringBuilder builder = new StringBuilder();
					for (Element branchProtectionElement: element.element("branchProtections").elements()) {
						Element reviewRequirementSpecElement = branchProtectionElement.element("reviewRequirementSpec");
						if (reviewRequirementSpecElement != null) 
							builder.append(reviewRequirementSpecElement.getText()).append(";");
						
						for (Element fileProtectionElement: branchProtectionElement.element("fileProtections").elements()) {
							reviewRequirementSpecElement = fileProtectionElement.element("reviewRequirementSpec");
							builder.append(reviewRequirementSpecElement.getText()).append(";");
						}
					}
					reviewRequirements.put(projectId, builder.toString());
				}				
			}
		}
		
		for (Map.Entry<String, Set<String>> entry: requestCodeComments.entrySet()) {
			Integer commentCount = requestCommentCounts.get(entry.getKey());
			if (commentCount == null)
				commentCount = 0;
			for (String commentId: entry.getValue()) {
				commentCount++;
				Integer replyCount = codeCommentReplyCounts.get(commentId);
				if (replyCount != null)
					commentCount += replyCount;
			}
			requestCommentCounts.put(entry.getKey(), commentCount);
		}
		
		VersionedDocument requestReviewsDOM = new VersionedDocument();
		Element requestReviewListElement = requestReviewsDOM.addElement("list");
		
		VersionedDocument configurationsDOM = new VersionedDocument();
		Element configurationListElement = configurationsDOM.addElement("list");
		Map<String, Map<String, Long>> projectConfigurations = new HashMap<>();
		long configurationCount = 0;
		
		int reviewCount = 0;
		
		VersionedDocument requestBuildsDOM = new VersionedDocument();
		Element requestBuildListElement = requestBuildsDOM.addElement("list");
		int requestBuildCount = 0;
		
		VersionedDocument teamsDOM = new VersionedDocument();
		Element teamListElement = teamsDOM.addElement("list");
		int teamCount = 0;
		
		VersionedDocument membershipsDOM = new VersionedDocument();
		Element membershipListElement = membershipsDOM.addElement("list");
		int membershipCount = 0;
		
		Map<String, String[]> teams = new HashMap<>();
		Map<String, String[]> teamMemberships = new HashMap<>();
		
		for (Map.Entry<String, String[]> groupEntry: groups.entrySet()) {
			String groupId = groupEntry.getKey();
			String groupName = groupEntry.getValue()[0];
			for (Map.Entry<String, String> reviewRequirementEntry: reviewRequirements.entrySet()) {
				String projectId = reviewRequirementEntry.getKey();
				String privilege = null;
				for (Map.Entry<String, String[]> groupAuthorizationEntry: groupAuthorizations.entrySet()) {
					if (groupAuthorizationEntry.getValue()[0].equals(projectId) 
							&& groupAuthorizationEntry.getValue()[1].equals(groupId)) {
						privilege = groupAuthorizationEntry.getValue()[2];
						break;
					}
				}
				if (privilege == null && reviewRequirementEntry.getValue().contains("group(" + groupName + ")"))
					privilege = "READ";
				if (privilege != null) {
					String teamId = String.valueOf(++teamCount);
					Element teamElement = teamListElement.addElement("io.onedev.server.model.Team");
					teamElement.addAttribute("revision", "0.0");
					teamElement.addElement("id").setText(teamId);
					teamElement.addElement("project").setText(projectId);
					teamElement.addElement("name").setText(groupName);
					if (privilege.equals("READ"))
						teamElement.addElement("privilege").setText("CODE_READ");
					else if (privilege.equals("WRITE"))
						teamElement.addElement("privilege").setText("CODE_WRITE");
					else
						teamElement.addElement("privilege").setText("PROJECT_ADMINISTRATION");

					teams.put(String.valueOf(teamId), new String[] {projectId, groupName});
					
					for (Map.Entry<String, String[]> membershipEntry: groupMemberships.entrySet()) {
						if (membershipEntry.getValue()[0].equals(groupId)) {
							String userId = membershipEntry.getValue()[1];
							Element membershipElement = membershipListElement.addElement("io.onedev.server.model.Membership");
							String membershipId = String.valueOf(++membershipCount);
							membershipElement.addAttribute("revision", "0.0");
							membershipElement.addElement("id").setText(membershipId);
							membershipElement.addElement("team").setText(teamId);
							membershipElement.addElement("user").setText(userId);
							teamMemberships.put(membershipId, new String[] {teamId, userId});
						}
					}
				}
			}
		}
		
		for (Map.Entry<String, String[]> userAuthorizationEntry: userAuthorizations.entrySet()) {
			String projectId = userAuthorizationEntry.getValue()[0];
			String userId = userAuthorizationEntry.getValue()[1];
			String privilege = userAuthorizationEntry.getValue()[2];
			String teamName;
			if (privilege.equals("READ"))
				teamName = "Code readers";
			else if (privilege.equals("WRITE"))
				teamName = "Code writers";
			else
				teamName = "Project administrators";
			String teamId = null;
			for (Map.Entry<String, String[]> teamEntry: teams.entrySet()) {
				if (teamEntry.getValue()[0].equals(projectId) && teamEntry.getValue()[1].equals(teamName)) {
					teamId = teamEntry.getKey();
					break;
				}
			}
			if (teamId == null) {
				teamId = String.valueOf(++teamCount);
				Element teamElement = teamListElement.addElement("io.onedev.server.model.Team");
				teamElement.addAttribute("revision", "0.0");
				teamElement.addElement("id").setText(teamId);
				teamElement.addElement("project").setText(projectId);
				teamElement.addElement("name").setText(teamName);
				if (privilege.equals("READ"))
					teamElement.addElement("privilege").setText("CODE_READ");
				else if (privilege.equals("WRITE"))
					teamElement.addElement("privilege").setText("CODE_WRITE");
				else
					teamElement.addElement("privilege").setText("PROJECT_ADMINISTRATION");
				teams.put(teamId, new String[] {projectId, teamName});
			} 
			
			boolean found = false;
			for (Map.Entry<String, String[]> membershipEntry: teamMemberships.entrySet()) {
				if (membershipEntry.getValue()[0].equals(teamId) && membershipEntry.getValue()[1].equals(userId)) {
					found = true;
					break;
				}
			}
			if (!found) {
				Element membershipElement = membershipListElement.addElement("io.onedev.server.model.Membership");
				membershipElement.addAttribute("revision", "0.0");
				String membershipId = String.valueOf(++membershipCount);
				membershipElement.addElement("id").setText(membershipId);
				membershipElement.addElement("team").setText(teamId);
				membershipElement.addElement("user").setText(userId);
				teamMemberships.put(membershipId, new String[] {teamId, userId});			
			}
		}
		
		for (File file: dataDir.listFiles()) {
			if (file.getName().startsWith("BranchWatches.xml") 
					|| file.getName().startsWith("PullRequestReferences.xml")
					|| file.getName().startsWith("PullRequestStatusChanges.xml")
					|| file.getName().startsWith("PullRequestTasks.xml")
					|| file.getName().startsWith("ReviewInvitations.xml")
					|| file.getName().startsWith("Reviews.xml")
					|| file.getName().startsWith("Groups.xml")
					|| file.getName().startsWith("GroupAuthorizations.xml")
					|| file.getName().startsWith("UserAuthorizations.xml")
					|| file.getName().startsWith("Memberships.xml")) {
				FileUtils.deleteFile(file);
			} else if (file.getName().startsWith("Users.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					String userId = element.elementTextTrim("id");
					boolean canCreateProjects = false;
					boolean administrator = false;
					for (Map.Entry<String, String[]> entry: groupMemberships.entrySet()) {
						if (entry.getValue()[1].equals(userId)) {
							String groupId = entry.getValue()[0];
							if (groups.get(groupId)[1].equals("true"))
								administrator = true;
							if (groups.get(groupId)[2].equals("true")) 
								canCreateProjects = true;
						}
					}
					element.addElement("administrator").setText(String.valueOf(administrator));
					element.addElement("canCreateProjects").setText(String.valueOf(canCreateProjects));
				}
				dom.writeToFile(file, false);
			} else if (file.getName().startsWith("CodeComments.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					Integer replyCount = codeCommentReplyCounts.get(element.elementTextTrim("id"));
					if (replyCount == null)
						replyCount = 0;
					element.addElement("replyCount").setText(String.valueOf(replyCount));
					
					Element lastEventElement = element.element("lastEvent");
					if (lastEventElement != null) {
						lastEventElement.setName("lastActivity");
						lastEventElement.element("type").setName("description");
						Element userElement = lastEventElement.element("user");
						Element userNameElement = lastEventElement.element("userName");
						if (userNameElement != null) {
							if (userElement != null)
								userElement.detach();
						} else {
							if (userElement != null) {
								userElement.setName("userName");
								userElement.setText(userNames.get(userElement.getTextTrim()));
							} else {
								lastEventElement.addElement("userName").setText("unknown");
							}
						}
					} else {
						Element lastActivityElement = element.addElement("lastActivity");
						lastActivityElement.addElement("description").setText("created");
						Element dateElement = lastActivityElement.addElement("date");
						dateElement.addAttribute("class", "sql-timestamp");
						dateElement.setText(element.elementText("date"));
						Element userElement = element.element("user");
						if (userElement != null)
							lastActivityElement.addElement("userName").setText(userNames.get(userElement.getTextTrim()));
						else
							lastActivityElement.addElement("userName").setText(element.elementText("userName"));
					}
				}				
				dom.writeToFile(file, false);
			} else if (file.getName().startsWith("PullRequests.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					Integer commentCount = requestCommentCounts.get(element.elementTextTrim("id"));
					if (commentCount == null)
						commentCount = 0;
					element.addElement("commentCount").setText(String.valueOf(commentCount));
					
					Element lastCodeCommentEventDateElement = element.element("lastCodeCommentEventDate");
					if (lastCodeCommentEventDateElement != null)
						lastCodeCommentEventDateElement.setName("lastCodeCommentActivityDate");
					
					Element closeInfoElement = element.element("closeInfo");
					if (closeInfoElement != null) {
						Element closedByElement = closeInfoElement.element("closedBy");
						if (closedByElement != null)
							closedByElement.setName("user");
						Element closedByNameElement = closeInfoElement.element("closedByName");
						if (closedByNameElement != null)
							closedByNameElement.setName("userName");
						closeInfoElement.element("closeDate").setName("date");
						closeInfoElement.element("closeStatus").setName("status");
					}
					Element lastEventElement = element.element("lastEvent");
					if (lastEventElement != null) {
						lastEventElement.setName("lastActivity");
						lastEventElement.element("type").setName("description");
						Element userElement = lastEventElement.element("user");
						Element userNameElement = lastEventElement.element("userName");
						if (userNameElement != null) {
							if (userElement != null)
								userElement.detach();
						} else {
							if (userElement != null) {
								userElement.setName("userName");
								userElement.setText(userNames.get(userElement.getTextTrim()));
							} else {
								lastEventElement.addElement("userName").setText("unknown");
							}
						}
					} else {
						Element lastActivityElement = element.addElement("lastActivity");
						lastActivityElement.addElement("description").setText("submitted");
						Element dateElement = lastActivityElement.addElement("date");
						dateElement.addAttribute("class", "sql-timestamp");
						dateElement.setText(element.elementText("submitDate"));
						Element submitterElement = element.element("submitter");
						if (submitterElement != null)
							lastActivityElement.addElement("userName").setText(userNames.get(submitterElement.getTextTrim()));
						else
							lastActivityElement.addElement("userName").setText(element.elementText("submitterName"));
					}
				}				
				dom.writeToFile(file, false);
			} else if (file.getName().startsWith("Configs.xml")) {
				String content;
				try {
					content = FileUtils.readFileToString(file, Charsets.UTF_8);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				content = StringUtils.replace(content, "io.onedev.server.security.authenticator.", 
						"io.onedev.server.model.support.authenticator.");
				VersionedDocument dom = VersionedDocument.fromXML(content);
				for (Element element: dom.getRootElement().elements()) {
					element.setName("io.onedev.server.model.Setting");
					Element settingElement = element.element("setting");
					if (settingElement != null) {
						settingElement.setName("value");
						if (element.elementTextTrim("key").equals("AUTHENTICATOR")) {
							Element authenticatorElement = settingElement.elementIterator().next();
							settingElement.addAttribute("class", authenticatorElement.getName());
							for (Element fieldElement: authenticatorElement.elements()) {
								if (!fieldElement.getName().equals("defaultGroupNames")) {
									fieldElement.detach();
									settingElement.add(fieldElement);
								}
							}
							authenticatorElement.detach();
							settingElement.addElement("canCreateProjects").setText("true");
						}
					}
				}
				FileUtils.deleteFile(file);
				dom.writeToFile(new File(file.getParentFile(), file.getName().replace("Config", "Setting")), false);
			} else if (file.getName().startsWith("PullRequestWatchs.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					Element reasonElement = element.element("reason");
					if (reasonElement != null)
						reasonElement.detach();
					Element ignoreElement = element.element("ignore");
					ignoreElement.setName("watching");
					ignoreElement.setText(String.valueOf(!Boolean.parseBoolean(ignoreElement.getTextTrim())));
				}
				dom.writeToFile(file, false);
			} else if (file.getName().startsWith("Projects.xml")) {
				VersionedDocument dom = VersionedDocument.fromFile(file);
				for (Element element: dom.getRootElement().elements()) {
					String project = element.elementTextTrim("id");
					Element publicReadElement = element.element("publicRead");
					if (publicReadElement.getTextTrim().equals("true")) 
						element.addElement("defaultPrivilege").setText("CODE_READ");
					publicReadElement.detach();
					
					for (Element branchProtectionElement: element.element("branchProtections").elements()) {
						branchProtectionElement.element("verifyMerges").setName("buildMerges");
						Element verificationsElement = branchProtectionElement.element("verifications");
						verificationsElement.setName("configurations");
						for (Element verificationElement: verificationsElement.elements()) {
							String verification = verificationElement.getText();
							Map<String, Long> configurations = projectConfigurations.get(project);
							if (configurations == null) {
								configurations = new HashMap<>();
								projectConfigurations.put(project, configurations);
							}
							Long configurationId = configurations.get(verification);
							if (configurationId == null) {
								configurationId = ++configurationCount;
								configurations.put(verification, configurationId);
								Element configurationElement = configurationListElement.addElement("io.onedev.server.model.Configuration");
								configurationElement.addAttribute("revision", "0.0");
								configurationElement.addElement("id").setText(String.valueOf(configurationId));
								configurationElement.addElement("project").setText(project);
								configurationElement.addElement("name").setText(verification);
							}
							for (String request: openRequests) {
								Element requestBuildElement = requestBuildListElement.addElement("io.onedev.server.model.PullRequestBuild");
								requestBuildElement.addAttribute("revision", "0.0");
								requestBuildElement.addElement("id").setText(String.valueOf(++requestBuildCount));
								requestBuildElement.addElement("request").setText(request);
								requestBuildElement.addElement("configuration").setText(String.valueOf(configurationId));
							}
						}
						Element submitterElement = branchProtectionElement.element("submitter");
						String submitterClass = submitterElement.attributeValue("class");
						submitterClass = submitterClass.replace("io.onedev.server.model.support.submitter.", 
								"io.onedev.server.model.support.usermatcher.");
						submitterClass = submitterClass.replace("SpecifiedGroup", "SpecifiedTeam");
						submitterElement.attribute("class").setValue(submitterClass);
						Element groupNameElement = submitterElement.element("groupName");
						if (groupNameElement != null)
							groupNameElement.setName("teamName");
						
						Element reviewRequirementSpecElement = branchProtectionElement.element("reviewRequirementSpec");
						if (reviewRequirementSpecElement != null) {
							reviewRequirementSpecElement.setName("reviewRequirement");
							String reviewRequirement = reviewRequirementSpecElement.getText();
							reviewRequirement = reviewRequirement.replace("group(", "team(");
							reviewRequirementSpecElement.setText(reviewRequirement);
						}
						
						for (Element fileProtectionElement: branchProtectionElement.element("fileProtections").elements()) {
							reviewRequirementSpecElement = fileProtectionElement.element("reviewRequirementSpec");
							reviewRequirementSpecElement.setName("reviewRequirement");
							String reviewRequirement = reviewRequirementSpecElement.getText();
							reviewRequirement = reviewRequirement.replace("group(", "team(");
							reviewRequirementSpecElement.setText(reviewRequirement);
						}
					}
					for (Element tagProtectionElement: element.element("tagProtections").elements()) {
						Element submitterElement = tagProtectionElement.element("submitter");
						String submitterClass = submitterElement.attributeValue("class");
						submitterClass = submitterClass.replace("io.onedev.server.model.support.submitter.", 
								"io.onedev.server.model.support.usermatcher.");
						submitterClass = submitterClass.replace("SpecifiedGroup", "SpecifiedTeam");
						submitterElement.attribute("class").setValue(submitterClass);
						Element groupNameElement = submitterElement.element("groupName");
						if (groupNameElement != null)
							groupNameElement.setName("teamName");
					}
				}				
				dom.writeToFile(file, false);
			}
		}

		requestReviewsDOM.writeToFile(new File(dataDir, "PullRequestReviews.xml"), false);
		configurationsDOM.writeToFile(new File(dataDir, "Configurations.xml"), false);
		requestBuildsDOM.writeToFile(new File(dataDir, "PullRequestBuilds.xml"), false);
		teamsDOM.writeToFile(new File(dataDir, "Teams.xml"), false);
		membershipsDOM.writeToFile(new File(dataDir, "Memberships.xml"), false);
	}
}
