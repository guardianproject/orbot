package org.torproject.android.service;

import org.torproject.android.service.ITorServiceCallback;

/**
 * an interface for calling on to a remote service
 */
interface ITorService {

    /**
     * This allows Tor service to send messages back to the GUI
     */
    void registerCallback(ITorServiceCallback cb);
    
    /**
     * Remove registered callback interface.
     */
    void unregisterCallback(ITorServiceCallback cb);
    
    /**
    * Get a simple int status value for the state of Tor
    **/
    int getStatus();
    
    /**
    * The profile value is the start/stop state for Tor
    **/
    void setProfile(int profile);
    
    
    /**
    * Set configuration
    **/
    boolean updateConfiguration (String name, String value, boolean saveToDisk);
 
    /**
    * Set configuration
    **/
    void processSettings();
    
    /**
    * Set configuration
    **/
    boolean saveConfiguration ();
    
    /**
    * Get current configuration value from torrc
    */
    String getConfiguration (String name);
    
    /**
    * Get information
    */
    String getInfo (String args);
    
    /**
    * change identity
    */
    void newIdentity ();
    
}
