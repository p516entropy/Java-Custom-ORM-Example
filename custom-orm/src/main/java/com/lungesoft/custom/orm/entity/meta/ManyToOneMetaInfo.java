package com.lungesoft.custom.orm.entity.meta;

public class ManyToOneMetaInfo {

    private final TableMetaInfo tableMetaInfo;
    private final BeanFieldInfo beanFieldInfo;
    private final boolean isEager;

    public ManyToOneMetaInfo(TableMetaInfo tableMetaInfo, BeanFieldInfo beanFieldInfo, boolean isEager) {
        this.tableMetaInfo = tableMetaInfo;
        this.beanFieldInfo = beanFieldInfo;
        this.isEager = isEager;
    }

    public TableMetaInfo getTableMetaInfo() {
        return tableMetaInfo;
    }

    public BeanFieldInfo getBeanFieldInfo() {
        return beanFieldInfo;
    }

    public boolean isEager() {
        return isEager;
    }
}