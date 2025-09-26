package com.portfolio.cucumber;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.jupiter.api.Test;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/**
 * JUnit 5 test that runs Cucumber scenarios
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, 
    value = "pretty, html:target/cucumber-reports, json:target/cucumber-reports/cucumber.json, junit:target/cucumber-reports/cucumber.xml")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, 
    value = "com.portfolio.cucumber")
@ConfigurationParameter(key = Constants.FILTER_TAGS_PROPERTY_NAME, 
    value = "@portfolio or @option or @realtime")
public class CucumberIntegrationTest {
    
    @Test
    public void runCucumberTests() {
        // This test will be executed by the JUnit 5 Platform Suite
        // The actual Cucumber tests are defined in the feature files
    }
}

