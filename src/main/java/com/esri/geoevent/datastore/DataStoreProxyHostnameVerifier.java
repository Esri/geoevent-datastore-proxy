package com.esri.geoevent.datastore;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;

public class DataStoreProxyHostnameVerifier implements HostnameVerifier
{
	private Collection<X509Certificate>	trustedCerts;
	private DefaultHostnameVerifier	verifier;

	DataStoreProxyHostnameVerifier(Collection<X509Certificate>	trustedCerts)
	{
		this.trustedCerts = trustedCerts;
		this.verifier = new DefaultHostnameVerifier();
	}
	
	@Override
	public final String toString()
	{
		return "DataStoreProxyHostnameVerifier";
	}
	
	@Override
	public boolean verify(String hostname, SSLSession session)
	{
		try
		{
			final Certificate[] certs = session.getPeerCertificates();
			if (trustedCerts.contains( ( (X509Certificate) certs[0] ) ) )
			{
				return true;
			}
			return verifier.verify(hostname,  session);
		}
		catch (Exception e)
		{
			return false;
		}
	}

}
