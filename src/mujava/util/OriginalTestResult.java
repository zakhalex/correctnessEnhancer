package mujava.util;

import org.junit.runner.notification.Failure;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class OriginalTestResult {
    private BigDecimal resultScore;
    private int runCount=-1;
    private List<Failure> failure=new ArrayList<>();

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

    public List<Failure> getFailure() {
        return failure;
    }

    public void setFailure(List<Failure> failure) {
        this.failure = failure;
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
}
