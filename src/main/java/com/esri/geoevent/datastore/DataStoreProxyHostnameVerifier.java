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
