CREATE TABLE user (
    user_id INTEGER AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    login VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL
);

CREATE TABLE item (
    item_id INTEGER AUTO_INCREMENT PRIMARY KEY,
    seller_id INT NOT NULL,
    title VARCHAR(100) NOT NULL,
    description VARCHAR(200) NOT NULL,
    price REAL NOT NULL
);


CREATE TABLE purchase (
    purchase_id INTEGER AUTO_INCREMENT PRIMARY KEY,
    purchaser_id INT NOT NULL,
    item_id INT NOT NULL,
    amount INT NOT NULL
);


ALTER TABLE item
    ADD FOREIGN KEY (seller_id) REFERENCES user (user_id) ON DELETE CASCADE;

ALTER TABLE purchase
    ADD FOREIGN KEY (purchaser_id) REFERENCES user (user_id) ON DELETE CASCADE;

ALTER TABLE purchase
    ADD FOREIGN KEY (item_id) REFERENCES item (item_id) ON DELETE CASCADE;

