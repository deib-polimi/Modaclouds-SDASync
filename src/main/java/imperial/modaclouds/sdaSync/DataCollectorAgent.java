package imperial.modaclouds.sdaSync;

import it.polimi.tower4clouds.common.net.UnexpectedAnswerFromServerException;
import it.polimi.tower4clouds.data_collector_library.DCAgent;
import it.polimi.tower4clouds.manager.api.ManagerAPI;
import it.polimi.tower4clouds.manager.api.NotFoundException;
import it.polimi.tower4clouds.model.data_collectors.DCDescriptor;
import it.polimi.tower4clouds.model.ontology.InternalComponent;
import it.polimi.tower4clouds.model.ontology.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DataCollectorAgent  {
	
	public static final Logger logger = LoggerFactory.getLogger(DataCollectorAgent.class);

	public ManagerAPI manager;

	public DCDescriptor dcDescriptor;

	public DCAgent dcAgent;

	Config config = null;

	private String[] supportedMetrics = {"EstimationCI","EstimationFCFS","EstimationUBO","EstimationUBR"
			,"HaproxyCI","HaproxyUBR","ForecastingTimeseriesARIMA","ForecastingTimeseriesARMA","ForecastingTimeseriesAR"};

	public DataCollectorAgent() {

	}

	public Resource createResource(String resource){
		Resource resources = new Resource(null,resource);
		return resources;
	}

	public void initiate() {
		try {
			config = Config.getInstance();
		} catch (ConfigurationException e) {
			logger.error("Error while getting the instance of the configuration.", e);
		}

		manager = new ManagerAPI(config.getMmIP(),
				config.getMmPort());
		dcAgent = new DCAgent(manager);
		dcDescriptor = new DCDescriptor();
		dcDescriptor.setConfigSyncPeriod(60);
		dcAgent.setDCDescriptor(dcDescriptor);
		dcAgent.start();
	}

	public Map<String,ArrayList<String>> checkEstimationMetric() {
		Map<String,ArrayList<String>> metrics = new HashMap<String,ArrayList<String>>();
		try {
			Set<Resource> resources = manager.getResources();

			while (resources == null) {
				try {
					Thread.sleep(1000);
				} catch (Exception e) { }
				resources = manager.getResources();
			}
			
			int count = 1;
			for(Resource resource : resources) {
				if (resource instanceof InternalComponent) {
					ArrayList<String> methods = new ArrayList<String>();
					
					methods.addAll(((InternalComponent) resource).getProvidedMethods());
					metrics.put("instance"+count, methods);
					count++;
				}
			}
		}
		catch (UnexpectedAnswerFromServerException | IOException e) {
			logger.error("Error while checking the estimation metric.", e);
		}

		return metrics; 
	}

	public Set<SDAMetric> checkMetric() {
		Set<String> requiredMetrics = null;
		Set<SDAMetric> sdaSet = new HashSet<SDAMetric>();
		try {
			requiredMetrics = manager.getRequiredMetrics();
		} catch (UnexpectedAnswerFromServerException | IOException e1) {
			logger.error("Error while getting the required metrics.", e1);
		}
		for (String requiredMetric : requiredMetrics) {
			for (String supportedMetric: supportedMetrics) {
				if (requiredMetric.startsWith(supportedMetric)) {

					SDAMetric sda = new SDAMetric();

					dcDescriptor.addMonitoredResource(requiredMetric,
							new Resource()); // registering a data collector for
					// metric
					// Forecast_AverageFrontendCPU and
					// for all resources
					dcAgent.refresh();  // required to be called when dcDescriptor is
					// updated

					int index = requiredMetric.indexOf("_");
					String subString = requiredMetric.substring(index+1);

					String metricToBeForecast = null;
					if (subString.indexOf("_") == -1) {
						metricToBeForecast = subString;
					}
					else {
						metricToBeForecast = subString.substring(0,subString.indexOf("_"));
					}

					//String metricToBeForecast = requiredMetric.substring(index+1);
					logger.info("Forecast required for metric "
							+ metricToBeForecast);

					try {
						manager.registerHttpObserver(metricToBeForecast, config.getSdaURL(),
								"TOWER/JSON");
					} catch (NotFoundException | IOException e) {
						logger.error("Error while registering the HTTP observer.", e);
					} 						// An observer can
					// be attached asking for different
					// formats: RDF/JSON, TOWER/JSON,
					// GRAPHITE,... TOWER/JSON is the new
					// serialization format for sending
					// monitoring data through the network
					// replacing RDF/JSON. Example of json
					// datum:
					// {"resourceId": "frontend1", "metric":
					// "AverageFrontendCPU", "value": 0.14,
					// "timestamp": 123131231 }
					sda.setFunction(supportedMetric);
					sda.setMetricName(requiredMetric);

					Map<String, String> params = dcAgent.getParameters(requiredMetric);
					while (params == null) {
						try {
							Thread.sleep(1000);
						} catch (Exception e) { }
						params = dcAgent.getParameters(requiredMetric);
					}

					sda.setParameters(params);
					sda.setTargetMetric(metricToBeForecast);
					sdaSet.add(sda);

					break;
				}
			}
		}
		return sdaSet;
	}

}
