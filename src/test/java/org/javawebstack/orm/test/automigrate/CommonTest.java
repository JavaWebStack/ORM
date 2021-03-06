package org.javawebstack.orm.test.automigrate;

import org.javawebstack.orm.ORM;
import org.javawebstack.orm.ORMConfig;
import org.javawebstack.orm.exception.ORMConfigurationException;
import org.javawebstack.orm.test.ORMTestCase;
import org.javawebstack.orm.test.shared.models.Datatype;
import org.javawebstack.orm.test.shared.verification.Field;
import org.javawebstack.orm.test.shared.verification.IdField;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class CommonTest extends ORMTestCase {

    private static final String tableName = "datatypes";
    private final Map<String, String> columnDataTypeMap;

    {
        columnDataTypeMap = new HashMap<>();

        columnDataTypeMap.put("primitive_boolean", "tinyint(1)");
        columnDataTypeMap.put("wrapper_boolean", "tinyint(1)");

        columnDataTypeMap.put("primitive_byte", "tinyint(4)");
        columnDataTypeMap.put("wrapper_byte", "tinyint(4)");

        columnDataTypeMap.put("primitive_short", "smallint(6)");
        columnDataTypeMap.put("wrapper_short", "smallint(6)");

        columnDataTypeMap.put("primitive_integer", "int(11)");
        columnDataTypeMap.put("wrapper_integer", "int(11)");

        columnDataTypeMap.put("primitive_long", "bigint(20)");
        columnDataTypeMap.put("wrapper_long", "bigint(20)");

        columnDataTypeMap.put("primitive_float", "float");
        columnDataTypeMap.put("wrapper_float", "float");

        columnDataTypeMap.put("primitive_double", "double");
        columnDataTypeMap.put("wrapper_double", "double");

        columnDataTypeMap.put("primitive_char", "char(1)");

        columnDataTypeMap.put("wrapper_string", "varchar(255)");

        columnDataTypeMap.put("char_array", "varchar(255)");

        columnDataTypeMap.put("byte_array", "varbinary(255)");

        columnDataTypeMap.put("timestamp", "timestamp");

        columnDataTypeMap.put("date", "date");

        columnDataTypeMap.put("uuid", "varchar(36)");

        columnDataTypeMap.put("option_enum", "enum('OPTION1','OPTION2')");
    }

    @BeforeEach
    public void setUp() throws ORMConfigurationException {
        ORMConfig config = new ORMConfig()
                .setDefaultSize(255);
        ORM.register(Datatype.class, sql(), config);
        ORM.autoMigrate(true);
    }

    @Test
    public void testId() throws SQLException {
        IdField.assertCorrectDatabaseFormat(tableName);
    }

    @Test
    public void testDatatypes() throws SQLException {
        Field checkedField;

        for(Map.Entry<String, String> entry : columnDataTypeMap.entrySet()) {
            checkedField = new Field(tableName, entry.getKey());
            checkedField.assertType(entry.getValue());
        }

    }

    @Test
    public void testNullable() throws SQLException {
        Field checkedField;

        for(Map.Entry<String, String> entry : columnDataTypeMap.entrySet()) {
            checkedField = new Field(tableName, entry.getKey());
            checkedField.assertNullable();
        }
    }

    @Test
    public void testNotPrimaryKey() throws SQLException {
        Field checkedField;

        for(Map.Entry<String, String> entry : columnDataTypeMap.entrySet()) {
            checkedField = new Field(tableName, entry.getKey());
            checkedField.assertNotPrimaryKey();
        }
    }

    @Test
    public void testNotAutoIncrement() throws SQLException {
        Field checkedField;

        for(Map.Entry<String, String> entry : columnDataTypeMap.entrySet()) {
            checkedField = new Field(tableName, entry.getKey());
            checkedField.assertNotAutoIncrementing();
        }
    }

}
