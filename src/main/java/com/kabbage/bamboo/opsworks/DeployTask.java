package com.kabbage.bamboo.opsworks;

import com.amazonaws.services.opsworks.AWSOpsWorks;
import com.amazonaws.services.opsworks.AWSOpsWorksClient;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.kabbage.bamboo.aws.ConfigurationMapCredentialsProvider;

public class DeployTask implements TaskType
{
	@Override
	public TaskResult execute(final TaskContext taskContext) throws TaskException
	{
		final BuildLogger buildLogger = taskContext.getBuildLogger();
		final ConfigurationMap configuration = taskContext.getConfigurationMap();

		final AWSOpsWorks opsWorks = new AWSOpsWorksClient(new ConfigurationMapCredentialsProvider(configuration));
		final OpsWorksService service = new OpsWorksService(opsWorks);
		
		final String stackName = configuration.get(DeployTaskConstants.STACK_NAME);
		final String appName = configuration.get(DeployTaskConstants.APP_NAME);
		
		String deploymentId;
		try {
			deploymentId = service.CreateDeployment(stackName, appName);
			buildLogger.addBuildLogEntry("Successfully created deployment with ID " + deploymentId);
		} catch(Exception ex) {
			buildLogger.addErrorLogEntry("Error creating OpsWorks deployment", ex);
			return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
		}
		
		try {
			buildLogger.addBuildLogEntry("Polling deployment status...");
			
			while(true) {
				if(service.IsDeploymentComplete(deploymentId)) {
					break;
				}
				
				buildLogger.addBuildLogEntry("Deployment still in progress, will check again in 10 seconds");
				Thread.sleep(10 * 1000);
			}
			
			if(!service.IsDeploymentSuccessful(deploymentId)) {
				//TODO: would be nice to provide more details here
				buildLogger.addErrorLogEntry("The OpsWorks deployment failed");
				return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
			}
		} catch (Exception ex) {
			buildLogger.addErrorLogEntry("Error polling OpsWorks deployment", ex);
			return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
		}
		
		buildLogger.addBuildLogEntry("The OpsWorks deployment completed successfully");
		return TaskResultBuilder.newBuilder(taskContext).success().build();
	}
}
