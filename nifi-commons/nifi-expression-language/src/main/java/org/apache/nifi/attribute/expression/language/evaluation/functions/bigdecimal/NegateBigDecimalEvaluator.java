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
 * BigDecimal 求反
 */
public class NegateBigDecimalEvaluator extends BigDecimalEvaluator {

    private Evaluator<?> subject;

    public NegateBigDecimalEvaluator(Evaluator<?> subject) {
        this.subject = subject;
    }

    @Override
    public QueryResult<BigDecimal> evaluate(Map<String, String> attributes) {
        final QueryResult<?> result = subject.evaluate(attributes);
        final Object value = result.getValue();
        if (value == null) {
            return new BigDecimalQueryResult(null);
        }

        return new BigDecimalQueryResult(new BigDecimal(value.toString()).negate());
    }

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subject;
    }

}
