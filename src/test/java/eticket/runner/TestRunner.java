package eticket.runner;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * TestRunner is the main execution file for our Cucumber tests.
 * TestNG uses this class to read, locate, and execute our Gherkin feature files.
 * 
 * For manual testers:
 * - "@CucumberOptions" is where we configure how our test runs.
 * - "features": Specifies the directory path where our .feature files are located.
 * - "glue": Specifies the package name containing our Java Step Definitions.
 * - "plugin": Configures built-in reports (like clean HTML and JSON files) and pretty-printing.
 * - "monochrome": When true, prints console outputs in a cleaner, more readable format.
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = "eticket.steps",
        plugin = {
                "pretty",
                "html:target/cucumber-reports/cucumber.html",
                "json:target/cucumber-reports/cucumber.json"
        },
        monochrome = true
)
public class TestRunner extends AbstractTestNGCucumberTests {
    // This class remains empty! It is simply used as a hook for TestNG to boot up Cucumber.
}
