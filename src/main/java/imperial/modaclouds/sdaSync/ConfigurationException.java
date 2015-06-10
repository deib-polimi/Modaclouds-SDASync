package imperial.modaclouds.sdaSync;

public class ConfigurationException extends Exception {

	public ConfigurationException(String message) {
		super(message);
	}

	public ConfigurationException(String message, Exception e) {
		super(message,e);
	}


}
