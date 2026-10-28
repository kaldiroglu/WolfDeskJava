package dev.kaldiroglu.wolfdesk.tenants;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * An active record: an object that wraps a tenant's rows, encapsulates the database access,
 * and adds a little logic on that data (Fowler, quoted in chapter 5, p. 69).
 *
 * <p>Its state is open: public getters and setters, and lists that callers change directly.
 * The logic stays small, validating the values assigned to its fields.
 */
public class Tenant {

    private final TenantsDatabase db;
    private final UUID id;
    private String name;
    private final List<Product> products = new ArrayList<>();
    private final List<String> ticketCategories = new ArrayList<>();

    public Tenant(TenantsDatabase db) {
        this.db = db;
        this.id = UUID.randomUUID();
    }

    private Tenant(TenantsDatabase db, UUID id) {
        this.db = db;
        this.id = id;
    }

    public static Optional<Tenant> find(TenantsDatabase db, UUID id) {
        return db.selectTenant(id).map(row -> {
            var tenant = new Tenant(db, row.id());
            tenant.name = row.name();
            db.selectProducts(id).forEach(p -> tenant.products.add(new Product(p.id(), p.name())));
            db.selectCategories(id).forEach(c -> tenant.ticketCategories.add(c.name()));
            return tenant;
        });
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A tenant needs a name");
        }
        this.name = name.strip();
    }

    public List<Product> getProducts() {
        return products;
    }

    public List<String> getTicketCategories() {
        return ticketCategories;
    }

    /** Writes the tenant row, its product rows and its category rows. */
    public void save() {
        if (name == null) throw new IllegalStateException("A tenant needs a name");
        db.upsertTenant(new TenantsDatabase.TenantRow(id, name));
        db.replaceProducts(id, products.stream()
                .map(p -> new TenantsDatabase.ProductRow(p.getId(), id, p.getName())).toList());
        db.replaceCategories(id, ticketCategories.stream()
                .map(c -> new TenantsDatabase.CategoryRow(id, c)).toList());
    }

    public void delete() {
        db.deleteTenant(id);
    }
}
