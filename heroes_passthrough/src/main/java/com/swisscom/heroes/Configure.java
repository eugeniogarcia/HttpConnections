package com.swisscom.heroes;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.web.client.MetricsRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.swisscom.heroes.ConfiguraPool.HttpHostConfiguration;


@Configuration
public class Configure {

	private static final Logger LOGGER = LoggerFactory.getLogger(Configure.class);

	@Bean
	public RestTemplateProperties restTemplateProperties() {
		return new RestTemplateProperties();
	}

	@Primary
	@Bean
	public RestTemplateBuilder oceRestTemplateBuilder(final MetricsRestTemplateCustomizer rtc, final RestTemplateProperties properties) {
		return new RestTemplateBuilder()
				.setConnectTimeout(properties.getConnectTimeout())
				.setReadTimeout(properties.getReadTimeout())

				.customizers(rtc);
	}

	//Opciones

	//xxxxxxxxxxxxxxxxxxxxxxxx
	//1. Si no incluimos la dependencia httpclient, por defecto el RestTemplate usara usa un httpclient sin pool de conexiones
	//xxxxxxxxxxxxxxxxxxxxxxxx

	//xxxxxxxxxxxxxxxxxxxxxxxx
	//2. Si incluimos la dependencia httpclient, por defecto spring tratara de usar la "mejor" connection factory para el httpclient
	// (esto se puede controlar, por ejemplo con el RestTemplateBuilder y el metodo .detectRequestFactory(false/true)
	//xxxxxxxxxxxxxxxxxxxxxxxx

	/*
	@Primary
	@Bean
	public RestTemplate oceRestTemplate(final RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.build();
	}
	 */	

	//xxxxxxxxxxxxxxxxxxxxxxxx
	//3. Podemos querer personalizar la httpclient connection factory para definir el numero de conexiones del pool, o las
	//conexiones por ruta
	//xxxxxxxxxxxxxxxxxxxxxxxx

	/*
	@Primary
	@Bean
	public RestTemplate oceRestTemplate(final RestTemplateBuilder restTemplateBuilder,final RestTemplateProperties properties) {
		final RestTemplate rt=restTemplateBuilder.build();

		final PoolingHttpClientConnectionManager poolingConnectionManager = new PoolingHttpClientConnectionManager();
		//Numero maximo de conexiones en el pool, independientemente de la ruta
		poolingConnectionManager.setMaxTotal(20);
		//Numero maximo de conexiones por ruta
		poolingConnectionManager.setDefaultMaxPerRoute(20);

		final CloseableHttpClient client = HttpClientBuilder.create().setConnectionManager(poolingConnectionManager).build();

		final HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(client);
		clientHttpRequestFactory.setConnectTimeout(properties.getConnectTimeout());
		clientHttpRequestFactory.setReadTimeout(properties.getReadTimeout());
		rt.setRequestFactory(clientHttpRequestFactory);
		return rt;
	}
	 */

	//xxxxxxxxxxxxxxxxxxxxxxxx
	//4. Similar al #3, solo que especificamos maximo de rutas especificas. Parece muy diferente pero es porque 
	//he creado mas helper clases, pero en el fondo es lo mismo
	//xxxxxxxxxxxxxxxxxxxxxxxx

	/*
	@Autowired
	private ConfiguraPool httpHostConfiguration;

	@Bean
	public PoolingHttpClientConnectionManager poolingHttpClientConnectionManager() {
		final PoolingHttpClientConnectionManager result = new PoolingHttpClientConnectionManager();
		result.setMaxTotal(this.httpHostConfiguration.getMaxTotal());
		// Default max per route is used in case it's not set for a specific route
		result.setDefaultMaxPerRoute(this.httpHostConfiguration.getDefaultMaxPerRoute());
		// and / or
		if (CollectionUtils.isNotEmpty(this.httpHostConfiguration.getMaxPerRoutes())) {
			for (final HttpHostConfiguration httpHostConfig : this.httpHostConfiguration.getMaxPerRoutes()) {
				final HttpHost host = new HttpHost(httpHostConfig.getHost(), httpHostConfig.getPort(), httpHostConfig.getScheme());
				// Max per route for a specific host route
				result.setMaxPerRoute(new HttpRoute(host), httpHostConfig.getMaxPerRoute());
			}
		}
		return result;
	}

	@Bean
	public RequestConfig requestConfig(final RestTemplateProperties properties) {
		final RequestConfig result = RequestConfig.custom()
				.setConnectionRequestTimeout(properties.getConnectTimeout())
				.setConnectTimeout(properties.getConnectTimeout())
				.setSocketTimeout(properties.getConnectTimeout())
				.build();
		return result;
	}

	@Bean
	public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager poolingHttpClientConnectionManager, RequestConfig requestConfig) {
		final CloseableHttpClient result = HttpClientBuilder
				.create()
				.setConnectionManager(poolingHttpClientConnectionManager)
				.setDefaultRequestConfig(requestConfig)
				.build();
		return result;
	}

	@Primary
	@Bean
	public RestTemplate oceRestTemplate(final RestTemplateBuilder restTemplateBuilder,final HttpClient httpClient) {
		final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(httpClient);
		final RestTemplate rt=restTemplateBuilder.build();
		rt.setRequestFactory(requestFactory);
		return rt;
	}
	 */

	//xxxxxxxxxxxxxxxxxxxxxxxx
	//5. Sobre el anterior, añadimos logica para gestionar las conexiones
	//xxxxxxxxxxxxxxxxxxxxxxxx

	@Autowired
	private ConfiguraPool httpHostConfiguration;

	@Bean
	public PoolingHttpClientConnectionManager poolingHttpClientConnectionManager() {

		/**
		 * - Supports both HTTP and HTTPS
		 * - Uses a connection pool to re-use connections and save overhead of creating connections.
		 * - Has a custom connection keep-alive strategy (to apply a default keep-alive if one isn't specified)
		 * - Starts an idle connection monitor to continuously clean up stale connections.
		 */
		final SSLContextBuilder builder = new SSLContextBuilder();
		try {
			builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
		} catch (NoSuchAlgorithmException | KeyStoreException e) {
			LOGGER.error("Pooling Connection Manager Initialisation failure because of " + e.getMessage(), e);
		}

		SSLConnectionSocketFactory sslsf = null;
		try {
			sslsf = new SSLConnectionSocketFactory(builder.build());
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			LOGGER.error("Pooling Connection Manager Initialisation failure because of " + e.getMessage(), e);
		}

		final Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
				.<ConnectionSocketFactory>create().register("https", sslsf)
				.register("http", new PlainConnectionSocketFactory())
				.build();

		//Ahora crea el client connection manager con el contexto anterior
		final PoolingHttpClientConnectionManager result = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
		result.setMaxTotal(this.httpHostConfiguration.getMaxTotal());
		// Default max per route is used in case it's not set for a specific route
		result.setDefaultMaxPerRoute(this.httpHostConfiguration.getDefaultMaxPerRoute());
		// and / or
		if (CollectionUtils.isNotEmpty(this.httpHostConfiguration.getMaxPerRoutes())) {
			for (final HttpHostConfiguration httpHostConfig : this.httpHostConfiguration.getMaxPerRoutes()) {
				final HttpHost host = new HttpHost(httpHostConfig.getHost(), httpHostConfig.getPort(), httpHostConfig.getScheme());
				// Max per route for a specific host route
				result.setMaxPerRoute(new HttpRoute(host), httpHostConfig.getMaxPerRoute());
			}
		}
		return result;
	}

	//Gestion de las conexiones. Determina cuanto tiempo tienen que mantenerse activas
	@Bean
	public ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
		return new ConnectionKeepAliveStrategy() {
			@Override
			public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
				final HeaderElementIterator it = new BasicHeaderElementIterator
						(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
				while (it.hasNext()) {
					final HeaderElement he = it.nextElement();
					final String param = he.getName();
					final String value = he.getValue();

					if (value != null && param.equalsIgnoreCase("timeout")) {
						return Long.parseLong(value) * 1000;
					}
				}
				return httpHostConfiguration.getDefault_keep_alive_time();
			}
		};
	}


	@Bean
	public RequestConfig requestConfig(final RestTemplateProperties properties) {
		final RequestConfig result = RequestConfig.custom()
				.setConnectionRequestTimeout(properties.getConnectTimeout())
				.setConnectTimeout(properties.getConnectTimeout())
				.setSocketTimeout(properties.getConnectTimeout())
				.build();
		return result;
	}

	@Bean
	public CloseableHttpClient httpClient(final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager, final RequestConfig requestConfig,final ConnectionKeepAliveStrategy connectionKeepAliveStrategy) {
		final CloseableHttpClient result = HttpClientBuilder
				.create()
				.setConnectionManager(poolingHttpClientConnectionManager)
				.setDefaultRequestConfig(requestConfig)
				//Especifica la estrategia para mantener las conexiones activas
				.setKeepAliveStrategy(connectionKeepAliveStrategy)
				.build();
		return result;
	}

	@Primary
	@Bean
	public RestTemplate oceRestTemplate(final RestTemplateBuilder restTemplateBuilder,final HttpClient httpClient) {
		final HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(httpClient);
		final RestTemplate rt=restTemplateBuilder.build();
		rt.setRequestFactory(requestFactory);
		return rt;
	}

}
