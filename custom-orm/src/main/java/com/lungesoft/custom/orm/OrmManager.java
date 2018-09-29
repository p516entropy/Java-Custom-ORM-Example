package com.lungesoft.custom.orm;

import com.lungesoft.custom.orm.entity.criteria.Criteria;

import java.util.List;

public interface OrmManager {

    <T> T findById(Class<T> objectClass, Object key);

    <T> List<T> findAll(Class<T> objectClass);

    <T> List<T> findAll(Class<T> objectClass, Criteria criteria);

    <T> T create(T object);

    <T> boolean update(T object);

    <T> boolean delete(T object);


}