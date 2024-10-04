package io.sirius.ms.sdk.api;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class SiriusSpringHandler implements TestExecutionListener {
    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        TestExecutionListener.super.testPlanExecutionStarted(testPlan);
        System.out.println("=============================> INIT TEST SETUP");
        TestSetup.getInstance();
    }

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        TestExecutionListener.super.testPlanExecutionFinished(testPlan);
        System.out.println("=============================> STOP TEST SETUP. SHUT DOWN SIRIUS INSTANCE");
        TestSetup.getInstance().destroy();
    }
}
