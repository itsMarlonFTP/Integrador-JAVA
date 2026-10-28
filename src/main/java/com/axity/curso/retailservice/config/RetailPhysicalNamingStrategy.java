package com.axity.curso.retailservice.config;

import org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy;
import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

public class RetailPhysicalNamingStrategy extends CamelCaseToUnderscoresNamingStrategy {

    @Override
    public Identifier toPhysicalTableName(Identifier logicalName, JdbcEnvironment jdbcEnvironment) {
        return logicalName.isQuoted()
                ? logicalName
                : super.toPhysicalTableName(logicalName, jdbcEnvironment);
    }
}
