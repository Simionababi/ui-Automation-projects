package config

import org.testng.IInvokedMethod
import org.testng.IInvokedMethodListener
import org.testng.ITestResult
import org.testng.Reporter

class ReportAdapter implements IInvokedMethodListener {
    void beforeInvocation(IInvokedMethod iInvokedMethod, ITestResult iTestResult){

    }

    void afterInvocation(IInvokedMethod iInvokedMethod, ITestResult iTestResult){
        Reporter.log String.valueOf("number of errors: " + iTestResult.instance.properties.report.numberOfErrors),true
        Reporter.log String.valueOf("some parameter: " + iTestResult.instance.properties.SOMEPARAMETER),true
    }
}
