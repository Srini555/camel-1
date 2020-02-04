/* Generated by camel build tools - do NOT edit this file! */
package org.apache.camel.component.spark;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.GeneratedPropertyConfigurer;
import org.apache.camel.support.component.PropertyConfigurerSupport;

/**
 * Generated by camel build tools - do NOT edit this file!
 */
@SuppressWarnings("unchecked")
public class SparkComponentConfigurer extends PropertyConfigurerSupport implements GeneratedPropertyConfigurer {

    @Override
    public boolean configure(CamelContext camelContext, Object obj, String name, Object value, boolean ignoreCase) {
        SparkComponent target = (SparkComponent) obj;
        switch (ignoreCase ? name.toLowerCase() : name) {
        case "lazystartproducer":
        case "lazyStartProducer": target.setLazyStartProducer(property(camelContext, boolean.class, value)); return true;
        case "rdd": target.setRdd(property(camelContext, org.apache.spark.api.java.JavaRDDLike.class, value)); return true;
        case "rddcallback":
        case "rddCallback": target.setRddCallback(property(camelContext, org.apache.camel.component.spark.RddCallback.class, value)); return true;
        case "basicpropertybinding":
        case "basicPropertyBinding": target.setBasicPropertyBinding(property(camelContext, boolean.class, value)); return true;
        default: return false;
        }
    }

}
