package org.sakaiproject.feeds.impl;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.ControllerThreadSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class CertIgnoreSSLProtocolSocketFactory implements SecureProtocolSocketFactory {
	/** Logging */
	private static Log			LOG				= LogFactory.getLog(CertIgnoreSSLProtocolSocketFactory.class);

	/** Custom SSL context */
	private SSLContext			context			= null;

	/** Custom Trust Manager */
	private X509TrustManager	trustManager	= null;
	

	public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
		return getContext().getSocketFactory().createSocket(socket, host, port, autoClose);
	}

	public Socket createSocket(String host, int port) throws IOException, UnknownHostException {
		return getContext().getSocketFactory().createSocket(host, port);
	}

	public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort) throws IOException, UnknownHostException {
		return getContext().getSocketFactory().createSocket(host, port, clientAddress, clientPort);
	}

	public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort, HttpConnectionParams params) throws IOException, UnknownHostException, ConnectTimeoutException {
		int timeout = params.getConnectionTimeout();
		if(timeout == 0){
			return createSocket(host, port, clientAddress, clientPort);
		}else{
			return ControllerThreadSocketFactory.createSocket(this, host, port, clientAddress, clientPort, timeout);
		}
	}
	
	private X509TrustManager getTrustManager() {
		if(trustManager == null) {
			// create a trust manager that blindly ignores certificate errors
			trustManager = new X509TrustManager() {
				public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					// ignore
				}
	
				public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
					// ignore
				}
	
				public X509Certificate[] getAcceptedIssuers() {
					return null; // ignore
				}
			};
		}
		return trustManager;
	}
	
	private SSLContext getContext() {
		if(context == null) {
			// setup context
			try{
				//this.context = SSLContext.getInstance("TLS");
				this.context = SSLContext.getInstance("SSL");
				context.init(null, new TrustManager[] { getTrustManager() }, null);
			}catch(NoSuchAlgorithmException e){
				LOG.error("Error setting up trust manager to ignore certificate errors", e);
			}catch(KeyManagementException e){
				LOG.error("Error setting up trust manager to ignore certificate errors", e);
			}
		}
		return context;
	}

}
