package org.torproject.android.service;

import org.torproject.android.service.ITorServiceCallback;

/**
 * an interface for calling on to a remote service
 */
interface ITorService {
    /**
     * Often you want to allow a service to call back to its clients.
     * This shows how to do so, by registering a callback interface with
     * the service.
     */
    void registerCallback(ITorServiceCallback cb);
    
    /**
     * Remove a previously registered callback interface.
     */
    void unregisterCallback(ITorServiceCallback cb);
    
    int getStatus();
    
    void setProfile(int profile);
}
