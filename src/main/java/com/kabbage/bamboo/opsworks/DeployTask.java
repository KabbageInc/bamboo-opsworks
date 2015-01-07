package com.kabbage.bamboo.opsworks;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.opsworks.AWSOpsWorks;
import com.amazonaws.services.opsworks.AWSOpsWorksClient;
import com.amazonaws.services.opsworks.model.CreateDeploymentRequest;
import com.amazonaws.services.opsworks.model.CreateDeploymentResult;
import com.amazonaws.services.opsworks.model.Deployment;
import com.amazonaws.services.opsworks.model.DeploymentCommand;
import com.amazonaws.services.opsworks.model.DeploymentCommandName;
import com.amazonaws.services.opsworks.model.DescribeDeploymentsRequest;
import com.amazonaws.services.opsworks.model.DescribeDeploymentsResult;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.configuration.ConfigurationMap;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;

public class DeployTask implements TaskType
{
	@Override
	public TaskResult execute(final TaskContext taskContext) throws TaskException
	{
		final BuildLogger buildLogger = taskContext.getBuildLogger();
		final ConfigurationMap configuration = taskContext.getConfigurationMap();

		final AWSOpsWorks opsworks = new AWSOpsWorksClient(getCredentials(configuration));

		String deploymentId;
		try {
			CreateDeploymentRequest request = buildCreateDeploymentRequest(configuration);
			final CreateDeploymentResult result = opsworks.createDeployment(request);
			deploymentId = result.getDeploymentId();
			buildLogger.addBuildLogEntry("Successfully created deployment with ID " + deploymentId);
		} catch(Exception ex) {
			buildLogger.addErrorLogEntry("Error creating OpsWorks deployment", ex);
			return TaskResultBuilder.newBuilder(taskContext).failedWithError().build();
		}
		
		try {
			buildLogger.addBuildLogEntry("Polling deployment status...");
			DescribeDeploymentsRequest request = new DescribeDeploymentsRequest()
				.withDeploymentIds(deploymentId);
			
			boolean deploymentComplete = false;
			boolean deploymentSuccess = true;
			while(!deploymentComplete) {
				final DescribeDeploymentsResult result = opsworks.describeDeployments(request);
				deploymentComplete = true;
				for (Deployment deployment : result.getDeployments()) {
					final String status = deployment.getStatus();
					if(status.equals("running")) {
						deploymentComplete = false;
					} else if(status.equals("failed")) {
						deploymentSuccess = false;
					}
				}
				
				if(!deploymentComplete) {
					buildLogger.addBuildLogEntry("Deployment still in progress...");
					Thread.sleep(10 * 1000);	
				}
			}
			
			if(!deploymentSuccess) {
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

	private AWSCredentials getCredentials(ConfigurationMap configuration) {
		// if profile is specified, use that
		final String profile = configuration.get(DeployTaskConstants.PROFILE);
		if(!isNullOrEmpty(profile)) {
			return new ProfileCredentialsProvider(profile).getCredentials();
		}

		// then try access key and secret
		final String accessKeyId = configuration.get(DeployTaskConstants.ACCESS_KEY_ID);
		final String secretAccessKey = configuration.get(DeployTaskConstants.SECRET_ACCESS_KEY);
		if(!isNullOrEmpty(accessKeyId) && !isNullOrEmpty(secretAccessKey)) {
			return new BasicAWSCredentials(accessKeyId, secretAccessKey);
		}

		// fall back to default
		return new DefaultAWSCredentialsProviderChain().getCredentials();
	}

	private CreateDeploymentRequest buildCreateDeploymentRequest(ConfigurationMap configuration) {
		final String stackId = configuration.get(DeployTaskConstants.STACK_ID);
		final String appId = configuration.get(DeployTaskConstants.APP_ID);
		final DeploymentCommand command = new DeploymentCommand()
			.withName(DeploymentCommandName.Deploy);

		//TODO: Support custom JSON
		//TODO: Support instance Ids
		return new CreateDeploymentRequest()
			.withStackId(stackId)
			.withAppId(appId)
			.withCommand(command);
	}

	private boolean isNullOrEmpty(String str) {
		return str == null || str.isEmpty();
	}
}
