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
