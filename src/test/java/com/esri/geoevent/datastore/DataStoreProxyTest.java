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
