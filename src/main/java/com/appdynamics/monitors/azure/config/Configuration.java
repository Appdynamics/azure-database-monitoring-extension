/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */
package com.appdynamics.monitors.azure.config;

import java.util.List;

public class Configuration {

    private List<DatabaseServer> databaseServers;
    private String metricPrefix;

    public List<DatabaseServer> getDatabaseServers() {
        return databaseServers;
    }

    public void setDatabaseServers(List<DatabaseServer> databaseServers) {
        this.databaseServers = databaseServers;
    }

    public String getMetricPrefix() {
        return metricPrefix;
    }

    public void setMetricPrefix(String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }
}