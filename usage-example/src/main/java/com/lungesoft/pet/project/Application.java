package com.lungesoft.pet.project;

import com.lungesoft.pet.project.models.Item;
import com.lungesoft.pet.project.models.Purchase;
import com.lungesoft.pet.project.models.User;
import com.lungesoft.custom.orm.OrmManager;
import com.lungesoft.custom.orm.OrmManagerImpl;
import com.lungesoft.custom.orm.entity.criteria.SQLWhere;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.io.IOUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Application {

    public static void main(String[] args) {
        new Application().run();
    }

    private void run() {
        DataSource dataSource = createH2DataSource();
        initDDL(dataSource);
        OrmManager ormManager = new OrmManagerImpl(dataSource);

        createInitData(ormManager);
        findAllCriteriaLike(ormManager);
        findAllCriteriaEqualAndLike(ormManager);
        findAllCriteriaIn(ormManager);
        updateAndDelete3idItem(ormManager);
        findAllItemsWithLazyLoading(ormManager);
    }

    private void createInitData(OrmManager ormManager) {
        System.out.println("createInitData");
        List<User> users = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            User user = new User("name" + i, "login" + i, "password" + i);
            users.add(ormManager.create(user));
        }
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            Item item = new Item();
            item.setDescription("description" + i);
            item.setPrice(new Random().nextDouble());
            item.setTitle("title" + i);
            item.setSeller(users.get(new Random().nextInt(users.size())));
            items.add(ormManager.create(item));
        }
        for (int i = 0; i < 20; i++) {
            Purchase purchase = new Purchase();
            purchase.setAmount(new Random().nextInt(10));
            purchase.setItem(items.get(new Random().nextInt(items.size())));
            purchase.setPurchaser(users.get(new Random().nextInt(users.size())));
            ormManager.create(purchase);
        }

        for (User user : ormManager.findAll(User.class)) {
            System.out.println(user);
        }
        for (Item item : ormManager.findAll(Item.class)) {
            System.out.println(item);
        }
        for (Purchase purchase : ormManager.findAll(Purchase.class)) {
            System.out.println(purchase);
        }
    }

    private void findAllCriteriaLike(OrmManager ormManager) {
        System.out.println("findAll like(\"full_name\", \"%4\")");
        for (User user : ormManager.findAll(User.class, new SQLWhere().like("full_name", "%4"))) {
            System.out.println(user);
        }
        System.out.println();
    }

    private void findAllCriteriaEqualAndLike(OrmManager ormManager) {
        System.out.println("findAll equal(\"login\", \"login7\").and().like(\"password\", \"%7\")");
        for (User user : ormManager.findAll(User.class, new SQLWhere().equal("login", "login7").and().like("password", "%7"))) {
            System.out.println(user);
        }
        System.out.println();
    }

    private void findAllCriteriaIn(OrmManager ormManager) {
        System.out.println("findAll in(\"ITEM_ID\", Arrays.asList(1,2,3))");
        for (Item item : ormManager.findAll(Item.class, new SQLWhere().in("ITEM_ID", Arrays.asList(1, 2, 3)))) {
            System.out.println(item);
        }
        System.out.println();
    }

    private void updateAndDelete3idItem(OrmManager ormManager) {
        System.out.println("update item 3");
        Item itemToUpdate = ormManager.findById(Item.class, 3);
        itemToUpdate.setDescription("new Description");
        System.out.println(ormManager.update(itemToUpdate));
        System.out.println(ormManager.findById(Item.class, 3));

        System.out.println("delete item 3");
        System.out.println(ormManager.delete(itemToUpdate));
    }

    private void findAllItemsWithLazyLoading(OrmManager ormManager) {
        for (Item item : ormManager.findAll(Item.class)) {
            System.out.println();
            System.out.println(item);
            System.out.println(item.getSeller());
            System.out.println(item);
        }
    }

    private void initDDL(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(IOUtils.toString(this.getClass().getResourceAsStream("/script.sql")));
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }


    private DataSource createH2DataSource() {
        HikariConfig config = new HikariConfig();
        config.setDriverClassName("org.h2.Driver");
        config.setJdbcUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
        config.setUsername("sa");
        config.setPassword("");
        return new HikariDataSource(config);

    }
}
