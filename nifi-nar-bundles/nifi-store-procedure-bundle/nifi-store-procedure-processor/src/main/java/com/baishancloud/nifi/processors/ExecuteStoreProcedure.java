package com.baishancloud.nifi.processors;

import groovy.lang.Closure;
import groovy.sql.GroovyRowResult;
import groovy.sql.OutParameter;
import groovy.sql.Sql;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.behavior.EventDriven;
import org.apache.nifi.annotation.behavior.InputRequirement;
import org.apache.nifi.annotation.behavior.InputRequirement.Requirement;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Marks;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.dbcp.DBCPService;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.io.OutputStreamCallback;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.util.StopWatch;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;


@EventDriven
@Marks(categories = { "Database/Database" }, createdDate = "2018-07-31")
@InputRequirement(Requirement.INPUT_ALLOWED)
@Tags({"store procedure", "call", "sql"})
@CapabilityDescription("call provided SQL store procedure method. Query result will be converted to JSON."
        + " Streaming is used so arbitrarily large result sets are supported. This processor can be scheduled to run on "
        + "a timer, or cron expression, using the standard scheduling methods, or it can be triggered by an incoming FlowFile. "
        + "If it is triggered by an incoming FlowFile, then attributes of that FlowFile will be available when evaluating the "
        + "select query, and the query may use the ? to escape parameters. In this case, the parameters to use must exist as FlowFile attributes "
        + "with the naming convention sql.args.N.type and sql.args.N.value, where N is a positive integer. The sql.args.N.type is expected to be "
        + "a number indicating the JDBC Type. The content of the FlowFile is expected to be in UTF-8 format. "
        + "FlowFile attribute 'executesql.row.count' indicates how many rows were selected.")
@WritesAttributes({
        @WritesAttribute(attribute = "executesql.row.count", description = "Contains the number of rows returned in the select query"),
        @WritesAttribute(attribute = "executesql.query.duration", description = "Duration of the query in milliseconds"),
        @WritesAttribute(attribute = "executesql.resultset.index", description = "Assuming multiple result sets are returned, "
                + "the zero based index of this result set."),
        @WritesAttribute(attribute = "ResultSet", description = "call store procedure is expected to return some results. ResultSet is a JsonArray returned by call store procedure which also " +
                "has some JsonArrays contains many JsonObject, one JsonObject represents a row in a table of database"),
        @WritesAttribute(attribute = "Results", description = "call store procedure is expected to return some results. Results is JsonObject returned by call store procedure represents some" +
                "return variables like outParameters or inoutParameters")
})
public class ExecuteStoreProcedure extends AbstractProcessor {

    public static final String RESULT_ROW_COUNT = "sp.row.count";
    public static final String RESULT_OUT_COUNT = "sp.out.count";
    public static final String RESULT_QUERY_DURATION = "sp.query.duration";
    public static final String RESULT_QUERY_CALL = "sp.query.call";
    public static final String RESULT_QUERY_PARAMS = "sp.query.params";
    public static final String PARAM_INDEX = "param.index";
    public static final String PARAM_VALUE = "param.value";
    public static final String PARAM_TYPE = "param.type";
    public static final String PARAM_VALUE_DATA_TYPE = "param.value.type";
    public static final String PARAM_FORMAT = "param.format";
    public static final Pattern LONG_PATTERN = Pattern.compile("^-?\\d{1,19}$");

    public enum DateType {
        BIT(0, Sql.BIT),
        BOOLEAN(1, Sql.BOOLEAN),
        SMALLINT(2, Sql.SMALLINT),
        INTEGER(3, Sql.INTEGER),
        BIGINT(4, Sql.BIGINT),
        REAL(5, Sql.REAL),
        FLOAT(6, Sql.FLOAT),
        DOUBLE(7, Sql.DOUBLE),
        DECIMAL(8, Sql.DECIMAL),
        NUMERIC(9, Sql.NUMERIC),
        DATE(10, Sql.DATE),
        TIME(11, Sql.TIME),
        TIMESTAMP(12, Sql.TIMESTAMP),
        BINARY(13, Sql.BINARY),
        VARBINARY(14, Sql.VARBINARY),
        LONGVARBINARY(15, Sql.LONGVARBINARY),
        CHAR(16, Sql.CHAR),
        VARCHAR(17, Sql.VARCHAR),
        LONGVARCHAR(18, Sql.LONGVARCHAR),
        CLOB(19, Sql.CLOB),
        ARRAY(20, Sql.ARRAY),
        BLOB(21, Sql.BLOB),
        DATALINK(22, Sql.DATALINK),
        DISTINCT(23, Sql.DISTINCT),
        JAVA_OBJECT(24, Sql.JAVA_OBJECT),
        NULL(25, Sql.NULL),
        OTHER(26, Sql.OTHER),
        REF(27, Sql.REF),
        STRUCT(28, Sql.STRUCT),
        TINYINT(29, Sql.TINYINT);

        private Integer value;
        private OutParameter outParameter;

        DateType(Integer value, OutParameter outParameter) {
            this.value = value;
            this.outParameter = outParameter;
        }

        public Integer getValue() {
            return value;
        }

        public OutParameter getOutParameter() {
            return outParameter;
        }

        public static DateType getByValue(int value) {
            for (DateType v : values()) {
                if (v.getValue().equals(value))
                    return v;
            }
            return null;
        }

    }

    // Relationships
    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Successfully created FlowFile from SQL query result set.")
            .build();
    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("SQL query execution failed. Incoming FlowFile will be penalized and routed to this relationship")
            .build();
    private final Set<Relationship> relationships;

    public static final PropertyDescriptor DBCP_SERVICE = new PropertyDescriptor.Builder()
            .name("Database Connection Pooling Service")
            .description("The Controller Service that is used to obtain connection to database")
            .required(true)
            .identifiesControllerService(DBCPService.class)
            .build();

    public static final PropertyDescriptor METHOD_NAME = new PropertyDescriptor.Builder()
            .name("MethodName")
            .description("The method name to call. If this property is specified, it will be used regardless of the content of "
                    + "incoming flowfiles. If this property is empty, the content of the incoming flow file is expected "
                    + "to a valid method, to be issued by the processor to the database.")
            .required(true)
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    public static final PropertyDescriptor PARAMS = new PropertyDescriptor.Builder()
            .name("Params")
            .description("The params to method. If this property is specified, it will be used regardless of the attr of "
                    + "incoming flowfiles. If this property is empty, the attr of the incoming flow file is expected "
                    + "to be valid, to be issued by the processor to the database.params is JsonArray which has many JsonObjects" +
                    "represents to a param features ,features key contails param.index,param.value,param.type,param.value.type," +
                    "param.format. the value of param.index is a Integer represents a param sequence in a sql. param.type is a param type" +
                    "like IN,IN/OUT,OUT. param. param. param.value and param.value.type respresents the value and value type of the param." +
                    "param.format is optional, but default options may not always work for your data. \"\n" +
                    "Incoming FlowFiles are expected to be parametrized SQL statements. In some cases \"\n" +
                    "a format option needs to be specified, currently this is only applicable for binary data types, dates, times and timestamps. Binary Data Types (defaults to 'ascii') - \"\n" +
                    "ascii: each string character in your attribute value represents a single byte. This is the format provided by Avro Processors. \"\n" +
                    "base64: the string is a Base64 encoded string that can be decoded to bytes. \"\n" +
                    "hex: the string is hex encoded with all letters in upper case and no '0x' at the beginning. \"\n" +
                    "Dates/Times/Timestamps - \"\n" +
                    "Date, Time and Timestamp formats all support both custom formats or named format ('yyyy-MM-dd','ISO_OFFSET_DATE_TIME') \"\n" +
                    "as specified according to java.time.format.DateTimeFormatter. \"\n" +
                    "If not specified, a long value input is expected to be an unix epoch (milli seconds from 1970/1/1), or a string value in \"\n" +
                    "'yyyy-MM-dd' format for Date, 'HH:mm:ss.SSS' for Time (some database engines e.g. Derby or MySQL do not support milliseconds and will truncate milliseconds), \"\n" +
                    "'yyyy-MM-dd HH:mm:ss.SSS' for Timestamp is used.")
            .required(false)
            .addValidator(StandardValidators.ATTRIBUTE_EXPRESSION_LANGUAGE_VALIDATOR)
            .build();

    public static final PropertyDescriptor QUERY_TIMEOUT = new PropertyDescriptor.Builder()
            .name("Max Wait Time")
            .description("The maximum amount of time allowed for a running SQL select query "
                    + " , zero means there is no limit. Max time less than 1 second will be equal to zero.")
            .defaultValue("0 seconds")
            .required(true)
            .addValidator(StandardValidators.TIME_PERIOD_VALIDATOR)
            .sensitive(false)
            .build();

    public static final PropertyDescriptor FETCH_SIZE = new PropertyDescriptor.Builder()
            .name("Fetch Size")
            .description("The maximum amount of time allowed for a running SQL select query "
                    + " , zero means there is no limit. Max time less than 1 second will be equal to zero.")
            .defaultValue("0")
            .required(false)
            .addValidator(StandardValidators.NUMBER_VALIDATOR)
            .sensitive(false)
            .build();
    private final List<PropertyDescriptor> propDescriptors;
    private Object[] outs;

    public ExecuteStoreProcedure() {
        final Set<Relationship> r = new HashSet<>();
        r.add(REL_SUCCESS);
        r.add(REL_FAILURE);
        relationships = Collections.unmodifiableSet(r);

        final List<PropertyDescriptor> pds = new ArrayList<>();
        pds.add(DBCP_SERVICE);
        pds.add(METHOD_NAME);
        pds.add(QUERY_TIMEOUT);
        pds.add(PARAMS);
        pds.add(FETCH_SIZE);
        propDescriptors = Collections.unmodifiableList(pds);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return propDescriptors;
    }

    @OnScheduled
    public void setup(ProcessContext context) {
        // If the query is not set, then an incoming flow file is needed. Otherwise fail the initialization
        if (!context.getProperty(METHOD_NAME).isSet() && !context.hasIncomingConnection()) {
            final String errorString = "Either the method name must be specified or there must be an incoming connection "
                    + "providing flowfile(s) containing a SQL select query";
            getLogger().error(errorString);
            throw new ProcessException(errorString);
        }
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile fileToProcess = null;
        if (context.hasIncomingConnection()) {
            fileToProcess = session.get();
            // If we have no FlowFile, and all incoming connections are self-loops then we can continue on.
            // However, if we have no FlowFile and we have connections coming from other Processors, then
            // we know that we should run only if we have a FlowFile.
            if (fileToProcess == null && context.hasNonLoopConnection()) {
                return;
            }

        }

        final ComponentLog logger = getLogger();
        final DBCPService dbcpService = context.getProperty(DBCP_SERVICE).asControllerService(DBCPService.class);
        final Integer queryTimeout = context.getProperty(QUERY_TIMEOUT).asTimePeriod(TimeUnit.SECONDS).intValue();
        final Integer fetchSize = context.getProperty(FETCH_SIZE) == null ? 0 : Integer.parseInt(context.getProperty(FETCH_SIZE).getValue());
        final StopWatch stopWatch = new StopWatch(true);
        String methodName="", params=null;
        if (context.getProperty(METHOD_NAME).isSet()) {
            methodName = context.getProperty(METHOD_NAME).getValue();
        } else {
            // If the query is not set, then an incoming flow file is required, and expected to contain a valid SQL select query.
            // If there is no incoming connection, onTrigger will not be called as the processor will fail when scheduled.
            methodName = fileToProcess.getAttribute("MethodName");

        }
        if (context.getProperty(PARAMS).isSet()) {
            params = context.getProperty(PARAMS).getValue();
        } else {
            // If the query is not set, then an incoming flow file is required, and expected to contain a valid SQL select query.
            // If there is no incoming connection, onTrigger will not be called as the processor will fail when scheduled.
            try{
                params = fileToProcess.getAttribute("Params");
            }catch (Exception e){
            }
        }
        String callQuery = "";
        Connection con = null;
        ExecuteStoreProcedureUtils sql = null;
        try {
            con = dbcpService.getConnection();
            sql = new ExecuteStoreProcedureUtils(con);
            sql.withStatement(new Closure(this) {
                @Override
                public Object call(Object... args) {
                    if (null != args && args.length > 0) {
                        if (args[0] instanceof CallableStatement) {
                            //here can set statement ,I set queryTimeout now and later can add more
                            CallableStatement callableStatement = (CallableStatement) args[0];
                            try {
                                if (queryTimeout != null)
                                    callableStatement.setQueryTimeout(queryTimeout);
                                if (fetchSize != null && fetchSize != 0){
                                    callableStatement.setFetchSize(fetchSize);
                                }
                            } catch (SQLException e) {
                                throw new ProcessException(e);
                            }
                        }
                    }
                    return null;
                }
            });
            List<Object> paramList = null;
            paramList = analyzeParamJsonArray(params);
            callQuery = GenerteCallQuery(methodName, paramList == null ? 0 : paramList.size());
            List<List<GroovyRowResult>> list = sql.callWithAllRows(callQuery, paramList == null ? new ArrayList<Object>() : paramList, new Closure(this) {
                @Override
                public Object call(Object... args) {
                    if (null != args && args.length > 0)
                        outs = args;
                    return null;
                }
            });
            if (null == fileToProcess)
                fileToProcess = session.create();
            fileToProcess = session.write(fileToProcess, new OutputStreamCallback() {
                @Override
                public void process(OutputStream outputStream) throws IOException {
                    if (null != list && list.size() > 0) {
                        outputStream.write(formatResults(list).getBytes(StandardCharsets.UTF_8));
                    }
                    if (null != outs && !outs.equals("")) {
                        outputStream.write(formatResults(outs).getBytes(StandardCharsets.UTF_8));
                    }
                }
            });
            Map<String,String> attrs=new HashMap<>();
            if (list != null && list.size() > 0) {
                attrs.put(RESULT_ROW_COUNT, String.valueOf(list.size()));
            }
            if (outs != null && !outs.equals("")) {
                attrs.put(RESULT_OUT_COUNT, String.valueOf(outs.length));
            }
            long duration = stopWatch.getElapsed(TimeUnit.MILLISECONDS);
            attrs.put(RESULT_QUERY_DURATION, String.valueOf(duration));
            attrs.put( RESULT_QUERY_CALL, callQuery);
            attrs.put(RESULT_QUERY_PARAMS, params);
            
            fileToProcess=session.putAllAttributes(fileToProcess, attrs);
            session.transfer(fileToProcess, REL_SUCCESS);

        } catch (final ProcessException | SQLException e) {
            //If we had at least one result then it's OK to drop the original file, but if we had no results then
            //  pass the original flow file down the line to trigger downstream processors
            if (fileToProcess == null) {
                // This can happen if any exceptions occur while setting up the connection, statement, etc.
                logger.error("Unable to execute SQL select query {} due to {}. No FlowFile to route to failure",
                        new Object[]{callQuery, e});
                context.yield();
            } else {
                if (context.hasIncomingConnection()) {
                    logger.error("Unable to execute SQL select query {} for {} due to {}; routing to failure",
                            new Object[]{callQuery, fileToProcess, e});
                    fileToProcess = session.penalize(fileToProcess);
                } else {
                    logger.error("Unable to execute SQL select query {} due to {}; routing to failure",
                            new Object[]{callQuery, e});
                    context.yield();
                }
                session.transfer(fileToProcess, REL_FAILURE);
            }
        } finally {
            if (null != sql) {
                sql.close();
            }
            if (null != con) {
                try {
                    con.close();
                } catch (Exception e) {
                    throw new ProcessException(e.getMessage());
                }
            }
        }
    }

    public List<Object> analyzeParamJsonArray(String paramString) {
        if (null == paramString || paramString.equals("[]"))
            return null;
        String paramType = "";
        Integer paramIndex = 0;
        Integer paramDataType = 0;
        String paramFormat = "";
        String paramValue = "";
        JSONArray paramJsonArray = null;
        try {
            paramJsonArray = new JSONArray(paramString);
        } catch (Exception e) {
            final String errorString = "params you input can not convert to a JsonArray";
            getLogger().error(errorString);
            throw new ProcessException(errorString);
        }

        if (paramJsonArray != null && paramJsonArray.length() > 0) {
            Object[] paramsArray = new Object[paramJsonArray.length()];
            for (int i = 0; i < paramJsonArray.length(); i++) {
                JSONObject paramJsonObject = (JSONObject) paramJsonArray.get(i);
                paramIndex = i;
                try {
                    paramType = paramJsonObject.getString(PARAM_TYPE);
                    paramDataType = paramJsonObject.getInt(PARAM_VALUE_DATA_TYPE);
                    paramValue = paramJsonObject.getString(PARAM_VALUE);
                    paramFormat = paramJsonObject.getString(PARAM_FORMAT);
                } catch (Exception e) {
                    getLogger().info(e.getMessage());
                }
                getParam(paramIndex, paramType, paramDataType, paramFormat, paramValue, paramsArray);
            }
            return Arrays.asList(paramsArray);
        }
        return null;
    }

    public void getParam(Integer index, String paramType, Integer paramDataType, String paramFormat, String paramValue, Object[] params) {
        if (paramType.equals("IN")) {
            params[index] = getConvertJavaDataByType(paramDataType, paramFormat, paramValue);
        } else if (paramType.equals("IN/OUT") || paramType.equals("OUT")) {
            params[index] = getConvertTypeByType(paramDataType, paramFormat, paramValue, paramType);
        } else {
            final String errorString = paramType + " type do not support";
            getLogger().error(errorString);
            throw new ProcessException(errorString);
        }

    }

    public String GenerteCallQuery(String methodName, int paramNumber) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{").append("call ").append(methodName).append("(")
                .append(getPlaceHolders(paramNumber)).append(")").append("}");
        return stringBuilder.toString();
    }

    private String getPlaceHolders(int number) {
        StringBuilder stringBuilder = new StringBuilder();
        if (number > 0) {
            for (int i = 0; i < number; i++) {
                stringBuilder.append(",?");
            }
            return stringBuilder.toString().substring(1);
        }
        return "";

    }

    private String formatResults(List<List<GroovyRowResult>> list) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            throw new ProcessException(e);
        }
    }

    private String formatResults(Object[] args) {
        Map map = new HashMap();
        for (int i = 0; i < args.length; i++) {
            map.put("sql.args." + i, args[i]);
        }
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            throw new ProcessException(e);
        }
    }

    public static Object getConvertJavaDataByType(Integer paramDataType, String paramFormat, String parameterValue) {
        DateType dateType = DateType.getByValue(paramDataType);
        Object object = null;
        try {
            // return java date
            switch (dateType) {
                case BIT:
                    object = "1".equals(parameterValue) || "t".equalsIgnoreCase(parameterValue) || Boolean.parseBoolean(parameterValue);
                    break;
                case BOOLEAN:
                    object = Boolean.parseBoolean(parameterValue);
                    break;
                case TINYINT:
                    object = Byte.parseByte(parameterValue);
                    break;
                case SMALLINT:
                    object = Short.parseShort(parameterValue);
                    break;
                case INTEGER:
                    object = Integer.parseInt(parameterValue);
                    break;
                case BIGINT:
                    object = Long.parseLong(parameterValue);
                    break;
                case REAL:
                    object = Float.parseFloat(parameterValue);
                    break;
                case FLOAT:
                case DOUBLE:
                    object = Double.parseDouble(parameterValue);
                    break;
                case DECIMAL:
                case NUMERIC:
                    object = new BigDecimal(parameterValue);
                    break;
                case DATE:
                    java.sql.Date date;

                    if (StringUtils.isEmpty(paramFormat)) {
                        if (LONG_PATTERN.matcher(parameterValue).matches()) {
                            date = new java.sql.Date(Long.parseLong(parameterValue));
                        } else {
                            String dateFormatString = "yyyy-MM-dd";
                            SimpleDateFormat dateFormat = new SimpleDateFormat(dateFormatString);
                            Date parsedDate = dateFormat.parse(parameterValue);
                            date = new java.sql.Date(parsedDate.getTime());
                        }
                    } else {
                        final DateTimeFormatter dtFormatter = getDateTimeFormatter(paramFormat);
                        LocalDate parsedDate = LocalDate.parse(parameterValue, dtFormatter);
                        date = new java.sql.Date(java.sql.Date.from(parsedDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant()).getTime());
                    }

                    object = date;
                    break;
                case TIME:
                    Time time;

                    if (StringUtils.isEmpty(paramFormat)) {
                        if (LONG_PATTERN.matcher(parameterValue).matches()) {
                            time = new Time(Long.parseLong(parameterValue));
                        } else {
                            String timeFormatString = "HH:mm:ss.SSS";
                            SimpleDateFormat dateFormat = new SimpleDateFormat(timeFormatString);
                            Date parsedDate = dateFormat.parse(parameterValue);
                            time = new Time(parsedDate.getTime());
                        }
                    } else {
                        final DateTimeFormatter dtFormatter = getDateTimeFormatter(paramFormat);
                        LocalTime parsedTime = LocalTime.parse(parameterValue, dtFormatter);
                        LocalDateTime localDateTime = parsedTime.atDate(LocalDate.ofEpochDay(0));
                        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
                        time = new Time(instant.toEpochMilli());
                    }

                    object = time;
                    break;
                case TIMESTAMP:
                    long lTimestamp = 0L;

                    // Backwards compatibility note: Format was unsupported for a timestamp field.
                    if (StringUtils.isEmpty(paramFormat)) {
                        if (LONG_PATTERN.matcher(parameterValue).matches()) {
                            lTimestamp = Long.parseLong(parameterValue);
                        } else {
                            final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                            Date parsedDate = dateFormat.parse(parameterValue);
                            lTimestamp = parsedDate.getTime();
                        }
                    } else {
                        final DateTimeFormatter dtFormatter = getDateTimeFormatter(paramFormat);
                        TemporalAccessor accessor = dtFormatter.parse(parameterValue);
                        Date parsedDate = Date.from(Instant.from(accessor));
                        lTimestamp = parsedDate.getTime();
                    }

                    object = new Timestamp(lTimestamp);

                    break;
                case BINARY:
                case VARBINARY:
                case LONGVARBINARY:
                    byte[] bValue;
                    paramFormat = paramFormat == null ? "" : paramFormat;
                    switch (paramFormat.toLowerCase()) {
                        case "":
                        case "ascii":
                            bValue = parameterValue.getBytes("ASCII");
                            break;
                        case "hex":
                            bValue = DatatypeConverter.parseHexBinary(parameterValue);
                            break;
                        case "base64":
                            bValue = DatatypeConverter.parseBase64Binary(parameterValue);
                            break;
                        default:
                            throw new Exception("Unable to parse binary data using the formatter `" + paramFormat);
                    }

                    object = new ByteArrayInputStream(bValue);
                    break;
                case CHAR:
                case VARCHAR:
                case LONGVARCHAR:
                    object = parameterValue;
                    break;
                case CLOB:
                    object = new StringReader(parameterValue);
                    break;
                default:
                    object = parameterValue;
                    break;
            }
        } catch (Exception e) {
            final String errorString = "Unable to convert java data with your input" + parameterValue;
            //getLogger().error(errorString);
            throw new ProcessException(errorString);
        }

        return object;

    }

    private Object getConvertTypeByType(Integer paramDataType, String paramFormat, String paramValue, String type) {
        //eg:  INOUT  Sql.inout(Sql.VARCHAR("girl"))
        //eg:  OUT    Sql.VARCHAR
        if (type.equals("OUT")) {
            return DateType.getByValue(paramDataType).outParameter;
        } else if (type.equals("IN/OUT")) {
            return Sql.inout(Sql.in(DateType.getByValue(paramDataType).outParameter.getType(), paramValue));
        }
        return null;
    }

    public static DateTimeFormatter getDateTimeFormatter(String pattern) {
        switch (pattern.substring(0, pattern.indexOf("("))) {
            case "BASIC_ISO_DATE":
                return DateTimeFormatter.BASIC_ISO_DATE;
            case "ISO_LOCAL_DATE":
                return DateTimeFormatter.ISO_LOCAL_DATE;
            case "ISO_OFFSET_DATE":
                return DateTimeFormatter.ISO_OFFSET_DATE;
            case "ISO_DATE":
                return DateTimeFormatter.ISO_DATE;
            case "ISO_LOCAL_TIME":
                return DateTimeFormatter.ISO_LOCAL_TIME;
            case "ISO_OFFSET_TIME":
                return DateTimeFormatter.ISO_OFFSET_TIME;
            case "ISO_TIME":
                return DateTimeFormatter.ISO_TIME;
            case "ISO_LOCAL_DATE_TIME":
                return DateTimeFormatter.ISO_LOCAL_DATE_TIME;
            case "ISO_OFFSET_DATE_TIME":
                return DateTimeFormatter.ISO_OFFSET_DATE_TIME;
            case "ISO_ZONED_DATE_TIME":
                return DateTimeFormatter.ISO_ZONED_DATE_TIME;
            case "ISO_DATE_TIME":
                return DateTimeFormatter.ISO_DATE_TIME;
            case "ISO_ORDINAL_DATE":
                return DateTimeFormatter.ISO_ORDINAL_DATE;
            case "ISO_WEEK_DATE":
                return DateTimeFormatter.ISO_WEEK_DATE;
            case "ISO_INSTANT":
                return DateTimeFormatter.ISO_INSTANT;
            case "RFC_1123_DATE_TIME":
                return DateTimeFormatter.RFC_1123_DATE_TIME;
            default:
                return DateTimeFormatter.ofPattern(pattern);
        }
    }


}