package org.apache.nifi.attribute.expression.language.evaluation.functions.bigdecimal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

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

        BigDecimal percent = subject.multiply(hundr);
        percent = percent.setScale(subject.scale() - 2, RoundingMode.DOWN); // 00 in end, cut it
        return new StringQueryResult(percent.toString() + "%");
    }

    @Override
    public Evaluator<?> getSubjectEvaluator() {
        return subject;
    }

}
