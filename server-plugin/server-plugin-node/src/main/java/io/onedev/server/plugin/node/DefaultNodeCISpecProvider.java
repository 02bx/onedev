package io.onedev.server.plugin.node;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.onedev.server.ci.CISpec;
import io.onedev.server.ci.DefaultCISpecProvider;
import io.onedev.server.ci.job.CacheSpec;
import io.onedev.server.ci.job.Job;
import io.onedev.server.ci.job.trigger.BranchUpdateTrigger;
import io.onedev.server.git.Blob;
import io.onedev.server.git.BlobIdent;
import io.onedev.server.model.Project;

public class DefaultNodeCISpecProvider implements DefaultCISpecProvider {

	@Override
	public CISpec getDefaultCISpec(Project project, ObjectId commitId){
		Blob blob = project.getBlob(new BlobIdent(commitId.name(), "package.json", FileMode.TYPE_FILE), false);

		if (blob != null) {
			String content = null;
			String version = null;

			content = blob.getText().getContent();
			ObjectMapper objectMapper = new ObjectMapper();
			try {
				JsonNode jsonNode = objectMapper.readTree(content);
			
			CISpec ciSpec = new CISpec();

			Job job = new Job();

			job.setName("ci");

			job.setEnvironment("node:10.16-alpine");
			
			if (content.indexOf("angular/core") != -1) {	//Recognize angular projects
				
				job.setEnvironment("1dev/node:10.16-alpine-chrome");
				
				version = jsonNode.findValue("version").asText();
				
				String Commands = "" 
						+ "buildVersion=" + version +  " \n"
						+ "echo \"##onedev[SetBuildVersion '$buildVersion']\"\n" 
						+ "echo\n" 
						+ "npm install \n"
						+ "npm install @angular/cli \n";
			
			if (jsonNode.has("scripts")) {
				JsonNode jsonScripts = jsonNode.get("scripts");
				Iterator<String> iterator = jsonScripts.fieldNames();
				int length = jsonScripts.size();
				String[] valueArray = new String[length];
				int valueIndex = 0;

				while (iterator.hasNext()) {
					String key = (String) iterator.next();
					if (key.indexOf("lint") != -1 || key.indexOf("build") != -1) {
						String value = jsonScripts.findValue(key).asText();
						valueArray[valueIndex] = value;
						valueIndex++;
					}else if(key.indexOf("test") != -1) {
						String value = jsonScripts.findValue(key).asText();
						valueArray[valueIndex] = value + " --watch=false --browsers=ChromeHeadless";
						valueIndex++;
					}
				}

				if (valueArray[0] != null) {
					for (int i = 0; i < valueIndex; i++) {
						Commands = Commands + "npx " + valueArray[i] + " \n";
					}
				}else {
					Commands = Commands
							+ "npx ng lint \n"
							+ "npx ng test --watch=false --browsers=ChromeHeadless \n" 
							+ "npx ng build \n";
				}
			} else {
				Commands = Commands
						+ "npx ng lint \n"
						+ "npx ng test --watch=false --browsers=ChromeHeadless \n" 
						+ "npx ng build \n";
			}

			job.setCommands(Commands);

			} else if (content.indexOf("react") != -1) {	//Recognize react projects
				
				version = jsonNode.findValue("version").asText();
				
				String Commands = "" 
							+ "buildVersion=" + version +  " \n"
							+ "echo \"##onedev[SetBuildVersion '$buildVersion']\"\n" 
							+ "echo\n"
							+ "npm install typescript \n" 
							+ "npm install \n" 
							+ "export CI=TRUE \n";
				
				if (jsonNode.has("scripts")) {
					JsonNode jsonScripts = jsonNode.get("scripts");
					Iterator<String> iterator = jsonScripts.fieldNames();
					int length = jsonScripts.size();
					String[] valueArray = new String[length];
					int valueIndex = 0;

					while (iterator.hasNext()) {
						String key = (String) iterator.next();
						if (key.indexOf("lint") != -1 || key.indexOf("test") != -1 || key.indexOf("build") != -1) {
							String value = jsonScripts.findValue(key).asText();
							valueArray[valueIndex] = value;
							valueIndex++;
						}
					}

					if (valueArray[0] != null) {
						for (int i = 0; i < valueIndex; i++) {
							Commands = Commands + "npx " + valueArray[i] + " \n";
						}
					}else {
						Commands = Commands
								+ "npx react-scripts test \n"
								+ "npx react-scripts build \n";
					}
				} else {
					Commands = Commands
							+ "npx react-scripts test \n"
							+ "npx react-scripts build \n";
				}

				job.setCommands(Commands);

			} else if (content.indexOf("vue") != -1) {	//Recognize vue projects

				version = jsonNode.findValue("version").asText();
				
				String Commands = "" 
						+ "buildVersion=" + version +  " \n"
						+ "echo \"##onedev[SetBuildVersion '$buildVersion']\"\n" 
						+ "echo\n" 
						+ "npm install \n";
				
				if (jsonNode.has("scripts")) {
					JsonNode jsonScripts = jsonNode.get("scripts");
					Iterator<String> iterator = jsonScripts.fieldNames();
					int length = jsonScripts.size();
					String[] valueArray = new String[length];
					int valueIndex = 0;

					while (iterator.hasNext()) {
						String key = (String) iterator.next();
						if (key.indexOf("lint") != -1 || key.indexOf("test") != -1 || key.indexOf("build") != -1) {
							String value = jsonScripts.findValue(key).asText();
							valueArray[valueIndex] = value;
							valueIndex++;
						}
					}

					if (valueArray[0] != null) {
						for (int i = 0; i < valueIndex; i++) {
							Commands = Commands + "npx " + valueArray[i] + " \n";
						}
					}else {
						Commands = Commands
								+ "npx jest \n";
						}
				} else {
					Commands = Commands
							+ "npx jest \n";
					}

				job.setCommands(Commands);

			} else if (content.indexOf("express") != -1) {	//Recognize express projects

				version = jsonNode.findValue("version").asText();
				
				String Commands = "" 
						+ "buildVersion=" + version +  " \n"
						+ "echo \"##onedev[SetBuildVersion '$buildVersion']\"\n" 
						+ "echo\n" 
						+ "npm install \n";
				
				if (jsonNode.has("scripts")) {
					JsonNode jsonScripts = jsonNode.get("scripts");
					Iterator<String> iterator = jsonScripts.fieldNames();
					int length = jsonScripts.size();
					String[] valueArray = new String[length];
					int valueIndex = 0;

					while (iterator.hasNext()) {
						String key = (String) iterator.next();
						if (key.indexOf("lint") != -1 || key.indexOf("test") != -1 || key.indexOf("build") != -1) {
							String value = jsonScripts.findValue(key).asText();
							valueArray[valueIndex] = value;
							valueIndex++;
						}
					}

					if (valueArray[0] != null) {
						for (int i = 0; i < valueIndex; i++) {
							Commands = Commands + "npx " + valueArray[i] + " \n";
						}
					}else {
						Commands = Commands
								+ "npx mocha \n";
						}
				} else {
					Commands = Commands
							+ "npx mocha \n";
					}
			} else {
				return null;
			}
			// Trigger the job automatically when there is a push to the branch
			BranchUpdateTrigger trigger = new BranchUpdateTrigger();
			job.getTriggers().add(trigger);

			CacheSpec cache = new CacheSpec();
			cache.setKey("node_modules");
			cache.setPath("node_modules");
			job.getCaches().add(cache);

			ciSpec.getJobs().add(job);

			return ciSpec;
			
			
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		} else {
			return null;
		}
	}
	

	@Override
	public int getPriority() {
		return DEFAULT_PRIORITY;
	}

}
