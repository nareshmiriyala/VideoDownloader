/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.dellnaresh.videodownload.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author nareshm
 */
public class ConfigReader {
    private volatile static ConfigReader uniqueInstance;

    private ConfigReader() {

    }

    public static ConfigReader getInstance() {
        if (uniqueInstance == null) {
            synchronized (ConfigReader.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new ConfigReader();
                }
            }
        }
        return uniqueInstance;
    }

    public String getPropertyValue(String property) throws IOException {
        Properties prop = new Properties();
        String propFileName = "config.properties";

        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(propFileName);

        if (inputStream != null) {
            try {
                prop.load(inputStream);
            } catch (IOException ex) {
                Logger.getLogger(ConfigReader.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
        }
        inputStream.close();

        // get the property value and print it out
        return prop.getProperty(property);
    }

}
