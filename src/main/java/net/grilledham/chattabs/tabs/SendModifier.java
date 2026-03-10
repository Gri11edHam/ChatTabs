package net.grilledham.chattabs.tabs;

import com.google.gson.annotations.Expose;

public class SendModifier {
	
	@Expose
	private String prefix;
	
	@Expose
	private String suffix;
	
	public SendModifier() {
		this("", "");
	}
	
	public SendModifier(String prefix) {
		this(prefix, "");
	}
	
	public SendModifier(String prefix, String suffix) {
		this.prefix = prefix;
		this.suffix = suffix;
	}
	
	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}
	
	public String getSuffix() {
		return suffix;
	}
	
	public String apply(String msg) {
		return prefix + msg + suffix;
	}
}
