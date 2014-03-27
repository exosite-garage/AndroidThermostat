/*=============================================================================
* HttpRPCRequestException.java
* Exception class for http request failure.
*==============================================================================
*
* Tested with JDK 1.6
*
* Copyright (c) 2011, Exosite LLC
* All rights reserved.
*/

package com.exosite.api.onep;

@SuppressWarnings("serial")
public class RPCRequestException extends OneException {

	public RPCRequestException(final String message) {
		super(message);
	}
}
