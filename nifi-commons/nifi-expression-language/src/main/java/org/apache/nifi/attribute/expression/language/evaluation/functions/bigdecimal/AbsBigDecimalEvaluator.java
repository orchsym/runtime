package org.apache.nifi.attribute.expression.language.evaluation.functions.bigdecimal;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.nifi.attribute.expression.language.evaluation.BigDecimalEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.BigDecimalQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.attribute.expression.language.evaluation.QueryResult;

/**
 * 
 * @author LiGuo
 * BigDecimal 绝对值
 */
public class AbsBigDecimalEvaluator extends BigDecimalEvaluator {

    private Evaluator<?> subject;

    public AbsBigDecimalEvaluator(Evaluator<?> subject) {
        this.subject = subject;
    }

    @Override
    public QueryResult<BigDecimal> evaluate(Map<String, String> attributes) {
        final QueryResult<?> result = subject.evaluate(attributes);
        final Object value = result.getValue();
        if (value == null) {
            return new BigDecimalQueryResult(null);
        }

        return new BigDecimalQueryResult(new BigDecimal(value.toString()).abs());
    }

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subject;
    }

}
