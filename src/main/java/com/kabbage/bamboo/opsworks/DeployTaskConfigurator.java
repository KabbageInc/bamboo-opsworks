package com.kabbage.bamboo.opsworks;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.opensymphony.xwork2.TextProvider;

public class DeployTaskConfigurator extends AbstractTaskConfigurator {

	private TextProvider textProvider;
	private String[] fields = new String[] {
			DeployTaskConstants.STACK_ID,
			DeployTaskConstants.APP_ID,
			DeployTaskConstants.PROFILE,
			DeployTaskConstants.ACCESS_KEY_ID,
			DeployTaskConstants.SECRET_ACCESS_KEY
	};
	
	public void setTextProvider(final TextProvider textProvider)
	{
		this.textProvider = textProvider;
	}

	@Override
	public Map<String, String> generateTaskConfigMap(final ActionParametersMap params, final TaskDefinition previousTaskDefinition) {
		final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);

		for (String field : fields) {
			config.put(field, params.getString(field));
		}
		
		return config;
	}

	@Override
	public void populateContextForCreate(final Map<String, Object> context)
	{
		super.populateContextForCreate(context);
	}

	@Override
	public void populateContextForEdit(final Map<String, Object> context, final TaskDefinition taskDefinition)
	{
		super.populateContextForEdit(context, taskDefinition);
		
		for (String field : fields) {
			context.put(field, taskDefinition.getConfiguration().get(field));
		}
	}

	@Override
	public void populateContextForView(final Map<String, Object> context, final TaskDefinition taskDefinition)
	{
		super.populateContextForView(context, taskDefinition);

		for (String field : fields) {
			context.put(field, taskDefinition.getConfiguration().get(field));
		}
	}

	@Override
	public void validate(final ActionParametersMap params, final ErrorCollection errorCollection)
	{
		super.validate(params, errorCollection);

		if (StringUtils.isEmpty(params.getString(DeployTaskConstants.STACK_ID)))
		{
			errorCollection.addError(DeployTaskConstants.STACK_ID, textProvider.getText("bamboo-opsworks.stack_id.required"));
		}
		if (StringUtils.isEmpty(params.getString(DeployTaskConstants.APP_ID)))
		{
			errorCollection.addError(DeployTaskConstants.APP_ID, textProvider.getText("bamboo-opsworks.app_id.required"));
		}
		
		if(StringUtils.isEmpty(params.getString(DeployTaskConstants.ACCESS_KEY_ID))
				&& !StringUtils.isEmpty(params.getString(DeployTaskConstants.SECRET_ACCESS_KEY))) {
			errorCollection.addError(DeployTaskConstants.ACCESS_KEY_ID, textProvider.getText("bamboo-opsworks.access_key_id.required_with_secret"));
		}
		
		if(StringUtils.isEmpty(params.getString(DeployTaskConstants.SECRET_ACCESS_KEY))
				&& !StringUtils.isEmpty(params.getString(DeployTaskConstants.ACCESS_KEY_ID))) {
			errorCollection.addError(DeployTaskConstants.SECRET_ACCESS_KEY, textProvider.getText("bamboo-opsworks.secret_access_key.required_with_access"));
		}
	}
}
