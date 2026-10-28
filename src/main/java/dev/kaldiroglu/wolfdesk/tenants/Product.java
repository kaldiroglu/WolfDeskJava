package dev.kaldiroglu.wolfdesk.tenants;

import java.util.UUID;

/** A product the tenant supports. Saved as a row of the products table by its tenant. */
public class Product {

    private final UUID id;
    private String name;

    public Product(String name) {
        this(UUID.randomUUID(), name);
    }

    Product(UUID id, String name) {
        this.id = id;
        setName(name);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A product needs a name");
        }
        this.name = name.strip();
    }
}
