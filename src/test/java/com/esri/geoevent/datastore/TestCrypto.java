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

import org.junit.Assert;
import org.junit.Test;

public class TestCrypto
{
	@Test
	public void testEncryptDecrypt() throws Exception
	{
		String plainText = "GeoEvent DataStore Proxy";
		String encryptedText = Crypto.doEncrypt(plainText);
		String decryptedText = Crypto.doDecrypt(encryptedText);
		Assert.assertEquals(plainText, decryptedText);
	}
	
	@Test
	public void testCredentials() throws Exception
	{
		String username = "username";
		String password = "password";
		String encryptedPassword = Crypto.doEncrypt(password);
		
		UsernameEncryptedPasswordCredentials creds = new UsernameEncryptedPasswordCredentials(username, encryptedPassword);
		Assert.assertEquals(password, creds.getPassword());
		
		NTCredentialsEncryptedPassword ntCreds = new NTCredentialsEncryptedPassword(username + ":" + encryptedPassword);
		Assert.assertEquals(password, ntCreds.getPassword());
	}
}
