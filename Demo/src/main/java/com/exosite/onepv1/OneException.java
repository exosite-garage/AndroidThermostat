package com.exosite.onepv1;

/*
  Base class for One Platform RPC API exceptions.
 */
public class OneException extends Exception {
    public OneException(final String message) {
        super(message);
    }
}
