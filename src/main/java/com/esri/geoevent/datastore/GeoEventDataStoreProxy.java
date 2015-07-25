/*
  Copyright 1995-2015 Esri

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

  For additional information, contact:
  Environmental Systems Research Institute, Inc.
  Attn: Contracts Dept
  380 New York Street
  Redlands, California, USA 92373

  email: contracts@esri.com
 */
package com.esri.geoevent.datastore;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.http.*;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.win.WindowsCredentialsProvider;
import org.apache.http.impl.client.*;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("/")
public class GeoEventDataStoreProxy
{
	private static final Logger		LOG				= Logger.getLogger(GeoEventDataStoreProxy.class.getName());
	private static final String		userAgent;
	private static final boolean	isWindows	= System.getProperty("os.name").toLowerCase().indexOf("win") >= 0;

	static
	{
		String tmpVersion;
		try
		{
			tmpVersion = IOUtils.toString(GeoEventDataStoreProxy.class.getResourceAsStream("/version"));
		}
		catch (IOException e)
		{
			tmpVersion = "<No Version Found>";
		}
		userAgent = "GeoEventDataStore Proxy " + tmpVersion;

	}

	private static class ServerInfo
	{
		URL				url, tokenUrl;
		AuthScope	authscope;
		Credentials	credentials, ntCredentials;
		HttpContext	httpContext	= null;
		String			encryptedToken, gisTierUsername, gisTierEncryptedPassword, name;
		Long				tokenExpiration;
	}

	private Map<String, ServerInfo>			serverInfos	= new HashMap<>();
	Registry<ConnectionSocketFactory>		registry;
	private Collection<X509Certificate>	trustedCerts;

	public GeoEventDataStoreProxy()
	{
		try (InputStream is = GeoEventDataStoreProxy.class.getResourceAsStream("/arcgisservers.properties"))
		{
			Properties props = new Properties();
			props.load(is);
			StringReader csvServers = new StringReader(props.getProperty("servers", ""));
			Iterable<CSVRecord> records = CSVFormat.DEFAULT.parse(csvServers);
			ServerInfo currInfo;
			String currServerName = "<not initialized>";
			String username, password;
			int port;
			Iterator<CSVRecord> iterator = records.iterator();
			if (iterator.hasNext())
			{
				CSVRecord record = iterator.next();
				int size = record.size();
				for (int i = 0; i < size; i++)
				{
					try
					{
						currInfo = new ServerInfo();
						currServerName = record.get(i);
						currInfo.url = new URL(props.getProperty(currServerName + ".url", ""));
						port = (currInfo.url.getPort() == -1) ? getDefaultPortForScheme(currInfo.url.getProtocol()) : currInfo.url.getPort();
						currInfo.authscope = new AuthScope(currInfo.url.getHost(), port);
						username = props.getProperty(currServerName + ".username", "");
						password = props.getProperty(currServerName + ".password", "");
						if (!StringUtils.isEmpty(username))
						{
							String encryptedPassword = Crypto.doEncrypt(password);
							currInfo.credentials = new UsernameEncryptedPasswordCredentials(username, encryptedPassword);
							currInfo.ntCredentials = new NTCredentialsEncryptedPassword(username + ":" + encryptedPassword);
						}
						currInfo.httpContext = createContextForServer(currInfo);
						String tokenUrlKey = currServerName + ".tokenUrl";
						String tokenUrl = props.getProperty(tokenUrlKey);
						if (tokenUrl != null)
						{
							currInfo.tokenUrl = new URL(tokenUrl);
						}

						username = props.getProperty(currServerName + ".gisTierUsername", "");
						if (!StringUtils.isEmpty(username))
						{
							password = props.getProperty(currServerName + ".gisTierPassword", "");
							currInfo.gisTierUsername = username;
							currInfo.gisTierEncryptedPassword = Crypto.doEncrypt(password);
						}
						currInfo.name=currServerName;
						serverInfos.put(currServerName, currInfo);
					}
					catch (Throwable t)
					{
						LOG.log(Level.ALL, "Failed to parse properties for server " + currServerName, t);
					}
				}
			}
		}
		catch (Throwable t)
		{
			LOG.log(Level.SEVERE, "Unable to initialize.  Will not be able to proxy any requests.", t);
		}

	}

	private String getBodyFromResponse(CloseableHttpResponse response) throws IOException
	{
		if (response == null)
		{
			return null;
		}

		HttpEntity entity = response.getEntity();
		String responseString = null;
		if (entity != null)
		{
			responseString = EntityUtils.toString(entity);
		}

		StatusLine statusLine = response.getStatusLine();
		if (statusLine.getStatusCode() != HttpStatus.SC_OK)
		{
			return null;
		}

		return responseString;

	}

	protected String messageFromErrorNode(JsonNode errorNode)
	{
		StringBuffer sb = new StringBuffer();
		sb.append(errorNode.get("message").asText());
		if (errorNode.has("details"))
		{
			JsonNode detailsNode = errorNode.get("details");
			if (detailsNode.isArray())
			{
				ArrayNode detailsArray = (ArrayNode) detailsNode;
				if (detailsArray.size() > 0)
				{
					sb.append(" ");
					for (JsonNode detail : detailsArray)
					{
						sb.append("[");
						sb.append(detail.asText());
						sb.append("]");
					}
				}
			}
			else
			{
				sb.append("[");
				sb.append(detailsNode.asText());
				sb.append("]");
			}
		}
		return sb.toString();
	}

	private String encodeParam(String param) throws UnsupportedEncodingException
	{
		if (StringUtils.isEmpty(param))
		{
			return "";
		}
		return URLEncoder.encode(param, "UTF-8");
	}

	synchronized private void getTokenForServer(ServerInfo serverInfo, MessageContext context) throws IOException, URISyntaxException, GeneralSecurityException
	{
		ensureCertsAreLoaded(context);
		try (CloseableHttpClient http = createHttpClient())
		{
			String query = "f=json&username=" + encodeParam(serverInfo.gisTierUsername) + "&password=" + encodeParam((serverInfo.gisTierEncryptedPassword == null) ? "" : Crypto.doDecrypt(serverInfo.gisTierEncryptedPassword)) + "&client=requestip&expiration=60";
			serverInfo.encryptedToken = null;
			HttpPost httpPost = createPostRequest(serverInfo.tokenUrl.toURI(), query, "application/x-www-form-urlencoded", serverInfo);
			try (CloseableHttpResponse response = http.execute(httpPost, serverInfo.httpContext))
			{
				String responseString = getBodyFromResponse(response);
				ObjectMapper mapper = new ObjectMapper();
				JsonNode jsonResponse = (responseString != null) ? mapper.readTree(responseString) : mapper.createObjectNode();
				if (!jsonResponse.has("token"))
				{
					String message;
					if (jsonResponse.has("error"))
					{
						message = messageFromErrorNode(jsonResponse.get("error"));
					}
					else
					{
						message = "No token in response: " + responseString;
					}
					LOG.log(Level.INFO, "Could not get token from URL: " + serverInfo.tokenUrl.toExternalForm() + ": " + message);
					serverInfo.tokenExpiration = Long.MAX_VALUE;
				}
				else
				{
					serverInfo.encryptedToken = Crypto.doEncrypt(jsonResponse.get("token").asText());
					serverInfo.tokenExpiration = jsonResponse.get("expires").asLong();
				}
			}
		}
	}

	private HttpClientConnectionManager createConnectionManager() throws GeneralSecurityException, IOException
	{
		KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
		trustStore.load(null, null);

		if (registry == null)
		{
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
			trustManagerFactory.init((KeyStore) null);
			X509TrustManager x509TrustManager = null;
			for (TrustManager trustManager : trustManagerFactory.getTrustManagers())
			{
				if (trustManager instanceof X509TrustManager)
				{
					x509TrustManager = (X509TrustManager) trustManager;
					break;
				}
			}

			X509Certificate[] acceptedIssuers = x509TrustManager.getAcceptedIssuers();
			if (acceptedIssuers != null)
			{
				// If this is null, something is really wrong...
				int issuerNum = 1;
				for (X509Certificate cert : acceptedIssuers)
				{
					trustStore.setCertificateEntry("issuer" + issuerNum, cert);
					issuerNum++;
				}
			}
			else
			{
				LOG.log(Level.INFO, "Didn't find any new certificates to trust.");
			}

			SSLContextBuilder sslContextBuilder = new SSLContextBuilder();

			sslContextBuilder.loadTrustMaterial(trustStore, new KnownArcGISCertificatesTrustStrategy(new ArrayList<>(trustedCerts)));
			SSLContext sslContext = sslContextBuilder.build();
			SSLContext.setDefault(sslContext);
			SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, new DataStoreProxyHostnameVerifier(new ArrayList<>(trustedCerts)));

			this.registry = RegistryBuilder.<ConnectionSocketFactory> create().register("http", PlainConnectionSocketFactory.getSocketFactory()).register("https", sslSocketFactory).build();
		}
		return new PoolingHttpClientConnectionManager(registry);
	}

	private HttpClientConnectionManager createConnectionManagerIfNecessary()
	{
		if (!CollectionUtils.isEmpty(trustedCerts))
		{
			try
			{
				return createConnectionManager();
			}
			catch (Throwable t)
			{
				LOG.log(Level.INFO, "Failed trying to create connection manager.", t);
			}
		}
		return null;
	}

	synchronized private void readSiteCertificates(ServletContext servletContext)
	{
		if (trustedCerts != null)
			return;

		trustedCerts = new ArrayList<>();

		// Now to add the other certificates added to the site
		File certsDirectory = new File(servletContext.getRealPath("/WEB-INF/classes/certificates"));
		try
		{
			if (certsDirectory.exists())
			{
				CertificateFactory cf = CertificateFactory.getInstance("X.509");
				String[] certs = certsDirectory.list(new FilenameFilter()
					{

						@Override
						public boolean accept(File dir, String fileName)
						{
							return fileName.endsWith(".crt") || fileName.endsWith(".cer") || fileName.endsWith(".pem");
						}

					});
				if (certs != null)
				{
					X509Certificate x509Cert;
					for (String cert : certs)
					{
						try (FileInputStream fis = new FileInputStream(new File(certsDirectory, cert)))
						{
							x509Cert = (X509Certificate) cf.generateCertificate(fis);
						}
						if (x509Cert != null && !trustedCerts.contains(x509Cert))
						{
							trustedCerts.add(x509Cert);
						}
					}
				}
			}
		}
		catch (Throwable e1)
		{
			LOG.log(Level.INFO, "Failed to load certificates from diretory " + certsDirectory.getAbsolutePath(), e1);
		}
	}

	private int getDefaultPortForScheme(String scheme)
	{
		if ("http".equalsIgnoreCase(scheme))
		{
			return 80;
		}
		else if ("https".equalsIgnoreCase(scheme))
		{
			return 443;
		}
		throw new RuntimeException("Unknown scheme: " + scheme);
	}

	synchronized private ServerInfo getServerInfo(String serverName)
	{
		if (!serverInfos.containsKey(serverName))
		{
			throw new WebApplicationException(Response.status(Status.BAD_REQUEST).entity("No configuration found for " + serverName).type(MediaType.TEXT_PLAIN).build());
		}
		return serverInfos.get(serverName);
	}

	private HttpContext createContextForServer(ServerInfo serverInfo)
	{
		HttpContext httpContext = null;
		if (serverInfo.credentials != null || serverInfo.ntCredentials != null)
		{
			HttpClientContext context = HttpClientContext.create();
			CredentialsProvider credsProvider = (isWindows) ? new WindowsCredentialsProvider(new SystemDefaultCredentialsProvider()) : new SystemDefaultCredentialsProvider();
			if (serverInfo.credentials != null)
			{
				credsProvider.setCredentials(serverInfo.authscope, serverInfo.credentials);
			}
			if (serverInfo.ntCredentials != null)
			{
				credsProvider.setCredentials(serverInfo.authscope, serverInfo.ntCredentials);
			}
			context.setCredentialsProvider(credsProvider);
			httpContext = context;
		}
		return httpContext;
	}

	private Response execute(CloseableHttpClient http, ServerInfo serverInfo, HttpRequestBase request, MessageContext context) throws IOException
	{
		ensureCertsAreLoaded(context);
		CloseableHttpResponse response = http.execute(request, serverInfo.httpContext);
		if (response == null)
		{
			return Response.status(502).build();
		}
		Header[] responseHeaders = response.getAllHeaders();
		ResponseBuilder builder = Response.status(response.getStatusLine().getStatusCode());
		for (Header header : responseHeaders)
		{
			builder.header(header.getName(), header.getValue());
		}
		String strReply = new String(EntityUtils.toByteArray(response.getEntity()));
		HttpServletRequest servletRequest = context.getHttpServletRequest();
		StringBuffer entireRequestUrl = servletRequest.getRequestURL();
		builder.entity(strReply.replaceAll(serverInfo.url.toExternalForm(), entireRequestUrl.substring(0, entireRequestUrl.indexOf(servletRequest.getPathInfo()))+"/"+serverInfo.name+"/"));
		return builder.build();
	}

	private CloseableHttpClient createHttpClient()
	{

		HttpClientBuilder builder = (isWindows) ? WinHttpClients.custom() : HttpClients.custom();
		HttpClientConnectionManager connMgr = createConnectionManagerIfNecessary();
		if (connMgr != null)
		{
			builder.setConnectionManager(connMgr);
		}
		builder.setUserAgent(userAgent);
		builder.useSystemProperties();

		return builder.build();
	}

	private void ensureCertsAreLoaded(MessageContext context)
	{
		if (trustedCerts == null)
			readSiteCertificates(context.getServletContext());
	}

	private Response execute(ServerInfo serverInfo, HttpRequestBase request, MessageContext context) throws IOException
	{
		ensureCertsAreLoaded(context);
		try (CloseableHttpClient http = createHttpClient())
		{
			return execute(http, serverInfo, request, context);
		}
		catch (Exception e)
		{
			if (e instanceof RuntimeException)
			{
				throw (RuntimeException) e;
			}
			throw new IOException(e);
		}
	}

	private List<NameValuePair> parseQueryStringAndAddToken(String queryString, String tokenToUse)
	{
		List<NameValuePair> params = URLEncodedUtils.parse(queryString, Charset.forName("UTF-8"));
		if (params != null)
		{
			NameValuePair tokenParam = null;
			for (NameValuePair param : params)
			{
				if ("token".equals(param.getName()))
				{
					tokenParam = param;
					break;
				}
			}
			if (tokenParam != null)
			{
				params.remove(tokenParam);
			}
		}
		else
		{
			params = new ArrayList<>();
		}
		if (tokenToUse != null)
		{
			params.add(new BasicNameValuePair("token", tokenToUse));
		}
		return params;
	}

	private URI createDestinationURI(ServerInfo serverInfo, String path, MessageContext msgContext)
	{
		try
		{
			if (serverInfo.tokenExpiration == null)
			{
				getTokenForServer(serverInfo, msgContext);
			}
			StringBuilder sb = new StringBuilder();
			sb.append(serverInfo.url.toExternalForm());
			if (sb.charAt(sb.length() - 1) != '/')
			{
				sb.append('/');
			}
			sb.append(path);

			String queryString = msgContext.getHttpServletRequest().getQueryString();
			String tokenToUse = null;
			if (serverInfo.encryptedToken != null)
			{
				tokenToUse = Crypto.doDecrypt(serverInfo.encryptedToken);
			}
			if (!StringUtils.isEmpty(queryString))
			{
				sb.append('?');

				if (serverInfo.tokenExpiration == null || System.currentTimeMillis() > serverInfo.tokenExpiration)
				{
					getTokenForServer(serverInfo, msgContext);
				}
				List<NameValuePair> params = parseQueryStringAndAddToken(queryString, tokenToUse);
				sb.append(URLEncodedUtils.format(params, "UTF-8"));
			}
			else if (tokenToUse != null)
			{
				sb.append("?token=");
				sb.append(tokenToUse);
			}

			return new URI(sb.toString());
		}
		catch (Throwable t)
		{
			throw new RuntimeException(t);
		}
	}

	@GET
	@Path("/{server}/{path:.*}")
	public Response proxyGet(@PathParam("server") String server, @PathParam("path") String path, @Context MessageContext msgContext) throws IOException
	{
		ServerInfo serverInfo = getServerInfo(server);
		return execute(serverInfo, new HttpGet(createDestinationURI(serverInfo, path, msgContext)), msgContext);
	}

	@DELETE
	@Path("/{server}/{path:.*}")
	public Response proxyDelete(@PathParam("server") String server, @PathParam("path") String path, @Context MessageContext msgContext) throws IOException
	{
		ServerInfo serverInfo = getServerInfo(server);
		return execute(serverInfo, new HttpDelete(createDestinationURI(serverInfo, path, msgContext)), msgContext);
	}

	private String getContentType(MessageContext msgContext)
	{
		HttpHeaders headers = msgContext.getHttpHeaders();
		List<String> contentTypeHeader = headers.getRequestHeader("Content-Type");
		String contentType;
		if (CollectionUtils.isEmpty(contentTypeHeader))
		{
			contentType = ContentType.APPLICATION_OCTET_STREAM.getMimeType();
		}
		else
		{
			contentType = contentTypeHeader.get(0);
		}
		return contentType;
	}

	private HttpPut createPutRequest(URI uri, byte[] putBody, String contentTypes)
	{
		HttpPut httpPut = new HttpPut(uri);

		ContentType contentType = ContentType.create(contentTypes);
		if (contentType == null)
			throw new RuntimeException("Couldn't create content types for " + contentTypes);

		ByteArrayEntity entity = new ByteArrayEntity(putBody, contentType);
		httpPut.setEntity(entity);

		return httpPut;
	}

	private HttpPost createPostRequest(URI uri, String postBody, String contentTypes, ServerInfo serverInfo)
	{
		HttpPost httpPost = new HttpPost(uri);
		ContentType contentType = ContentType.create(contentTypes);
		if (contentType == null)
			throw new RuntimeException("Couldn't create content types for " + contentTypes);

		if (ContentType.APPLICATION_FORM_URLENCODED.getMimeType().equals(contentType.getMimeType()))
		{
			String tokenToUse = null;
			if (serverInfo.encryptedToken != null)
			{
				try
				{
					tokenToUse = Crypto.doDecrypt(serverInfo.encryptedToken);
				}
				catch (GeneralSecurityException e)
				{
					throw new RuntimeException(e);
				}
			}
			List<NameValuePair> params = parseQueryStringAndAddToken(postBody, tokenToUse);
			postBody = URLEncodedUtils.format(params, "UTF-8");
		}

		StringEntity entity = new StringEntity(postBody, contentType);
		httpPost.setEntity(entity);

		return httpPost;
	}

	@POST
	@Path("/{server}/{path:.*}")
	public Response proxyPost(String payload, @PathParam("server") String server, @PathParam("path") String path, @Context MessageContext msgContext) throws IOException
	{
		ServerInfo serverInfo = getServerInfo(server);
		return execute(serverInfo, createPostRequest(createDestinationURI(serverInfo, path, msgContext), payload, getContentType(msgContext), serverInfo), msgContext);
	}

	@PUT
	@Path("/{server}/{path:.*}")
	public Response proxyPut(String payload, @PathParam("server") String server, @PathParam("path") String path, @Context MessageContext msgContext) throws IOException
	{
		ServerInfo serverInfo = getServerInfo(server);
		return execute(serverInfo, createPutRequest(createDestinationURI(serverInfo, path, msgContext), payload.getBytes(), getContentType(msgContext)), msgContext);
	}

}
