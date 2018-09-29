package com.lungesoft.custom.orm.entity.criteria;

import java.util.List;

public interface Criteria {
    Where and();

    Where or();

    String getQuery();

    List<Object> getValues();

}
