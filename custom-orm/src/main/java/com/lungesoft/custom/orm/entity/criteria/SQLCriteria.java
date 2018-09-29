package com.lungesoft.custom.orm.entity.criteria;

import java.util.ArrayList;
import java.util.List;

class SQLCriteria implements Criteria {

    private StringBuilder sql = new StringBuilder();
    private List<Object> values = new ArrayList<>();

    SQLCriteria() {
    }

    @Override
    public Where and() {
        sql.append(" AND ");
        return new SQLWhere(this);
    }

    @Override
    public Where or() {
        sql.append(" OR ");
        return new SQLWhere(this);
    }


    public StringBuilder getSql() {
        return sql;
    }

    public String getQuery() {
        return " WHERE " + sql;
    }

    @Override
    public List<Object> getValues() {
        return values;
    }


}
