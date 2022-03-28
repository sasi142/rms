/**
 * https://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html
 * http.conn-manager.timeout - time to get the connection
 * http://dev.bizo.com/2013/04/sensible-defaults-for-apache-httpclient.html
 */

package core.utils;

import javax.net.ssl.SSLContext;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class HttpConnectionManager implements InitializingBean, DisposableBean {
	private static final Logger logger = LoggerFactory.getLogger(HttpConnectionManager.class);

	@Autowired
	private Environment env;

	private CloseableHttpClient client;
	private Boolean enablePooling;
	private PoolingHttpClientConnectionManager cm;

	public HttpConnectionManager() {

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		enablePooling = Boolean.valueOf(env.getProperty(Constants.ENABLE_HTTP_CONNECTION_POOLING));
		String tlsConfigStr = env.getProperty(Constants.SUPPORTED_TLS_PROTOCOLS);
		String tlsProtocols[] = tlsConfigStr.split(",");
		logger.info("received enablePooling falg as " + enablePooling);
		if (enablePooling) {
			SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(SSLContext.getDefault(),
					tlsProtocols, null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
			Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
					.register("http", PlainConnectionSocketFactory.getSocketFactory())
					.register("https", sslSocketFactory).build();

			Integer maxConnection = Integer.valueOf(env.getProperty(Constants.MAX_HTTPS_CONNECTIONS));
			cm = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
			cm.setMaxTotal(maxConnection);

			Integer perRouteConnections = Integer.valueOf(env.getProperty(Constants.MAX_PER_ROUTE_CONNECTIONS));
			cm.setDefaultMaxPerRoute(perRouteConnections);

			// configure IMS
			String imsHost = env.getProperty(Constants.IMS_HTTP_HOST);
			String imstHttpScheme = env.getProperty(Constants.IMS_HTTP_SCHEME);
			Integer port = Integer.valueOf(env.getProperty(Constants.IMS_HTTP_PORT));
			HttpHost host = new HttpHost(imsHost, port, imstHttpScheme);
			cm.setMaxPerRoute(new HttpRoute(host), perRouteConnections);

			// Http Client version 4.3+, use RequestConfig. Connection manager timeout.
			// default to the connection timeout
			// so no need to set it explicitly
			int connectTimeout = Integer.valueOf(env.getProperty(Constants.HTTP_CONNECT_TIMEOUT));
			int socketTimeout = Integer.valueOf(env.getProperty(Constants.HTTP_SOCKET_TIMEOUT));
			int connectionRequestTimeout = Integer.valueOf(env.getProperty(Constants.HTTP_CONNECTION_REQUEST_TIMEOUT));
			logger.info(
					"received http params as(imsHost,imstHttpScheme,port,connectTimeout,socketTimeout,connectionRequestTimeout) "
							+ imsHost + "," + imstHttpScheme + "," + port + "," + connectTimeout + "," + socketTimeout
							+ "," + connectionRequestTimeout);
			RequestConfig config = RequestConfig.custom().setConnectTimeout(connectTimeout)
					.setConnectionRequestTimeout(connectionRequestTimeout).setSocketTimeout(socketTimeout).build();

			client = HttpClients.custom().setConnectionManager(cm).setDefaultRequestConfig(config).build();
		}
	}

	@Override
	public void destroy() throws Exception {
		logger.info("destroying httpconnection " + cm);
		if (cm != null) {
			cm.shutdown();
		}
		logger.info("destroyed httpconnection " + cm);
	}

	public CloseableHttpClient getHttpClient() {
		logger.info("get httpconnection ");
		if (enablePooling) {
			logger.info("get httpconnection , returning from pool " + client);
			return client;
		} else {
			logger.info("get httpconnection , returning by creating new connection ");
			return HttpClientBuilder.create().build();
		}
	}
}
