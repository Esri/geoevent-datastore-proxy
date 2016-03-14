package com.esri.geoevent.datastore;

import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.impl.client.SystemDefaultCredentialsProvider;

public class HttpClientCredentialsProvider extends SystemDefaultCredentialsProvider
{
	Credentials		credentials;
	NTCredentials	ntCredentials;

	HttpClientCredentialsProvider(Credentials credentials, NTCredentials ntCredentials)
	{
		this.credentials = credentials;
		this.ntCredentials = ntCredentials;
	}

	@Override
	public Credentials getCredentials(AuthScope authscope)
	{
		Credentials retCredentials = null;
		String host = authscope.getHost();
		int port = authscope.getPort();
		if (host != AuthScope.ANY_HOST && port != AuthScope.ANY_PORT)
		{
			String scheme = authscope.getScheme().toUpperCase();
			switch (scheme)
			{
				case "BASIC":
				case "DIGEST":
					retCredentials = credentials;
					break;
				case AuthSchemes.NTLM:
					retCredentials = ntCredentials;
					break;
				default:
					break;
			}
		}

		if (retCredentials == null)
		{
			retCredentials = super.getCredentials(authscope);
		}
		return retCredentials;
	}
}
