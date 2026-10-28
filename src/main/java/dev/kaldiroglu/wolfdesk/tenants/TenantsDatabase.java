package dev.kaldiroglu.wolfdesk.tenants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The data access layer the active records are coupled to. It mimics three tables, tenants,
 * products and ticket categories, with maps. A transaction works on a copy of the tables;
 * commit keeps the copy, rollback throws it away.
 */
public class TenantsDatabase {

    record TenantRow(UUID id, String name) {
    }

    record ProductRow(UUID id, UUID tenantId, String name) {
    }

    record CategoryRow(UUID tenantId, String name) {
    }

    private static final class Tables {
        final Map<UUID, TenantRow> tenants = new HashMap<>();
        final Map<UUID, ProductRow> products = new HashMap<>();
        final List<CategoryRow> categories = new ArrayList<>();

        Tables copy() {
            var copy = new Tables();
            copy.tenants.putAll(tenants);
            copy.products.putAll(products);
            copy.categories.addAll(categories);
            return copy;
        }
    }

    private Tables committed = new Tables();
    private Tables working;

    public void startTransaction() {
        if (working != null) throw new IllegalStateException("A transaction is already open");
        working = committed.copy();
    }

    public void commit() {
        committed = tables();
        working = null;
    }

    public void rollback() {
        working = null;
    }

    private Tables tables() {
        return working != null ? working : committed;
    }

    Optional<TenantRow> selectTenant(UUID id) {
        return Optional.ofNullable(tables().tenants.get(id));
    }

    List<ProductRow> selectProducts(UUID tenantId) {
        return tables().products.values().stream()
                .filter(p -> p.tenantId().equals(tenantId)).toList();
    }

    List<CategoryRow> selectCategories(UUID tenantId) {
        return tables().categories.stream().filter(c -> c.tenantId().equals(tenantId)).toList();
    }

    void upsertTenant(TenantRow row) {
        tables().tenants.put(row.id(), row);
    }

    void replaceProducts(UUID tenantId, List<ProductRow> rows) {
        tables().products.values().removeIf(p -> p.tenantId().equals(tenantId));
        rows.forEach(r -> tables().products.put(r.id(), r));
    }

    void replaceCategories(UUID tenantId, List<CategoryRow> rows) {
        tables().categories.removeIf(c -> c.tenantId().equals(tenantId));
        tables().categories.addAll(rows);
    }

    void deleteTenant(UUID tenantId) {
        replaceProducts(tenantId, List.of());
        replaceCategories(tenantId, List.of());
        tables().tenants.remove(tenantId);
    }
}
