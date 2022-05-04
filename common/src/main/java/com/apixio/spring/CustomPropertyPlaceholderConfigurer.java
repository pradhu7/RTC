package com.apixio.spring;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

import java.io.IOException;
import java.util.Properties;

/**
 * The CustomPropertyPlaceholderConfigurer is an extension of the PropertyPlaceholderConfigurer and
 * allows local caching of the loaded properties so they can later be read in and used as needed.
 *
 * The class was created based off of the implementation provided by Alexander V. Zinin.
 */
public class CustomPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {
    private Properties mProps = new Properties();

    protected void loadProperties(Properties props)
        throws IOException 
    {
        super.loadProperties(props);

        mProps.putAll(props);
    }

    /**
     * Returns the entire set of accumulated properties
     */
    public Properties getProps() 
    {
        return mProps;
    }

    /**
     *
     */
    public String getProperty(String name, String defValue)
    {
        return mProps.getProperty(name, defValue);
    }

}
