package com.pmease.commons.util.execution;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.pmease.commons.util.StringUtils;

public class Commandline  {
	
    static final ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

	private static final Logger logger = LoggerFactory.getLogger(Commandline.class);

    private String executable;
    
    private List<String> arguments = new ArrayList<String>();
    
    private File workingDir;
    
    private Map<String, String> environment = new HashMap<String, String>();

    public Commandline(String command) {
        String[] parts = StringUtils.parseQuoteTokens(command);
        Preconditions.checkArgument(parts.length != 0, "Argument 'command' is invalid.");
        
        executable(parts[0]);
        for (int i = 1; i < parts.length; i++) 
            arguments.add(parts[i]);
    }
    
    public Commandline executable(String executable) {
    	Preconditions.checkArgument(StringUtils.isNotBlank(executable), "Argument 'executable' should not be empty.");
    	
        this.executable = executable.replace('/', File.separatorChar).replace('\\', File.separatorChar);
        
        return this;
    }
    
    public Commandline arguments(List<String> arguments) {
    	this.arguments.clear();
    	this.arguments.addAll(arguments);
    	return this;
    }
    
    public Commandline addArgs(String... args) {
    	for (String each: args)
    		arguments.add(each);
    	return this;
    }
    
    public Commandline workingDir(File workingDir) {
    	this.workingDir = workingDir;
    	return this;
    }
    
    public Commandline environment(Map<String, String> environment) {
    	this.environment.clear();
    	this.environment.putAll(environment);
    	return this;
    }

    public String toString() {
    	List<String> command = new ArrayList<String>();
    	command.add(executable);
    	command.addAll(arguments);

    	StringBuffer buf = new StringBuffer();
        for (String each: command) {
        	if (each.contains(" ") || each.contains("\t")) {
        		buf.append("\"").append(StringUtils.replace(
        				each, "\n", "\\n")).append("\"").append(" ");
        	} else {
        		buf.append(StringUtils.replace(
        				each, "\n", "\\n")).append(" ");
        	}
        }
        return buf.toString();
    }

    public Commandline clear() {
        executable = null;
        arguments.clear();
        environment.clear();
        workingDir = null;
        
        return this;
    }

    public Commandline clearArgs() {
        arguments.clear();
        return this;
    }
    
    public ExecuteResult execute(OutputStream stdoutConsumer, LineConsumer stderrConsumer) {
    	return execute(stdoutConsumer, stderrConsumer, null);
    }
    
	private ProcessBuilder createProcessBuilder() {
		File workingDir = this.workingDir;
		if (workingDir == null)
			workingDir = new File(".");
		
		String executable = this.executable;
		
        if (!new File(executable).isAbsolute()) {
            if (new File(workingDir, executable).isFile())
            	executable = new File(workingDir, executable).getAbsolutePath();
            else if (new File(workingDir, executable + ".exe").isFile())
            	executable = new File(workingDir, executable + ".exe").getAbsolutePath();
            else if (new File(workingDir, executable + ".bat").isFile())
            	executable = new File(workingDir, executable + ".bat").getAbsolutePath();
        }

		List<String> command = new ArrayList<String>();
		command.add(executable);
		command.addAll(arguments);
		
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDir);
        
        processBuilder.environment().putAll(environment);
		
        if (logger.isDebugEnabled()) {
    		logger.debug("Executing command: " + this);
    		logger.debug("Command working directory: " + 
    				processBuilder.directory().getAbsolutePath());
    		StringBuffer buffer = new StringBuffer();
    		for (Map.Entry<String, String> entry: processBuilder.environment().entrySet())
    			buffer.append("	" + entry.getKey() + "=" + entry.getValue() + "\n");
    		logger.trace("Command execution environments:\n" + 
    				StringUtils.stripEnd(buffer.toString(), "\n"));
    	}

    	return processBuilder;
    }
    
	public ExecuteResult execute(OutputStream stdoutConsumer, final LineConsumer stderrConsumer, 
			@Nullable byte[] stdinBytes) {
		
    	Process process;
        try {
        	ProcessBuilder processBuilder = createProcessBuilder();
        	process = processBuilder.redirectErrorStream(stderrConsumer == null).start();
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }

    	ByteArrayInputStream inputStream = null;
    	if (stdinBytes != null && stdinBytes.length != 0) 
    		inputStream = new ByteArrayInputStream(stdinBytes);
    	
    	final StringBuffer errorMessage = new StringBuffer();
		OutputStream errorMessageCollector = null;
		if (stderrConsumer != null) {
			errorMessageCollector = new LineConsumer(stderrConsumer.getEncoding()) {

				@Override
				public void consume(String line) {
					if (errorMessage.length() != 0)
						errorMessage.append("\n");
					errorMessage.append(line);
					stderrConsumer.consume(line);
				}
				
			};
		}
    	
        ProcessStreamPumper streamPumper = ProcessStreamPumper.pump(process, stdoutConsumer, 
        		errorMessageCollector, inputStream);
        
        ExecuteResult result = new ExecuteResult(this);
        
        try {
            result.setReturnCode(process.waitFor());
		} catch (InterruptedException e) {
			process.destroy();
			throw new RuntimeException(e);
		} finally {
			streamPumper.waitFor();
		}

        if (errorMessage.length() != 0)
        	result.setErrorMessage(errorMessage.toString());
        
        return result;
    }
    
	public void executeWithoutWait(@Nullable byte[] stdinBytes) {
    	ByteArrayInputStream stdinStream = null;
    	if (stdinBytes != null && stdinBytes.length != 0) 
    		stdinStream = new ByteArrayInputStream(stdinBytes);
        
        try {
            ProcessStreamPumper.pump(createProcessBuilder().start(), null, null, stdinStream);
        } catch (IOException e) {
        	throw new RuntimeException(e);
        }
    }

}
