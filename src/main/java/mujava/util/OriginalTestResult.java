package mujava.util;

import org.junit.runner.notification.Failure;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class OriginalTestResult implements Serializable {
    private BigDecimal resultScore;
    private int runCount=-1;
    private ArrayList<Failure> failure=new ArrayList<>();
    private String testSetName;//Name of the testset for which these results are applicable
    private JacocoTestResult instrumentedResult=new JacocoTestResult();

    public BigDecimal getResultScore() {
        return resultScore;
    }

    public void setResultScore(double resultScore) {
        this.resultScore = new BigDecimal(resultScore);
    }

    public void setResultScore(int resultScore) {
        this.resultScore = BigDecimal.valueOf(resultScore);
    }

    public void setResultScore(BigDecimal resultScore) {
        this.resultScore = resultScore;
    }

    public int getRunCount() {
        return runCount;
    }

    public void setRunCount(int runCount) {
        this.runCount = runCount;
    }

    public ArrayList<Failure> getFailure() {
        return failure;
    }

    public void setFailure(List<Failure> failure) {
        this.failure=new ArrayList<Failure>(failure);
    }

    public JacocoTestResult getInstrumentedResult() {
        return instrumentedResult;
    }

    public void setInstrumentedResult(JacocoTestResult instrumentedResult) {
        this.instrumentedResult = instrumentedResult;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OriginalTestResult that = (OriginalTestResult) o;
        return runCount == that.runCount &&
                Objects.equals(resultScore, that.resultScore) &&
                Objects.equals(failure, that.failure);
    }

    @Override
    public int hashCode() {

        return Objects.hash(resultScore, runCount, failure);
    }

    public String getTestSetName()
    {
        return testSetName;
    }

    public void setTestSetName(String testSetName)
    {
        this.testSetName = testSetName;
    }
}
