package org.apache.nifi.attribute.expression.language.evaluation;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.nifi.expression.AttributeExpression.ResultType;

public abstract class BigDecimalEvaluator implements Evaluator<BigDecimal> {

    private String token;

    @Override
    public ResultType getResultType() {
        return ResultType.BIGDECIMAL;
    }

    @Override
    public int getEvaluationsRemaining() {
        return 0;
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public void setToken(final String token) {
        this.token = token;
    }

}
