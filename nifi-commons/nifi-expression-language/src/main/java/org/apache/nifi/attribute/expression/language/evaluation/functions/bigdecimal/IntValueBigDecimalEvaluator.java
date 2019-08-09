package org.apache.nifi.attribute.expression.language.evaluation.functions.bigdecimal;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.nifi.attribute.expression.language.evaluation.BigDecimalEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.BigDecimalQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.attribute.expression.language.evaluation.NumberEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.NumberQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.QueryResult;

/**
 * 
 * @author LiGuo
 * BigDecimal 转int
 */
public class IntValueBigDecimalEvaluator extends NumberEvaluator {

    private final Evaluator<BigDecimal> subject;

    public IntValueBigDecimalEvaluator(Evaluator<?> subjectEvaluator) {
        this.subject =  (Evaluator<BigDecimal>) subjectEvaluator;
    }

    @Override
    public QueryResult<Number> evaluate(Map<String, String> attributes) {
        final BigDecimal subjectValue = subject.evaluate(attributes).getValue();
        if (subjectValue == null) {
            return new NumberQueryResult(null);
        }

        return new NumberQueryResult(subjectValue.intValue());
    }

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subject;
    }

}
