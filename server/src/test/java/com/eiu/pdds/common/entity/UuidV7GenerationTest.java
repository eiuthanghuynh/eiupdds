package com.eiu.pdds.common.entity;

import com.eiu.pdds.diagnosis.entity.BoundingBox;
import com.eiu.pdds.disease.entity.PlantDisease;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UuidV7GenerationTest {

    // Concrete subclass to test BaseAuditEntity lifecycle
    static class DummyAuditEntity extends BaseAuditEntity {
    }

    @Test
    @DisplayName("ensureId - When ID is null, generates a valid RFC 9562 UUIDv7")
    void shouldGenerateVersion7UuidWhenPersistedWithNullId() throws Exception {
        DummyAuditEntity entity = new DummyAuditEntity();
        assertThat(entity.getId()).isNull();

        invokeEnsureId(entity);

        UUID generatedId = entity.getId();
        assertThat(generatedId).isNotNull();
        // RFC 9562 UUIDv7 specification: version bits = 7, variant bits = 2 (IETF)
        assertThat(generatedId.version())
                .as("UUID must be RFC 9562 version 7")
                .isEqualTo(7);
        assertThat(generatedId.variant())
                .as("UUID must have IETF variant (2)")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("ensureId - When ID is pre-assigned by client (offline-first), preserves the ID")
    void shouldPreservePreassignedIdWhenProvided() throws Exception {
        UUID offlineClientId = UUID.randomUUID();
        DummyAuditEntity entity = new DummyAuditEntity();
        entity.setId(offlineClientId);

        invokeEnsureId(entity);

        assertThat(entity.getId())
                .as("Pre-assigned offline client ID must not be overwritten")
                .isEqualTo(offlineClientId);
    }

    @Test
    @DisplayName("ensureId - Generates monotonically ordered UUIDs over time")
    void shouldGenerateMonotonicallyOrderedUuids() throws Exception {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            DummyAuditEntity entity = new DummyAuditEntity();
            invokeEnsureId(entity);
            ids.add(entity.getId());
            Thread.sleep(1); // 1ms delay to ensure monotonic millisecond epoch progression
        }

        for (int i = 0; i < ids.size() - 1; i++) {
            UUID id1 = ids.get(i);
            UUID id2 = ids.get(i + 1);
            assertThat(id1.version()).isEqualTo(7);
            assertThat(id2.version()).isEqualTo(7);
            assertThat(id1.compareTo(id2))
                    .as("Consecutive UUIDv7s should be ordered monotonically")
                    .isLessThan(0);
        }
    }

    @Test
    @DisplayName("BoundingBox - Generates UUIDv7 on ensureId")
    void shouldGenerateUuidV7ForBoundingBox() throws Exception {
        BoundingBox box = new BoundingBox();
        assertThat(box.getId()).isNull();

        invokeEnsureId(box);

        assertThat(box.getId()).isNotNull();
        assertThat(box.getId().version()).isEqualTo(7);
    }

    @Test
    @DisplayName("PlantDisease - Generates UUIDv7 on ensureId")
    void shouldGenerateUuidV7ForPlantDisease() throws Exception {
        PlantDisease plantDisease = new PlantDisease();
        assertThat(plantDisease.getId()).isNull();

        invokeEnsureId(plantDisease);

        assertThat(plantDisease.getId()).isNotNull();
        assertThat(plantDisease.getId().version()).isEqualTo(7);
    }

    private void invokeEnsureId(Object target) throws Exception {
        Method method = target.getClass().getMethod("ensureId");
        method.setAccessible(true);
        method.invoke(target);
    }
}
