package imperial.modaclouds.sdaSync;

import java.util.Map;

public class SDAMetric {
	private String metricName;
	
	private String function;
	
	private String targetMetric;
	
	public String getTargetMetric() {
		return targetMetric;
	}

	public void setTargetMetric(String targetMetric) {
		this.targetMetric = targetMetric;
	}

	private Map<String, String> parameters;

	public String getMetricName() {
		return metricName;
	}

	public void setMetricName(String metricName) {
		this.metricName = metricName;
	}

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}
	
	
}
