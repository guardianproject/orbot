// Copyright 2005 Nick Mathewson, Roger Dingledine
// See LICENSE file for copying information
package org.torproject.android.control;

import java.io.IOException;

/**
 * An exception raised when Tor behaves in an unexpected way.
 */
public class TorControlSyntaxError extends IOException {

    static final long serialVersionUID = 3;

    public TorControlSyntaxError(String s) { super(s); }
}

