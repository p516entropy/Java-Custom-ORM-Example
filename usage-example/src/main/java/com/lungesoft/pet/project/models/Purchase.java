package com.lungesoft.pet.project.models;

import com.lungesoft.custom.orm.annotations.Column;
import com.lungesoft.custom.orm.annotations.FetchType;
import com.lungesoft.custom.orm.annotations.ManyToOne;
import com.lungesoft.custom.orm.annotations.Table;

@Table(name = "purchase")
public class Purchase {

    @Column(name = "purchase_id", primaryKey = true)
    private int id;

    @ManyToOne(fk = "purchaser_id", fetchType = FetchType.LAZY)
    private User purchaser;

    @ManyToOne(fk = "item_id", fetchType = FetchType.LAZY)
    private Item item;

    @Column(name = "amount")
    private int amount;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public User getPurchaser() {
        return purchaser;
    }

    public void setPurchaser(User purchaser) {
        this.purchaser = purchaser;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "Purchase{" +
                "id=" + id +
                ", purchaser=" + purchaser +
                ", item=" + item +
                ", amount=" + amount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || (getClass() != o.getClass() && getClass() != o.getClass().getSuperclass())) return false;

        Purchase purchase = (Purchase) o;

        return id == purchase.id;

    }

    @Override
    public int hashCode() {
        return id;
    }
}
