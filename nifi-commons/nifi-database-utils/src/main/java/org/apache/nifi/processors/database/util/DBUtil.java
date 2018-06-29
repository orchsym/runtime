package org.apache.nifi.processors.database.util;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.avro.Schema.Type;

public class DBUtil {

	public synchronized static void setValueForParam(PreparedStatement ps,
			int parameterIndex, Type fieldType, Object value) throws SQLException {
		if (fieldType.equals(Type.STRING) && value != null) {
			ps.setString(parameterIndex, value.toString());
		}else if(fieldType.equals(Type.BYTES) && value != null){
			ps.setBytes(parameterIndex, (byte[])value);
		}else if(fieldType.equals(Type.INT) && value != null){
			ps.setInt(parameterIndex, (Integer)value);
		}else if(fieldType.equals(Type.LONG) && value != null){
			ps.setLong(parameterIndex, (Long)value);
		}else if(fieldType.equals(Type.FLOAT) && value != null){
			ps.setFloat(parameterIndex, (float)value);
		}else if(fieldType.equals(Type.DOUBLE) && value != null){
			ps.setDouble(parameterIndex, (double)value);
		}else if(fieldType.equals(Type.BOOLEAN) && value != null){
			ps.setBoolean(parameterIndex, (Boolean)value);
		}
	}

}
