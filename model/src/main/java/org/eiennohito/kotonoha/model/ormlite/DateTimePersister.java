/*
 * Copyright 2012 eiennohito
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eiennohito.kotonoha.model.ormlite;

/**
 * @author eiennohito
 * @since 07.02.12
 */
import com.j256.ormlite.field.FieldType;
import com.j256.ormlite.field.SqlType;
import com.j256.ormlite.field.types.BaseDataType;
import com.j256.ormlite.support.DatabaseResults;
import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;

import java.lang.reflect.Field;
import java.sql.SQLException;

public class DateTimePersister extends BaseDataType {
	private static final DateTimePersister singleton = new DateTimePersister();

	public static DateTimePersister getSingleton() {
		return singleton;
	}

	public DateTimePersister() {
		super(null, new Class[] { DateTime.class });
	}
	@Override
	public Object parseDefaultString(FieldType fieldType, String defaultStr) throws SQLException {
		throw new SQLException("Default string doesn't work");
	}
	@Override
	public Object resultToJava(FieldType fieldType, DatabaseResults results, int columnPos) throws SQLException {
		Long value = results.getLong(columnPos);
		return sqlArgToJava(fieldType, value, columnPos);
	}
	@Override
	public Object sqlArgToJava(FieldType fieldType, Object sqlArg, int columnPos) throws SQLException {
		Long value = (Long) sqlArg;
		return new DateTime(value);
	}
	@Override
	public Object javaToSqlArg(FieldType fieldType, Object javaObject) throws SQLException {
		if (javaObject == null) {
			return null;
		} else {
			return ((ReadableInstant) javaObject).getMillis();
		}
	}
	@Override
	public boolean isValidForField(Field field) {
		return field.getType() == DateTime.class;
	}
	@Override
	public SqlType getSqlType() {
		return SqlType.LONG;
	}
}
