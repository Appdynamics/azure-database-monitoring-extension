/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */

package com.appdynamics.monitors.azure.statsCollector;

import com.appdynamics.monitors.azure.config.Configuration;
import com.appdynamics.monitors.azure.config.DatabaseServer;
import com.singularity.ee.agent.systemagent.api.exception.TaskExecutionException;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AzureDatabaseStatsCollector {

    private static final Logger logger = Logger.getLogger(AzureDatabaseStatsCollector.class);
    private static final String CONNECTION_STRING = "jdbc:sqlserver://${databaseServer}.database.windows.net:${databasePort};database=${databaseName};user=${adminUser}@${databaseServer};password=${adminPassword};encrypt=true;hostNameInCertificate=*.database.windows.net;loginTimeout=30;";

    public static final String METRICS_SEPARATOR = "|";

    private static final String MASTER_DB_CONNECTIONS_QUERY = "select database_name,start_time,end_time,success_count,total_failure_count,connection_failure_count,\n" +
            "terminated_connection_count,throttled_connection_count from sys.database_connection_stats \n" +
            "where end_time > DATEADD(second, -DATEPART( second , SYSUTCDATETIME() ), DATEADD(minute, -10, SYSUTCDATETIME()))\n" +
            "and end_time <= DATEADD(second, -DATEPART( second , SYSUTCDATETIME() ), DATEADD(minute, -5, SYSUTCDATETIME()))";

    private static final String DB_RESOURCE_USAGE_QUERY = "SELECT  \n" +
            "    AVG(avg_cpu_percent)*100 AS 'Average CPU Utilization Percent', \n" +
            "    MAX(avg_cpu_percent)*100 AS 'Maximum CPU Utilization Percent', \n" +
            "    AVG(avg_memory_usage_percent)*100 AS 'Average Memory Usage Percent', \n" +
            "    MAX(avg_memory_usage_percent)*100 AS 'Maximum Memory Usage Percent' \n" +
            "FROM sys.dm_db_resource_stats where end_time <= DATEADD(second, -DATEPART( second , SYSUTCDATETIME() ), DATEADD(minute, -1, SYSUTCDATETIME()))\n" +
            "and end_time > DATEADD(second, -DATEPART( second , SYSUTCDATETIME() ), DATEADD(minute, -2, SYSUTCDATETIME()))";


    public Map<String, String> collectMetrics(Configuration config) throws TaskExecutionException {
        Map<String, String> stats = new HashMap<String, String>();

        List<DatabaseServer> databaseServers = config.getDatabaseServers();
        if (databaseServers == null || databaseServers.size() <= 0) {
            logger.error("No database(s) configured in the configuration file. Please configure databases to see metrics");
            throw new TaskExecutionException("No database(s) configured in the configuration file. Please configure databases to see metrics");
        }

        String metricPrefix = config.getMetricPrefix();
        for (DatabaseServer databaseServer : databaseServers) {
            String databaseServerName = databaseServer.getServerName();
            String databasePort = databaseServer.getDatabasePort();
            String adminUser = databaseServer.getAdminUser();
            String adminPassword = databaseServer.getAdminPassword();
            Map<String, String> masterDBMap = new HashMap<String, String>();
            masterDBMap.put("databaseServer", databaseServerName);
            masterDBMap.put("databasePort", databasePort);
            masterDBMap.put("adminUser", adminUser);
            masterDBMap.put("adminPassword", adminPassword);

            List<String> databaseNames = databaseServer.getDatabaseNames();

            for (String databaseName : databaseNames) {

                String masterConnectionString = getConnectionString(CONNECTION_STRING, masterDBMap, "master");
                Map<String, String> connectionStats = executeDBConnectionsQuery(databaseServerName, databaseName, masterConnectionString, metricPrefix);
                stats.putAll(connectionStats);

                String dbConnectionString = getConnectionString(CONNECTION_STRING, masterDBMap, databaseName);
                Map<String, String> resourceStats = executeDBResourceQuery(databaseServerName, databaseName, dbConnectionString, metricPrefix);
                stats.putAll(resourceStats);
            }
        }
        return stats;
    }

    private String getConnectionString(String connectionString, Map<String, String> valueMap, String databaseName) {
        StrSubstitutor strSubstitutor = new StrSubstitutor(valueMap);
        valueMap.put("databaseName", databaseName);
        String connectionStringReplaced = strSubstitutor.replace(connectionString);
        return connectionStringReplaced;
    }

    private Map<String, String> executeDBResourceQuery(String databaseServerName, String databaseName, String dbConnectionString, String metricPrefix) throws TaskExecutionException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        Map<String, String> connectionMetrics = new HashMap<String, String>();
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            connection = DriverManager.getConnection(dbConnectionString);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(DB_RESOURCE_USAGE_QUERY);

            while (resultSet.next()) {
                String avgCPUPercent = resultSet.getString(1);
                String maxCPUPercent = resultSet.getString(2);
                String avgMemoryUsagePercent = resultSet.getString(3);
                String maxMemoryUsagePercent = resultSet.getString(4);
                connectionMetrics.put(buildMetricKey(metricPrefix + databaseServerName, databaseName, "Usage", "Average CPU Utilization Percent (x 100)"), avgCPUPercent);
                connectionMetrics.put(buildMetricKey(metricPrefix + databaseServerName, databaseName, "Usage", "Maximum CPU Utilization Percent (x 100)"), maxCPUPercent);
                connectionMetrics.put(buildMetricKey(metricPrefix + databaseServerName, databaseName, "Usage", "Average Memory Usage Percent (x 100)"), avgMemoryUsagePercent);
                connectionMetrics.put(buildMetricKey(metricPrefix + databaseServerName, databaseName, "Usage", "Maximum Memory Usage Percent (x 100)"), maxMemoryUsagePercent);
            }

        } catch (ClassNotFoundException e) {
            logger.error("Required Class not found", e);
            throw new TaskExecutionException("Required Class not found", e);
        } catch (SQLException e) {
            logger.error("Error executing query", e);
        } finally {
            closeAll(connection, statement, resultSet);
        }
        return connectionMetrics;
    }

    private Map<String, String> executeDBConnectionsQuery(String databaseServerName, String databaseName, String connectionString, String metricPrefix) throws TaskExecutionException {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        Map<String, String> connectionMetrics = new HashMap<String, String>();
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");

            connection = DriverManager.getConnection(connectionString);
            statement = connection.createStatement();
            String dbConnectionsQuery = MASTER_DB_CONNECTIONS_QUERY + " and database_name='" + databaseName + "'";
            resultSet = statement.executeQuery(dbConnectionsQuery);

            while (resultSet.next()) {
                String successCount = resultSet.getString("success_count");
                String totalFailureCount = resultSet.getString("total_failure_count");
                String connectionFailureCount = resultSet.getString("connection_failure_count");
                String terminatedConnectionCount = resultSet.getString("terminated_connection_count");
                String throttledConnectionCount = resultSet.getString("throttled_connection_count");
                connectionMetrics.put(buildMetricKey(metricPrefix + databaseServerName, databaseName, "Connections", "Successful Count"), successCount);
                connectionMetrics.put(buildMetricKey(metricPrefix + databaseServerName, databaseName, "Connections", "Total Failure Count"), totalFailureCount);
                connectionMetrics.put(buildMetricKey(metricPrefix + databaseServerName, databaseName, "Connections", "Connection Failure Count"), connectionFailureCount);
                connectionMetrics.put(buildMetricKey(metricPrefix + databaseServerName, databaseName, "Connections", "Terminated Connection Count"), terminatedConnectionCount);
                connectionMetrics.put(buildMetricKey(metricPrefix + databaseServerName, databaseName, "Connections", "Throttled Connection Count"), throttledConnectionCount);
            }

        } catch (ClassNotFoundException e) {
            logger.error("Required Class not found", e);
            throw new TaskExecutionException("Required Class not found", e);
        } catch (SQLException e) {
            logger.error("Error executing query", e);
        } finally {
            closeAll(connection, statement, resultSet);
        }
        return connectionMetrics;
    }

    private String buildMetricKey(String metricPrefix, String... metricPath) {
        StringBuilder sb = new StringBuilder(metricPrefix);
        for (String path : metricPath) {
            sb.append(METRICS_SEPARATOR).append(path);
        }
        return sb.toString();
    }

    private void closeAll(Connection connection, Statement statement, ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                logger.error("Error while closing result set", e);
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                logger.error("Error while closing statement", e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("Error while closing connection", e);
            }
        }
    }
}