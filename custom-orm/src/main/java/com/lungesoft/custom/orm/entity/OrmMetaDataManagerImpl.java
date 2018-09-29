package com.lungesoft.custom.orm.entity;

import com.lungesoft.custom.orm.OrmManager;
import com.lungesoft.custom.orm.annotations.Column;
import com.lungesoft.custom.orm.annotations.FetchType;
import com.lungesoft.custom.orm.annotations.ManyToOne;
import com.lungesoft.custom.orm.annotations.Table;
import com.lungesoft.custom.orm.entity.meta.BeanFieldInfo;
import com.lungesoft.custom.orm.entity.meta.ManyToOneMetaInfo;
import com.lungesoft.custom.orm.entity.meta.Pair;
import com.lungesoft.custom.orm.entity.meta.TableMetaInfo;
import com.lungesoft.custom.orm.exeption.OrmMetadataException;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OrmMetaDataManagerImpl implements OrmMetaDataManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrmMetaDataManagerImpl.class);
    private final OrmManager ormManager;
    private Map<Class, TableMetaInfo> tableMetaInfoCache = new ConcurrentHashMap<>();

    public OrmMetaDataManagerImpl(OrmManager ormManager) {
        this.ormManager = ormManager;
    }

    @Override
    public <T> T initProxyObject(Class<T> objectClass) {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(objectClass);
        enhancer.setCallback((MethodInterceptor) (obj, method, args1, proxy) -> {
            if (method.getName().contains("get")) {
                ManyToOneMetaInfo manyToOneMetaInfo = findInfoByBeanMethod(method, getTableMetaInfo(objectClass).getManyToOneRows());
                if (manyToOneMetaInfo != null && !manyToOneMetaInfo.isEager()) {
                    Object object = proxy.invokeSuper(obj, args1);
                    if (object != null) {
                        BeanFieldInfo idFieldInfo = manyToOneMetaInfo.getTableMetaInfo().getIdRow().getSecond();
                        Object lazyLoad = ormManager.findById(method.getReturnType(), getFieldValue(idFieldInfo, object));
                        manyToOneMetaInfo.getBeanFieldInfo().getSetter().invoke(obj, lazyLoad);
                        return lazyLoad;
                    }
                }
            }
            return proxy.invokeSuper(obj, args1);
        });
        LOGGER.debug("init proxy object {}", objectClass.getSimpleName());
        return (T) enhancer.create();

    }

    private ManyToOneMetaInfo findInfoByBeanMethod(Method method, Map<String, ManyToOneMetaInfo> manyToOneMetaInfoMap) {
        for (String databaseColumn : manyToOneMetaInfoMap.keySet()) {
            ManyToOneMetaInfo manyToOneMetaInfo = manyToOneMetaInfoMap.get(databaseColumn);
            BeanFieldInfo beanFieldInfo = manyToOneMetaInfo.getBeanFieldInfo();
            if (beanFieldInfo.getGetter().equals(method) || beanFieldInfo.getSetter().equals(method)) {
                return manyToOneMetaInfo;
            }
        }
        return null;
    }

    private List<BeanFieldInfo> getFieldsInfo(Class<?> objectClass) {
        List<BeanFieldInfo> beanFieldInfoList = new ArrayList<>();
        try {
            for (PropertyDescriptor propertyDescriptor : Introspector.getBeanInfo(objectClass).getPropertyDescriptors()) {
                if (propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null) {
                    Field field = objectClass.getDeclaredField(propertyDescriptor.getName());
                    beanFieldInfoList.add(
                            new BeanFieldInfo(
                                    field,
                                    propertyDescriptor.getReadMethod(),
                                    propertyDescriptor.getWriteMethod())
                    );
                }
            }
        } catch (NoSuchFieldException | IntrospectionException e) {
            throw new OrmMetadataException(e);
        }
        return beanFieldInfoList;
    }

    @Override
    public TableMetaInfo getTableMetaInfo(Class<?> objectClass) {
        if (Enhancer.isEnhanced(objectClass)) {
            objectClass = objectClass.getSuperclass();
        }
        if (tableMetaInfoCache.containsKey(objectClass)) {
            return tableMetaInfoCache.get(objectClass);
        }

        if (objectClass.getAnnotation(Table.class) == null) {
            throw new OrmMetadataException("Table annotation not found");
        }

        TableMetaInfo tableMetaInfo = new TableMetaInfo();
        tableMetaInfo.setTableName(objectClass.getAnnotation(Table.class).name());
        tableMetaInfo.setBaseRows(new HashMap<>());
        tableMetaInfo.setManyToOneRows(new HashMap<>());

        List<BeanFieldInfo> beanFieldInfoList = getFieldsInfo(objectClass);

        for (BeanFieldInfo beanFieldInfo : beanFieldInfoList) {
            Column column = beanFieldInfo.getField().getAnnotation(Column.class);
            ManyToOne manyToOne = beanFieldInfo.getField().getAnnotation(ManyToOne.class);
            if (column != null) {
                if (!column.primaryKey()) {
                    tableMetaInfo.getBaseRows().put(column.name(), beanFieldInfo);
                } else {
                    tableMetaInfo.setIdRow(Pair.of(column.name(), beanFieldInfo));
                }
            } else if (manyToOne != null) {
                TableMetaInfo manyToOneTableInfo = getTableMetaInfo(beanFieldInfo.getGetter().getReturnType());
                tableMetaInfo.getManyToOneRows()
                        .put(manyToOne.fk(), new ManyToOneMetaInfo(manyToOneTableInfo, beanFieldInfo, manyToOne.fetchType() == FetchType.EAGER));
            }
        }

        if (beanFieldInfoList.size() > 0) {
            tableMetaInfoCache.put(objectClass, tableMetaInfo);
            return tableMetaInfo;
        } else {
            throw new OrmMetadataException("not mapped fields found");
        }
    }

    private List<String> getColumnsList(ResultSet rs) {
        List<String> columnsList = new ArrayList<>();
        try {
            ResultSetMetaData rsMetaData = rs.getMetaData();
            int columns = rsMetaData.getColumnCount();
            for (int x = 1; x <= columns; x++) {
                columnsList.add(rsMetaData.getColumnName(x).toLowerCase());
            }
        } catch (SQLException e) {
            throw new OrmMetadataException(e);
        }
        return columnsList;
    }


    @Override
    public <T> T fillResultSetToObject(ResultSet resultSet, T object) {
        Class<?> objectClass = Enhancer.isEnhanced(object.getClass())
                ? object.getClass().getSuperclass()
                : object.getClass();

        List<String> columnsList = getColumnsList(resultSet);
        TableMetaInfo tableMetaInfo = getTableMetaInfo(objectClass);
        try {
            Pair<String, BeanFieldInfo> idRow = tableMetaInfo.getIdRow();
            if (columnsList.contains(idRow.getFirst().toLowerCase())) {
                idRow.getSecond().getSetter().invoke(object, resultSet.getObject(idRow.getFirst().toLowerCase()));
            } else if (columnsList.size() == 1 && columnsList.contains("scope_identity()")) {
                idRow.getSecond().getSetter().invoke(object, resultSet.getObject("scope_identity()"));
                return object;
            }
            for (String baseRow : tableMetaInfo.getBaseRows().keySet()) {
                BeanFieldInfo beanFieldInfo = tableMetaInfo.getBaseRows().get(baseRow);
                if (columnsList.contains(baseRow.toLowerCase())) {
                    beanFieldInfo.getSetter().invoke(object, resultSet.getObject(baseRow.toLowerCase()));
                }
            }
            for (String manyToOneRow : tableMetaInfo.getManyToOneRows().keySet()) {
                ManyToOneMetaInfo manyToOneMetaInfo = tableMetaInfo.getManyToOneRows().get(manyToOneRow);
                BeanFieldInfo beanFieldInfo = manyToOneMetaInfo.getBeanFieldInfo();

                if (columnsList.contains(manyToOneRow.toLowerCase())) {
                    Class<?> subClass = beanFieldInfo.getGetter().getReturnType();
                    if (manyToOneMetaInfo.isEager()) {
                        beanFieldInfo.getSetter().invoke(object, ormManager.findById(subClass, resultSet.getObject(manyToOneRow.toLowerCase())));
                    } else {
                        Object proxy = initProxyObject(subClass);
                        BeanFieldInfo primaryField = getTableMetaInfo(subClass).getIdRow().getSecond();
                        primaryField.getSetter().invoke(proxy, resultSet.getObject(manyToOneRow.toLowerCase()));
                        beanFieldInfo.getSetter().invoke(object, proxy);
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | SQLException e) {
            throw new OrmMetadataException(e);
        }

        return object;
    }

    @Override
    public <T> T resultSetToObject(ResultSet resultSet, Class<T> objectClass) {
        T object = initProxyObject(objectClass);
        return fillResultSetToObject(resultSet, object);
    }

    @Override
    public void fillPreparedStatement(PreparedStatement preparedStatement, List<Object> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            Object value = values.get(i);
            int parameterIndex = i + 1;
            if (value instanceof Date) {
                preparedStatement.setDate(parameterIndex, new java.sql.Date((((Date) value).getTime())));
            } else {
                preparedStatement.setObject(parameterIndex, value);
            }
        }
    }

    @Override
    public Object getFieldValue(BeanFieldInfo beanFieldInfo, Object object) {
        try {
            return beanFieldInfo.getGetter().invoke(object);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new OrmMetadataException();
        }
    }


}
