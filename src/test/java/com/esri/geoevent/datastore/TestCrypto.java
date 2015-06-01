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
