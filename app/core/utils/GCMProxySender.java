package core.utils;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.android.gcm.server.Endpoint;
import com.google.android.gcm.server.Sender;

public class GCMProxySender extends Sender {
	private static final Logger logger = LoggerFactory.getLogger(GCMProxySender.class);
	private String httpProxyHost;
	private int httpProxyPort;
	private String httpProxyUsername;
	private String httpProxyPassword;

	public GCMProxySender(String key, String httpProxyHost, String httpProxyPort, String httpProxyUsername,
			String httpProxyPassword) {
		super(key, Endpoint.FCM);
		this.httpProxyHost = httpProxyHost;
		this.httpProxyPort = Integer.valueOf(httpProxyPort);
		this.httpProxyUsername = httpProxyUsername;
		this.httpProxyPassword = httpProxyPassword;
	}

	@Override
	protected HttpsURLConnection getConnection(String url) throws IOException {
		logger.debug("getting httpconnection from GCMProxySender , for url " + url);
		Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost, httpProxyPort));
		HttpsURLConnection conn = (HttpsURLConnection) new URL(url).openConnection(proxy);
		Authenticator.setDefault(new Authenticator() {
			@Override
			public PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(httpProxyUsername, httpProxyPassword.toCharArray());
			}
		});
		logger.debug("got httpconnection from GCMProxySender , for url " + url);
		return conn;
	}
}