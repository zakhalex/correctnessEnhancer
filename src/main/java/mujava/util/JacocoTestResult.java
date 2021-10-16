package mujava.util;

import java.util.Map;

public class JacocoTestResult {

    private FullyQualifiedName testMethodFQN;
    private boolean wasFailed;
    private String failedReason;
    private Map<FullyQualifiedName, Coverage> coverages;

    public JacocoTestResult()
    {

    }

    public JacocoTestResult(FullyQualifiedName testMethodFQN, boolean wasFailed, String failedReason, Map<FullyQualifiedName, Coverage> coverages) {
        this.testMethodFQN=testMethodFQN;
        this.wasFailed=wasFailed;
        this.failedReason=failedReason;
        this.coverages=coverages;
    }

    public FullyQualifiedName getTestMethodFQN() {
        return testMethodFQN;
    }

    public void setTestMethodFQN(FullyQualifiedName testMethodFQN) {
        this.testMethodFQN = testMethodFQN;
    }

    public boolean isWasFailed() {
        return wasFailed;
    }

    public void setWasFailed(boolean wasFailed) {
        this.wasFailed = wasFailed;
    }

    public String getFailedReason() {
        return failedReason;
    }

    public void setFailedReason(String failedReason) {
        this.failedReason = failedReason;
    }

    public Map<FullyQualifiedName, Coverage> getCoverages() {
        return coverages;
    }

    public void setCoverages(Map<FullyQualifiedName, Coverage> coverages) {
        this.coverages = coverages;
    }
}
