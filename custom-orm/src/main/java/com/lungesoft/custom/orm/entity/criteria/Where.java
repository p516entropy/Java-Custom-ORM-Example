package com.lungesoft.custom.orm.entity.criteria;

import java.util.Date;
import java.util.List;

public interface Where {

    Criteria equal(String column, Object value);

    Criteria like(String column, String value);

    Criteria more(String column, int value);

    Criteria less(String column, int value);

    Criteria more(String column, double value);

    Criteria less(String column, double value);

    Criteria more(String column, Date value);

    Criteria less(String column, Date value);

    <T> Criteria in(String column, List<T> values);
}
