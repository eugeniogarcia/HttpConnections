package com.swisscom.heroes;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oce.mise.rest")
public class RestTemplateProperties {

	private static final int DEFAULT_TIMEOUT = 10000;

	/**
	 * The connect timeout to be used for all rest service connections.
	 */
	private int connectTimeout = DEFAULT_TIMEOUT;

	/**
	 * The read timeout to be used for all rest service connections.
	 */
	private int readTimeout = DEFAULT_TIMEOUT;


	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(final int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(final int readTimeout) {
		this.readTimeout = readTimeout;
	}
}