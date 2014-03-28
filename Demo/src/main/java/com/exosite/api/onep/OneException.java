package com.exosite.api.onep;

import com.exosite.api.ExoException;

/*
  Base class for One Platform RPC API exceptions.
 */
public class OneException extends ExoException {
    public OneException(final String message) {
        super(message);
    }
}
