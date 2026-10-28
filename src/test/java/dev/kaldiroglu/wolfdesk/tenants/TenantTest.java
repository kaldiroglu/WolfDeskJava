package dev.kaldiroglu.wolfdesk.tenants;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantTest {

    private TenantsDatabase db;

    @BeforeEach
    void setUp() {
        db = new TenantsDatabase();
    }

    @Test
    void aRegisteredTenantIsFoundWithItsCategories() {
        var id = new RegisterTenant(db).execute("Acme", List.of("Billing", "Technical"));

        var tenant = Tenant.find(db, id).orElseThrow();
        assertEquals("Acme", tenant.getName());
        assertEquals(List.of("Billing", "Technical"), tenant.getTicketCategories());
    }

    @Test
    void aSupportedProductIsSavedWithItsTenant() {
        var id = new RegisterTenant(db).execute("Acme", List.of());

        new AddSupportedProduct(db).execute(id, "Acme Mobile");

        var names = Tenant.find(db, id).orElseThrow().getProducts().stream().map(Product::getName).toList();
        assertEquals(List.of("Acme Mobile"), names);
    }

    @Test
    void aBlankNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RegisterTenant(db).execute(" ", List.of()));
    }

    @Test
    void anUnknownTenantRollsTheTransactionBack() {
        var script = new AddSupportedProduct(db);

        assertThrows(IllegalArgumentException.class, () -> script.execute(UUID.randomUUID(), "X"));
        // The transaction was rolled back, so a new one can start.
        assertDoesNotThrow(() -> new RegisterTenant(db).execute("Acme", List.of()));
    }

    @Test
    void changesThatAreRolledBackAreNotKept() {
        var id = new RegisterTenant(db).execute("Acme", List.of("Billing"));
        db.startTransaction();
        var tenant = Tenant.find(db, id).orElseThrow();
        tenant.getTicketCategories().add("Sales");
        tenant.save();
        db.rollback();

        assertEquals(List.of("Billing"), Tenant.find(db, id).orElseThrow().getTicketCategories());
    }

    @Test
    void aDeletedTenantIsGone() {
        var id = new RegisterTenant(db).execute("Acme", List.of("Billing"));

        Tenant.find(db, id).orElseThrow().delete();

        assertTrue(Tenant.find(db, id).isEmpty());
    }
}
