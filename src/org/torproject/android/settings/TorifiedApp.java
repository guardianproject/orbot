package org.torproject.android.settings;

import android.graphics.drawable.Drawable;

public class TorifiedApp implements Comparable {

	private boolean enabled;
	private int uid;
	private String username;
	private String procname;
	private String name;
	private Drawable icon;
	
	private boolean torified = false;
	private boolean usesInternet = false;
	
	public boolean usesInternet() {
		return usesInternet;
	}
	public void setUsesInternet(boolean usesInternet) {
		this.usesInternet = usesInternet;
	}
	/**
	 * @return the torified
	 */
	public boolean isTorified() {
		return torified;
	}
	/**
	 * @param torified the torified to set
	 */
	public void setTorified(boolean torified) {
		this.torified = torified;
	}
	private int[] enabledPorts;
	
	/**
	 * @return the enabledPorts
	 */
	public int[] getEnabledPorts() {
		return enabledPorts;
	}
	/**
	 * @param enabledPorts the enabledPorts to set
	 */
	public void setEnabledPorts(int[] enabledPorts) {
		this.enabledPorts = enabledPorts;
	}
	/**
	 * @return the enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}
	/**
	 * @param enabled the enabled to set
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	/**
	 * @return the uid
	 */
	public int getUid() {
		return uid;
	}
	/**
	 * @param uid the uid to set
	 */
	public void setUid(int uid) {
		this.uid = uid;
	}
	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}
	/**
	 * @param username the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	/**
	 * @return the procname
	 */
	public String getProcname() {
		return procname;
	}
	/**
	 * @param procname the procname to set
	 */
	public void setProcname(String procname) {
		this.procname = procname;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	

	public Drawable getIcon() {
		return icon;
	}
	
	public void setIcon(Drawable icon) {
		this.icon = icon;
	}
	
	@Override
	public int compareTo(Object another) {
		
		return this.toString().compareTo(another.toString());
	}
	
	@Override
	public String toString ()
	{
		return getName();
	}
}
