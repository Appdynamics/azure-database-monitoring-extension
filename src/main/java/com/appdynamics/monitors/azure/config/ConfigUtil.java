/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */
package com.appdynamics.monitors.azure.config;

import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ConfigUtil<T> {

	private static final Logger logger = Logger.getLogger(ConfigUtil.class);

	public T readConfig(String fileName, Class<T> clazz) throws FileNotFoundException {
		logger.info("Reading config file: " + fileName);
		Yaml yaml = new Yaml(new Constructor(clazz));
		T config = (T) yaml.load(new FileInputStream(fileName));
		return config;
	}

    public static void main(String[] args) throws FileNotFoundException {
        ConfigUtil<Configuration> configUtil = new ConfigUtil<Configuration>();
        Configuration config = configUtil.readConfig("/home/satish/AppDynamics/Code/extensions/azure-database-monitoring-extension/src/main/resources/config/config.yml", Configuration.class);
        System.out.println(config);
    }
}