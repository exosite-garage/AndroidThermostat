/*=============================================================================
* HttpRPCResponseException.java
* Exception class for http response failure.
*==============================================================================
*
* Tested with JDK 1.6
*
* Copyright (c) 2011, Exosite LLC
* All rights reserved.
*/

package com.exosite.api.onep;

@SuppressWarnings("serial")
public class RPCResponseException extends OneException {

	public RPCResponseException(final String message) {
		super(message);
	}
}
