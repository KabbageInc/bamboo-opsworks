package com.kabbage.bamboo.opsworks;

import com.amazonaws.services.opsworks.AWSOpsWorks;
import com.amazonaws.services.opsworks.model.App;
import com.amazonaws.services.opsworks.model.CreateDeploymentRequest;
import com.amazonaws.services.opsworks.model.CreateDeploymentResult;
import com.amazonaws.services.opsworks.model.Deployment;
import com.amazonaws.services.opsworks.model.DeploymentCommand;
import com.amazonaws.services.opsworks.model.DeploymentCommandName;
import com.amazonaws.services.opsworks.model.DescribeAppsRequest;
import com.amazonaws.services.opsworks.model.DescribeAppsResult;
import com.amazonaws.services.opsworks.model.DescribeDeploymentsRequest;
import com.amazonaws.services.opsworks.model.DescribeDeploymentsResult;
import com.amazonaws.services.opsworks.model.DescribeStacksRequest;
import com.amazonaws.services.opsworks.model.DescribeStacksResult;
import com.amazonaws.services.opsworks.model.Stack;

public class OpsWorksService {
	private AWSOpsWorks opsWorks;
	
	public OpsWorksService(AWSOpsWorks opsWorks) {
		this.opsWorks = opsWorks;
	}
	
	public String CreateDeployment(final String stackName, final String appName) throws Exception {
		final String stackId = GetStackId(stackName);
		final String appId = GetAppId(appName, stackId);
		final DeploymentCommand command = new DeploymentCommand()
			.withName(DeploymentCommandName.Deploy);
		
		final CreateDeploymentRequest request = new CreateDeploymentRequest()
			.withStackId(stackId)
			.withAppId(appId)
			.withCommand(command);
		
		final CreateDeploymentResult result = opsWorks.createDeployment(request);
		
		return result.getDeploymentId();
	}
	
	public boolean IsDeploymentComplete(String deploymentId) {
		return IsDeploymentOfStatus(deploymentId, "running");
	}
	
	public boolean IsDeploymentSuccessful(String deploymentId) {
		return IsDeploymentOfStatus(deploymentId, "successful");
	}
	
	public String GetStackId(final String stackName) throws Exception {
		final DescribeStacksRequest request = new DescribeStacksRequest();
		
		final DescribeStacksResult result = opsWorks.describeStacks(request);
		
		for (Stack stack : result.getStacks()) {
			if(stack.getName().equals(stackName)) {
				return stack.getStackId();
			}
		}
		
		throw new Exception("Unable to find stack with the name " + stackName);
	}
	
	public String GetAppId(final String appName, final String stackId) throws Exception {
		final DescribeAppsRequest request = new DescribeAppsRequest()
			.withStackId(stackId);
		
		final DescribeAppsResult result = opsWorks.describeApps(request);
		
		for (App app : result.getApps()) {
			if(app.getName().equals(appName)) {
				return app.getAppId();
			}
		}
		
		throw new Exception("Unable to find app with the name " + appName);
	}
	
	private boolean IsDeploymentOfStatus(String deploymentId, String status) {
		DescribeDeploymentsRequest request = new DescribeDeploymentsRequest()
			.withDeploymentIds(deploymentId);
		
		final DescribeDeploymentsResult result = opsWorks.describeDeployments(request);
		
		for (Deployment deployment : result.getDeployments()) {
			if(!deployment.getStatus().equals(status)) {
				return false;
			}
		}
		return true;
	}
}
