package com.esri.geoevent.datastore;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

import org.apache.http.conn.ssl.TrustStrategy;

public class KnownArcGISCertificatesTrustStrategy implements TrustStrategy
{

  
  private Collection<X509Certificate>	trustedCerts;

	KnownArcGISCertificatesTrustStrategy( Collection<X509Certificate> trustedCerts )
  {
    this.trustedCerts = trustedCerts;
  }
  
  @Override
  public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException
  {
    return chain != null && chain[0] != null && trustedCerts.contains( chain[0] );
  }

}
