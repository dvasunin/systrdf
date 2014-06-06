// (C) Copyright 2003-2011 Hewlett-Packard Development Company, L.P.

package demo.platform;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This tool unifies access to properties in Systinet's demos.
 * There are two levels of properties. First level is global.
 * You can define properties there, that are common to all
 * demos. Second level is local and contains properties, that
 * are specific to certain demo. You can even predefine global
 * properties there.
 * <p>
 * Global properties are mandatory. If they cannot be read,
 * the application will be killed! Local properties are optional,
 * but if you specify them and they cannot be read, the application
 * will be killed too!
 */
public final class DemoProperties {
    /** system property: filename of global properties */
    public static final String GLOBAL_PROPERTY_FILE = "platform.demo.global.properties";
    /** system property: filename of local properties */
    public static final String LOCAL_PROPERTY_FILE = "platform.demo.local.properties";

    static Properties properties;
    static {
        properties = loadProperties();
    }

    /**
     * Loads properties. At least <code>GLOBAL_PROPERTY_FILE</code> system
     * property must be specified.
     * @return Properties that were initialized with global and optionaly local properties.
     */
    private static Properties loadProperties() {
        String globalFile = System.getProperty(GLOBAL_PROPERTY_FILE);
        String localFile = System.getProperty(LOCAL_PROPERTY_FILE);
        if ( globalFile==null || globalFile.length()==0 ) {
            System.err.println("System property "+GLOBAL_PROPERTY_FILE+" is not set!");
            System.exit(1);
        }

        Properties props = new Properties();
        try {
            InputStream fis = new FileInputStream(globalFile);
            props.load(fis);
            fis.close();

            if ( localFile!=null && localFile.length()>0 ) {
                fis = new FileInputStream(localFile);
                props.load(fis);
                fis.close();
            }
        } catch (IOException e) {
            System.err.println("Cannot read properties!");
            e.printStackTrace();
            System.exit(1);
        }

        return props;
    }

    /**
     * Searches for the key in the demo's properties.
     * @param key name of property to be searched
     * @return value associated with key
     */
    public static String getProperty(String key) {
        String found = System.getProperty(key);
        return (found != null)? found : properties.getProperty(key);
    }

    /**
     * Searches for the key in the demo's properties. Default value
     * will be used, if key is not found.
     * @param key name of property to be searched
     * @param defaultValue to be used, if key is not found
     * @return value associated with key
     */
    public static String getProperty(String key, String defaultValue) {
        String found = System.getProperty(key);
        return (found != null) ? found : properties.getProperty(key,defaultValue);
    }

    /**
     * Searches for the key in the demo's properties. DefaultValue
     * will be used, if key is not found or cannot be converted to int.
     * @param key name of property to be searched
     * @param defaultValue to be used, if key is not found
     * @return value associated with key
     */
    public static int getIntProperty(String key, int defaultValue) {
        String tmp = System.getProperty(key);
        if (tmp==null)
            tmp = properties.getProperty(key);

        if ( tmp!=null && tmp.length()>0 ) {
            try {
                return Integer.parseInt(tmp);
            } catch (NumberFormatException e) {
                System.err.println("Cannot convert property " + key + " to int! ("+tmp+")");
            }
        }
        return defaultValue;
    }
}
