/*=============================================================================
* OnePlatformException.java
* Exception class for error message from one platform.
*==============================================================================
*
* Tested with JDK 1.6
*
* Copyright (c) 2011, Exosite LLC
* All rights reserved.
*/

package com.exosite.api.onep;

@SuppressWarnings("serial")
public class OnePlatformException extends OneException {

	public OnePlatformException(final String message) {
		super(message);
	}
}
