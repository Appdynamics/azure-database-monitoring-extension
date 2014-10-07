# AppDynamics Windows Azure Database Monitoring Extension

This extension works only with the standalone machine agent.

##Use Case

Windows Azure is an Internet-scale computing and services platform hosted in Microsoft data centers. It includes a number of features with corresponding developer services which can be used individually or together.
Windows Azure Database Monitoring Extension queries Azure Database system tables to fetch the metrics. For more information on Azure system tables please visit http://msdn.microsoft.com/en-us/library/dn282162.aspx 


##Prerequisite
Create database in azure and get the JDBC connection string. To get the connection string please follow this link http://azure.microsoft.com/en-us/documentation/articles/sql-data-java-how-to-use-sql-database/#determine_connection_string

##Installation
Azure SQL JDBC driver is not in maven repo. To build this we need to install the driver into the local maven repo.
Steps to install the Azure SQL JDBC driver in the local repo.

1. Download the SQL JDBC driver from http://www.microsoft.com/en-us/download/details.aspx?id=11774
2.  execut the maven install command <br>
mvn install:install-file -Dfile={path to sqljdbc4.jar} -DgroupId=com.microsoft.sqlserver -DartifactId=sqljdbc4 -Dversion=4.0 -Dpackaging=jar

Steps to build the extension

1. Run "mvn clean install"
2. Download and unzip the file 'target/AzureDatabaseMonitor.zip' to \{machineagent install dir\}/monitors
3. Open <b>monitor.xml</b> and configure the Azure arguments

<pre>
&lt;argument name="config-file" is-required="true" default-value="monitors/AzureDatabaseMonitor/config.yml" /&gt;
</pre>

<b>config-file</b> : yml file where we define the Azure Database configurations<br/>

example yml configuration
   ```
   # Azure Database particulars

   databaseServers:
    -   serverName: "gkbzse0m18"
        databasePort: "1433"
        databaseNames: [AppdTest, AppdTest2]
        adminUser: ""
        adminPassword: ""
    -   serverName: "hd57z9kvth"
        databasePort: "1433"
        databaseNames: [TestDb2]
        adminUser: ""
        adminPassword: ""
   
   #prefix used to show up metrics in AppDynamics
   metricPrefix: "Custom Metrics|Azure Database|"
   
   ```
<b>Note</b>: For each database configuration 2 queries will be fired to Azure. Please be careful while configuring more
databases.
##Metrics
The following metrics are reported.

###Connections

| Metrics|
|---------------- |
|Azure Database/{DatabaseServer}/{DatabaseName}/Connections/Throttled Connection Count|
|Azure Database/{DatabaseServer}/{DatabaseName}/Connections/Terminated Connection Count|
|Azure Database/{DatabaseServer}/{DatabaseName}/Connections/Successful Count|
|Azure Database/{DatabaseServer}/{DatabaseName}/Connections/Connection Failure Count|
|Azure Database/{DatabaseServer}/{DatabaseName}/Connections/Total Failure Count|

###Usage

All usage metrics are scaled to 100.

| Metric Path  |
|---------------- |
|Azure Database/{DatabaseServer}/{DatabaseName}/Usage/Average CPU Utilization Percent (x100)|
|Azure Database/{DatabaseServer}/{DatabaseName}/Usage/Maximum CPU Utilization Percent (x100)|
|Azure Database/{DatabaseServer}/{DatabaseName}/Usage/Maximum Memory Usage Percent (x100)|
|Azure Database/{DatabaseServer}/{DatabaseName}/Usage/Average Memory Usage Percent (x100)|

#Custom Dashboard
![](https://raw.githubusercontent.com/Appdynamics/azure-database-monitoring-extension/master/AzureDatabaseMonitor.png)

##Contributing

Always feel free to fork and contribute any changes directly here on GitHub.

##Community

Find out more in the [AppSphere]() community.

##Support

For any questions or feature request, please contact [AppDynamics Center of Excellence](mailto:help@appdynamics.com).
