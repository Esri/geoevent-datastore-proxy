package com.esri.geoevent.datastore;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.ext.MessageContext;
import org.junit.Test;
import org.mockito.Mockito;

public class DataStoreProxyTest extends Mockito
{
	@Test
	public void testProxyCreation() throws Exception
	{
		MessageContext msgContext = mock(MessageContext.class);
		HttpServletRequest servletRequest = mock(HttpServletRequest.class);
		ServletContext     servletContext = mock(ServletContext.class);
		when(msgContext.getHttpServletRequest()).thenReturn(servletRequest);
		when(msgContext.getServletContext()).thenReturn(servletContext);
		when(servletContext.getRealPath("/WEB-INF/classes/certificates")).thenReturn("target/classes/certificates");
		when(servletRequest.getQueryString()).thenReturn("f=pjson&token=shouldBeReplaced");
		
		GeoEventDataStoreProxy proxy = new GeoEventDataStoreProxy();
		//Response response = proxy.proxyGet("javidel", "arcgis/rest/info", msgContext);
		//byte[] entity = (byte[])response.getEntity();
		//System.out.println(new String(entity));
		//response = proxy.proxyGet("portalhostqa", "gis/sharing/rest/info", msgContext);
		//entity = (byte[])response.getEntity();
		//System.out.println(new String(entity));
	}
}
