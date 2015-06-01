package com.esri.geoevent.datastore;

import org.apache.http.auth.NTCredentials;

public class NTCredentialsEncryptedPassword extends NTCredentials
{

	private static final long	serialVersionUID	= -2904600252667151921L;

	public NTCredentialsEncryptedPassword(String usernamePassword)
	{
		super(usernamePassword);
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
