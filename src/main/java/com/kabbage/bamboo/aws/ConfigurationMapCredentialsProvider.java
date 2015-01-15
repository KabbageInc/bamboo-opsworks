package com.kabbage.bamboo.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.util.StringUtils;
import com.atlassian.bamboo.configuration.ConfigurationMap;

public class ConfigurationMapCredentialsProvider implements
		AWSCredentialsProvider {

	private ConfigurationMap configuration;
	
	public ConfigurationMapCredentialsProvider(ConfigurationMap configuration) {
		this.configuration = configuration;
	}
	
	@Override
	public AWSCredentials getCredentials() {
		// if profile is specified, use that
		final String profile = configuration.get(AWSConstants.PROFILE);
		if(!StringUtils.isNullOrEmpty(profile)) {
			return new ProfileCredentialsProvider(profile).getCredentials();
		}

		// then try access key and secret
		final String accessKeyId = configuration.get(AWSConstants.ACCESS_KEY_ID);
		final String secretAccessKey = configuration.get(AWSConstants.SECRET_ACCESS_KEY);
		if(!StringUtils.isNullOrEmpty(accessKeyId) && !StringUtils.isNullOrEmpty(secretAccessKey)) {
			return new BasicAWSCredentials(accessKeyId, secretAccessKey);
		}

		// fall back to default
		return new DefaultAWSCredentialsProviderChain().getCredentials();
	}

	@Override
	public void refresh() {
		return;
	}

}
