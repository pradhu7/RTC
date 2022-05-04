package com.apixio.v1security.exception;

public class ApixioSecurityException extends Exception
{
  /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

public ApixioSecurityException()
  {
  }

  public ApixioSecurityException(String msg)
  {
    super(msg);
  }
    public ApixioSecurityException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
