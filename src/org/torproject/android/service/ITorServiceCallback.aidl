package org.torproject.android.service;

/**
 * Callback interface used to send
 * synchronous notifications back to its clients.  Note that this is a
 * one-way interface so the server does not block waiting for the client.
 */
oneway interface ITorServiceCallback {
    /**
     * Called when the service has a something to display to the user
     */
    void statusChanged(String value);
    
    /**
     * Called when the service has something to add to the log
     */
    void logMessage(String value);
    
}
