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

package com.exosite.onepv1;

@SuppressWarnings("serial")
public class HttpRPCRequestException extends Exception {

	public HttpRPCRequestException(final String message) {
		super(message);
	}
}
