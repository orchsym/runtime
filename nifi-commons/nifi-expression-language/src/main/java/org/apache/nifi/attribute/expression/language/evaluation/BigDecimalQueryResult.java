package org.apache.nifi.attribute.expression.language.evaluation;

import java.math.BigDecimal;

import org.apache.nifi.expression.AttributeExpression.ResultType;

public class BigDecimalQueryResult implements QueryResult<BigDecimal> {

    private BigDecimal value;
    
    public BigDecimalQueryResult(BigDecimal value) {
        this.value = value;
    }

    @Override
    public BigDecimal getValue() {
        // TODO Auto-generated method stub
        return value;
    }

    @Override
    public ResultType getResultType() {
        // TODO Auto-generated method stub
        return ResultType.BIGDECIMAL;
    }
    
    @Override
    public String toString() {
        return String.valueOf(getValue());
    }

}
