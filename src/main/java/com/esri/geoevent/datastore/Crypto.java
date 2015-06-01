package com.esri.geoevent.datastore;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.commons.codec.binary.Base64;

class Crypto
{
	private static final SecretKey encryptionKey;
	private static final String ALGO = "AES";
	static
	{
		KeyGenerator keyGen;
		try
		{
			keyGen = KeyGenerator.getInstance("AES");
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
    keyGen.init(128);
    encryptionKey = keyGen.generateKey();
	}
	
  static public String doEncrypt(String stringToEncrypt) throws GeneralSecurityException
  {
    Cipher c = Cipher.getInstance(ALGO);
    c.init(Cipher.ENCRYPT_MODE, encryptionKey);
    byte[] encVal = c.doFinal(stringToEncrypt.getBytes());
    String encodedEncryptedString = new String(Base64.encodeBase64(encVal));
    return encodedEncryptedString;
  }	
  
  static public String doDecrypt(String stringToDecrypt) throws GeneralSecurityException
  {
    Cipher c = Cipher.getInstance(ALGO);
    c.init(Cipher.DECRYPT_MODE, encryptionKey);
    byte[] decodedValue = Base64.decodeBase64(stringToDecrypt.getBytes());
    byte[] decryptedValue = c.doFinal(decodedValue);
    String decryptedString = new String(decryptedValue);
    return decryptedString;
  }
  
  private Crypto() {}
}
