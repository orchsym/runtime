package org.apache.nifi.processors.mapper.var;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.attribute.expression.language.PreparedQuery;
import org.apache.nifi.attribute.expression.language.Query;
import org.apache.nifi.attribute.expression.language.VariableImpact;
import org.apache.nifi.components.PropertyValue;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processors.mapper.exp.ExpVar;
import org.apache.nifi.processors.mapper.exp.ExpVarTable;
import org.apache.nifi.processors.mapper.exp.VarTableType;
import org.apache.nifi.record.path.exception.RecordPathException;
import org.apache.nifi.record.path.util.RecordPathCache;

/**
 * @author GU Guoqiang
 *
 */
public final class VarUtil {
    private final static RecordPathCache testRecordPathCache = new RecordPathCache(200);

    public static void calcVarValues(final ProcessContext context, final Map<String, String> expValuesMap, final ExpVarTable varTable, Map<String, ExpVar> unCalcExpVars, boolean withInnerVar) {
        if (varTable == null) {
            return;
        }
        if (unCalcExpVars == null) {
            unCalcExpVars = new LinkedHashMap<>();// keep the order
        }
        final boolean isGlobal = varTable.getType().equals(VarTableType.GLOBAL);

        Set<String> rpVarsSet = Collections.emptySet();
        if (!isGlobal) { // no record path for global
            final Map<String, ExpVar> rpVarsMap = varTable.getVars().stream().filter(v -> !hasEL(v.getExp()) && hasRP(v.getExp())) // can't support both, so only check one condition
                    .collect(Collectors.toMap(ExpVar::getName, Function.identity(), (v1, v2) -> v2, LinkedHashMap::new));// keep the order
            rpVarsSet = rpVarsMap.keySet();

            unCalcExpVars.putAll(rpVarsMap);
        }

        final String varPrefix = varTable.getVarPrefix();

        final Map<String, String> innerVarValuesMap = new HashMap<>(expValuesMap);

        for (ExpVar var : varTable.getVars()) {
            final String innerVarName = var.getName();
            final String outerVarName = varPrefix + '.' + innerVarName;
            final String exp = var.getExp();
            if (StringUtils.isEmpty(exp)) {
                continue;
            }

            final PreparedQuery query = Query.prepare(exp);
            if (query.isExpressionLanguagePresent()) { // hasEL(exp), expression
                boolean found = false;
                if (!isGlobal) {// no need check record path for global
                    VariableImpact variableImpact = query.getVariableImpact();
                    for (String rpVar : rpVarsSet) {
                        if (variableImpact.isImpacted(rpVar)) {
                            found = true;
                            break;
                        }
                    }
                }
                if (found) { // relate the record path, can't do calc here.
                    unCalcExpVars.put(var.getName(), var);
                } else {
                    PropertyValue expPropValue = context.newPropertyValue(exp);
                    expPropValue = expPropValue.evaluateAttributeExpressions(innerVarValuesMap);
                    String value = expPropValue.getValue();
                    if (value != null) {
                        expValuesMap.put(outerVarName, value);
                        if (withInnerVar) {
                            expValuesMap.put(innerVarName, value);
                        }

                        // support inner and outer for table var only
                        innerVarValuesMap.put(outerVarName, value);
                        innerVarValuesMap.put(innerVarName, value);
                    }
                }

            } else if (rpVarsSet.contains(var.getName())/* && hasRP(exp) */) {// shouldn't be RP yet.
                // ignore
            } else { // just literal value, add directly
                expValuesMap.put(outerVarName, exp);
                if (withInnerVar) {
                    expValuesMap.put(innerVarName, exp);
                }
                innerVarValuesMap.put(outerVarName, exp);
                innerVarValuesMap.put(innerVarName, exp);
            }
        }
    }

    /**
     * Check the value to contain the expression language or not.
     */
    public static boolean hasEL(String value) {
        if (value != null && !value.isEmpty()) {
            final PreparedQuery query = Query.prepare(value);
            return query.isExpressionLanguagePresent();
        }
        return false;
    }

    /**
     * Check the value to contain the RecordPath or not.
     */
    public static boolean hasRP(String value) {
        if (value != null && !value.trim().isEmpty()) {
            try {
                testRecordPathCache.getCompiled(value);
                return true;
            } catch (RecordPathException e) {
                // invalid record path
            }
        }
        return false;
    }
}
