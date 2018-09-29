package com.lungesoft.custom.orm;

import com.lungesoft.custom.orm.entity.OrmMetaDataManager;
import com.lungesoft.custom.orm.entity.OrmMetaDataManagerImpl;
import com.lungesoft.custom.orm.entity.criteria.Criteria;
import com.lungesoft.custom.orm.entity.meta.BeanFieldInfo;
import com.lungesoft.custom.orm.entity.meta.ManyToOneMetaInfo;
import com.lungesoft.custom.orm.entity.meta.TableMetaInfo;
import com.lungesoft.custom.orm.exeption.OrmManagerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OrmManagerImpl implements OrmManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrmManagerImpl.class);
    private DataSource dataSource;
    private OrmMetaDataManager ormMetaDataManager = new OrmMetaDataManagerImpl(this);

    public OrmManagerImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public <T> T findById(Class<T> objectClass, Object key) {
        TableMetaInfo tableInfo = ormMetaDataManager.getTableMetaInfo(objectClass);
        String sql = " SELECT * FROM " + tableInfo.getTableName() + " WHERE " + tableInfo.getIdRow().getFirst() + " = ?;";

        List<T> searchResult = findAll(objectClass, sql, Collections.singletonList(key));
        if (searchResult.size() == 0) {
            throw new OrmManagerException("Entity not found");
        }
        if (searchResult.size() > 2) {
            throw new OrmManagerException("Bad id");
        }
        return searchResult.get(0);
    }

    @Override
    public <T> List<T> findAll(Class<T> objectClass) {
        TableMetaInfo tableInfo = ormMetaDataManager.getTableMetaInfo(objectClass);
        String sql = " SELECT * FROM " + tableInfo.getTableName() + ";";
        return findAll(objectClass, sql, Collections.emptyList());
    }

    @Override
    public <T> List<T> findAll(Class<T> objectClass, Criteria criteria) {
        TableMetaInfo tableInfo = ormMetaDataManager.getTableMetaInfo(objectClass);
        String sql = " SELECT * FROM " + tableInfo.getTableName() + criteria.getQuery() + ";";
        return findAll(objectClass, sql, criteria.getValues());
    }

    private <T> List<T> findAll(Class<T> objectClass, String sql, List<Object> params) {
        List<T> result = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            ormMetaDataManager.fillPreparedStatement(preparedStatement, params);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    result.add(ormMetaDataManager.resultSetToObject(resultSet, objectClass));
                }
            }
            LOGGER.debug("exec sql {}", sql);
        } catch (SQLException e) {
            throw new OrmManagerException(e);
        }
        return result;
    }

    @Override
    public <T> T create(T object) {
        TableMetaInfo tableInfo = ormMetaDataManager.getTableMetaInfo(object.getClass());

        List<Object> values = new ArrayList<>();
        StringBuilder sqlRows = new StringBuilder();

        for (Map.Entry<String, BeanFieldInfo> entry : tableInfo.getBaseRows().entrySet()) {
            sqlRows.append(entry.getKey()).append(", ");
            values.add(ormMetaDataManager.getFieldValue(entry.getValue(), object));
        }

        for (Map.Entry<String, ManyToOneMetaInfo> entry : tableInfo.getManyToOneRows().entrySet()) {
            sqlRows.append(entry.getKey()).append(", ");
            Object manyToOneObject = ormMetaDataManager.getFieldValue(entry.getValue().getBeanFieldInfo(), object);
            values.add(ormMetaDataManager.getFieldValue(entry.getValue().getTableMetaInfo().getIdRow().getSecond(), manyToOneObject));
        }

        StringBuilder sqlQuestionValues = new StringBuilder();
        for (int i = 0; i < tableInfo.getBaseRows().size() + tableInfo.getManyToOneRows().size() - 1; i++) {
            sqlQuestionValues.append("?, ");
        }
        sqlQuestionValues.append("?");

        String sql;
        if (sqlRows.length() > 3) {
            sql = "INSERT INTO " + tableInfo.getTableName() + " (" + sqlRows.substring(0, sqlRows.length() - 2) + ") " +
                    " VALUES (" + sqlQuestionValues + " );";
        } else {
            throw new OrmManagerException("Could not create sql INSERT query");
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ormMetaDataManager.fillPreparedStatement(preparedStatement, values);
            preparedStatement.executeUpdate();
            LOGGER.debug("exec sql {}", sql);

            try (ResultSet generatedKeys = preparedStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return findById((Class<? extends T>) object.getClass(), generatedKeys.getObject("scope_identity()"));
                } else {
                    throw new OrmManagerException("Nothing to create");
                }
            }
        } catch (SQLException e) {
            throw new OrmManagerException(e);
        }
    }

    @Override
    public <T> boolean update(T object) {
        TableMetaInfo tableInfo = ormMetaDataManager.getTableMetaInfo(object.getClass());

        StringBuilder set = new StringBuilder();
        List<Object> values = new ArrayList<>();

        for (Map.Entry<String, BeanFieldInfo> entry : tableInfo.getBaseRows().entrySet()) {
            set.append(entry.getKey()).append(" = ?, ");
            values.add(ormMetaDataManager.getFieldValue(entry.getValue(), object));
        }

        for (Map.Entry<String, ManyToOneMetaInfo> entry : tableInfo.getManyToOneRows().entrySet()) {
            set.append(entry.getKey()).append(" = ?, ");
            Object manyToOneObject = ormMetaDataManager.getFieldValue(entry.getValue().getBeanFieldInfo(), object);
            values.add(ormMetaDataManager.getFieldValue(entry.getValue().getTableMetaInfo().getIdRow().getSecond(), manyToOneObject));
        }

        String sql;
        if (tableInfo.getIdRow() != null && set.length() > 3) {
            sql = "UPDATE " + tableInfo.getTableName() +
                    " SET " + set.substring(0, set.length() - 2) +
                    " WHERE " + tableInfo.getIdRow().getFirst() + " = ?;";
            values.add(ormMetaDataManager.getFieldValue(tableInfo.getIdRow().getSecond(), object));

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
                ormMetaDataManager.fillPreparedStatement(preparedStatement, values);
                if (preparedStatement.executeUpdate() != 0) {
                    LOGGER.debug("exec sql {}", sql);
                    return true;
                }
            } catch (SQLException e) {
                throw new OrmManagerException(e);
            }
        }
        return false;

    }

    @Override
    public <T> boolean delete(T object) {

        TableMetaInfo tableInfo = ormMetaDataManager.getTableMetaInfo(object.getClass());

        String sql = "DELETE FROM " + tableInfo.getTableName() +
                " WHERE " + tableInfo.getIdRow().getFirst() + " = ? ;";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            if (tableInfo.getIdRow() != null) {
                preparedStatement.setObject(1, tableInfo.getIdRow().getSecond().getGetter().invoke(object));
                if (preparedStatement.executeUpdate() != 0) {
                    LOGGER.debug("exec sql {}", sql);
                    return true;
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | SQLException e) {
            throw new OrmManagerException(e);
        }

        return false;
    }

}
