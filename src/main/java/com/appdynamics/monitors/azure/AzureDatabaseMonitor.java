package com.appdynamics.monitors.azure;

import com.appdynamics.extensions.PathResolver;
import com.appdynamics.monitors.azure.config.AzureDatabaseMonitorConstants;
import com.appdynamics.monitors.azure.config.ConfigUtil;
import com.appdynamics.monitors.azure.config.Configuration;
import com.appdynamics.monitors.azure.statsCollector.AzureDatabaseStatsCollector;
import com.google.common.base.Strings;
import com.singularity.ee.agent.systemagent.api.AManagedMonitor;
import com.singularity.ee.agent.systemagent.api.MetricWriter;
import com.singularity.ee.agent.systemagent.api.TaskExecutionContext;
import com.singularity.ee.agent.systemagent.api.TaskOutput;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

public class AzureDatabaseMonitor extends AManagedMonitor {

    private static final Logger logger = Logger.getLogger(com.appdynamics.monitors.azure.AzureDatabaseMonitor.class);

    private static final String CONFIG_ARG = "config-file";
    private static final String FILE_NAME = "monitors/AzureDatabaseMonitor/config.yml";

    private static final ConfigUtil<Configuration> configUtil = new ConfigUtil<Configuration>();
    private AzureDatabaseStatsCollector azureDatabaseStatsCollector = new AzureDatabaseStatsCollector();

    public AzureDatabaseMonitor() {
        String details = com.appdynamics.monitors.azure.AzureDatabaseMonitor.class.getPackage().getImplementationTitle();
        String msg = "Using Monitor Version [" + details + "]";
        logger.info(msg);
        System.out.println(msg);
    }

    public TaskOutput execute(Map<String, String> taskArgs, TaskExecutionContext taskExecutionContext) throws TaskExecutionException {
        if (taskArgs != null) {
            logger.info("Starting the AzureDatabase Monitoring task.");
            String configFilename = getConfigFilename(taskArgs.get(CONFIG_ARG));
            try {
                Configuration config = configUtil.readConfig(configFilename, Configuration.class);
                collectAndPrintMetrics(config);
                logger.info("Completed the AzureDatabase Monitoring Task successfully");
                return new TaskOutput("AzureDatabase Monitor executed successfully");
            } catch (FileNotFoundException e) {
                logger.error("Config File not found: " + configFilename, e);
            } catch (Exception e) {
                logger.error("Metrics Collection Failed: ", e);
            }
        }
        throw new TaskExecutionException("AzureDatabase Monitor completed with failures");
    }

    private void collectAndPrintMetrics(Configuration config) throws TaskExecutionException {
        try {
            Map<String, String> metrics = azureDatabaseStatsCollector.collectMetrics(config);
            printMetrics(metrics);
            print(config.getMetricPrefix() + AzureDatabaseMonitorConstants.METRICS_COLLECTED, AzureDatabaseMonitorConstants.SUCCESS_VALUE);
        } catch (Exception e) {
            print(config.getMetricPrefix() + AzureDatabaseMonitorConstants.METRICS_COLLECTED, AzureDatabaseMonitorConstants.ERROR_VALUE);
        }
    }

    private void printMetrics(Map<String, String> resourceStats) {

        for (Map.Entry<String, String> statsEntry : resourceStats.entrySet()) {
            String value = statsEntry.getValue();
            String key = statsEntry.getKey();
            try {
                double metricValue = Double.parseDouble(value.trim());
                print(key, metricValue);
            } catch (NumberFormatException e) {
                logger.error("Value of metric [" + key + "] can not be converted to number, Ignoring the stats.");
            }
        }
    }

    private void print(String key, double metricValue) {
        MetricWriter metricWriter = super.getMetricWriter(key, MetricWriter.METRIC_AGGREGATION_TYPE_AVERAGE, MetricWriter.METRIC_TIME_ROLLUP_TYPE_AVERAGE, MetricWriter.METRIC_CLUSTER_ROLLUP_TYPE_COLLECTIVE
        );
        metricWriter.printMetric(String.valueOf(Math.round(metricValue)));
    }

    private String getConfigFilename(String filename) {
        if (filename == null) {
            return "";
        }

        if ("".equals(filename)) {
            filename = FILE_NAME;
        }
        // for absolute paths
        if (new File(filename).exists()) {
            return filename;
        }
        // for relative paths
        File jarPath = PathResolver.resolveDirectory(AManagedMonitor.class);
        String configFileName = "";
        if (!Strings.isNullOrEmpty(filename)) {
            configFileName = jarPath + File.separator + filename;
        }
        return configFileName;
    }
}