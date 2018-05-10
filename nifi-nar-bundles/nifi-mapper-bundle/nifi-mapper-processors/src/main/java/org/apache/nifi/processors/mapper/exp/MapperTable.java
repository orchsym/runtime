package org.apache.nifi.processors.mapper.exp;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.avro.Schema;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.processors.mapper.avro.DelegateJsonGenerator;
import org.apache.nifi.serialization.record.RecordSchema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Expression Table
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MapperTable {
    public static final String EMPTY_ARR = "[]";
    public static final String NAME_AVRO_SCHEMA = "avro_schema";
    /**
     * Unique id for each table
     */
    private String id;

    /**
     * name of table
     */
    private String name;

    /**
     * description of this table
     */
    private String desc;

    private MapperTableType type = MapperTableType.OUTPUT;

    /**
     * provide the reader or writer controller service
     */
    @JsonProperty("controller")
    private String controllerService;
    @JsonProperty("controller_desc")
    private String controllerDesc;

    /**
     * filter expression for table
     */
    private String filter;
    /**
     * filter expression for table
     */
    @JsonProperty("filter_desc")
    private String filterDescription;

    private List<MapperExpField> expressions;

    @JsonSerialize(using = SchemaSerializer.class)
    @JsonDeserialize(using = SchemaDeserializer.class)
    @JsonProperty("avro_schema")
    private Schema schema;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public MapperTableType getType() {
        return type;
    }

    public void setType(MapperTableType type) {
        this.type = type;
    }

    public String getControllerService() {
        return controllerService;
    }

    public void setControllerService(String controllerService) {
        this.controllerService = controllerService;
    }

    public String getControllerDesc() {
        return controllerDesc;
    }

    public void setControllerDesc(String controllerDesc) {
        this.controllerDesc = controllerDesc;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public String getFilterDescription() {
        return filterDescription;
    }

    public void setFilterDescription(String filterDescription) {
        this.filterDescription = filterDescription;
    }

    public List<MapperExpField> getExpressions() {
        if (expressions == null) {
            expressions = new ArrayList<>();
        }
        return expressions;
    }

    public void setExpressions(List<MapperExpField> expressions) {
        this.expressions = expressions;
    }

    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MapperTable other = (MapperTable) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ExpTable [id=" + id + ", name=" + name + ", type=" + type + "]";
    }

    static class SchemaDeserializer extends JsonDeserializer<Schema> {

        @Override
        public Schema deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            final JsonNode jsonNode = jp.getCodec().readTree(jp);
            final Schema schema = new Schema.Parser().parse(jsonNode.toString());
            return schema;
        }
    }

    static class SchemaSerializer extends JsonSerializer<Schema> {

        @Override
        public void serialize(Schema schema, JsonGenerator jgen, SerializerProvider serializers) throws IOException {
            DelegateJsonGenerator delegate = new DelegateJsonGenerator(jgen);
            try {
                final Class<?> namesClazz = Class.forName("org.apache.avro.Schema$Names", false, RecordSchema.class.getClassLoader());
                final Constructor<?> namesConstructor = namesClazz.getConstructor();
                namesConstructor.setAccessible(true);

                final Method toJsonMethod = schema.getClass().getDeclaredMethod("toJson", namesClazz, org.codehaus.jackson.JsonGenerator.class);
                toJsonMethod.setAccessible(true);
                toJsonMethod.invoke(schema, namesConstructor.newInstance(), delegate);
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

    }

    /**
     * 
     * parse the value to MappingTable
     *
     */
    public static class Parser {

        public MapperTable parseTable(String value) throws JsonProcessingException, IOException {
            if (StringUtils.isBlank(value)) {
                return null;
            }
            final ObjectMapper mapper = new ObjectMapper();
            // SimpleModule module = new SimpleModule();
            // module.addDeserializer(Schema.class, new SchemaDeserializer() );
            // mapper.registerModule(module);

            return mapper.readValue(value, MapperTable.class);
        }

        public Schema parseSchema(String value) throws JsonProcessingException, IOException {
            if (StringUtils.isBlank(value)) {
                return null;
            }
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode treeNode = mapper.readTree(value);

            // load schema
            final JsonNode avroSchemaNode = treeNode.get(NAME_AVRO_SCHEMA);
            final Schema schema = new Schema.Parser().parse(avroSchemaNode.toString());
            return schema;
        }

        public List<MapperTable> parseTables(String values) throws JsonProcessingException, IOException {
            final String arrStr = (StringUtils.isBlank(values)) ? EMPTY_ARR : values;

            final ObjectMapper mapper = new ObjectMapper();
            // JavaType javaType = mapper.getTypeFactory().constructParametricType(List.class, ExpTable.class, ExpField.class, ExpTableType.class);
            List<MapperTable> tables = mapper.readValue(arrStr, new TypeReference<List<MapperTable>>() {
            });
            return tables;
        }

    }

    /**
     * 
     * Writer the MappingTable to String.
     *
     */
    public static class Writer {

        public String write(MapperTable table) throws JsonProcessingException {
            if (table == null) {
                return null;
            }
            final ObjectMapper mapper = new ObjectMapper();

            // SimpleModule module = new SimpleModule();
            // module.addSerializer(Schema.class, new SchemaSerializer() );
            // mapper.registerModule(module);

            return mapper.writeValueAsString(table);
        }

    }
}
