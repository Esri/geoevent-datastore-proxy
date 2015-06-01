package com.esri.geoevent.datastore;

import org.apache.http.auth.UsernamePasswordCredentials;

public class UsernameEncryptedPasswordCredentials extends UsernamePasswordCredentials
{
	private static final long	serialVersionUID	= -2883802709045437235L;
	
	public UsernameEncryptedPasswordCredentials(String userName, String password)
	{
		super(userName, password);
	}

	@Override
	public String getPassword()
	{
		try
		{
			return Crypto.doDecrypt(super.getPassword());
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

}
