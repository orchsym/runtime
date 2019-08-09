package org.apache.nifi.attribute.expression.language.evaluation.functions.bigdecimal;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Map;

import org.apache.nifi.attribute.expression.language.evaluation.BigDecimalQueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.Evaluator;
import org.apache.nifi.attribute.expression.language.evaluation.QueryResult;
import org.apache.nifi.attribute.expression.language.evaluation.StringEvaluator;
import org.apache.nifi.attribute.expression.language.evaluation.StringQueryResult;

/**
 * 
 * @author LiGuo BigDecimal 百分比
 */
public class ToPercentBigDecimalEvaluator extends StringEvaluator {

    private Evaluator<?> subject;

    public ToPercentBigDecimalEvaluator(Evaluator<?> subject) {
        this.subject = subject;
    }

    public QueryResult<String> evaluate(Map<String, String> attributes) {
        final QueryResult<?> result = subject.evaluate(attributes);
        final Object value = result.getValue();
        if (value == null) {
            return new StringQueryResult(null);
        }
        BigDecimal subject = new BigDecimal(value.toString());
        BigDecimal hundr = new BigDecimal(100); 
        return new StringQueryResult(subject.multiply(hundr).toString()+"%");
    }

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subject;
    }

}
