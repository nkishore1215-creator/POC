package com.example.cartrecovery.entity;

import jakarta.persistence.Column;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AbandonedCartMappingTest {

    @Test
    void mapsAbandonedAtToRequiredLegacyDatabaseColumn() throws Exception {
        Field field = AbandonedCart.class.getDeclaredField("abandonedAt");
        Column column = field.getAnnotation(Column.class);

        assertNotNull(column);
        assertEquals("abandoned_time", column.name());
        assertFalse(column.nullable());
    }

    @Test
    void mapsReminderCountToAbandonedCartTableColumn() throws Exception {
        Field field = AbandonedCart.class.getDeclaredField("reminderCount");
        Column column = field.getAnnotation(Column.class);

        assertNotNull(column);
        assertEquals("reminder_count", column.name());
    }

    @Test
    void mapsLegacyRecoveryColumns() throws Exception {
        Field recoveredField =
                AbandonedCart.class.getDeclaredField("recovered");
        Field recoveredTimeField =
                AbandonedCart.class.getDeclaredField("recoveredTime");

        assertEquals(
                "recovered",
                recoveredField.getAnnotation(Column.class).name()
        );
        assertEquals(
                "recovered_time",
                recoveredTimeField.getAnnotation(Column.class).name()
        );
    }
}
