package utils.jsExecHelper.core.logging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TargetDescriptorTest {

    @Test
    void noneHasNoTargetFieldsAndImmutableMetadata() {
        TargetDescriptor target = TargetDescriptor.none();

        assertNull(target.selector());
        assertNull(target.label());
        assertNull(target.tagName());
        assertNull(target.text());
        assertTrue(target.metadata().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> target.metadata().put("x", "y"));
    }

    @Test
    void selectorSetsOnlySelector() {
        TargetDescriptor target = TargetDescriptor.selector("#save");

        assertEquals("#save", target.selector());
        assertNull(target.label());
    }

    @Test
    void labelSetsOnlyLabel() {
        TargetDescriptor target = TargetDescriptor.label("Save");

        assertEquals("Save", target.label());
        assertNull(target.selector());
    }

    @Test
    void withMetadataReturnsNewDescriptorAndKeepsOriginalUnchanged() {
        TargetDescriptor original = TargetDescriptor.selector("#save")
                .withMetadata("role", "button");

        TargetDescriptor changed = original.withMetadata("state", "enabled");

        assertEquals("button", original.metadata().get("role"));
        assertFalse(original.metadata().containsKey("state"));
        assertEquals("button", changed.metadata().get("role"));
        assertEquals("enabled", changed.metadata().get("state"));
        assertThrows(UnsupportedOperationException.class, () -> changed.metadata().put("x", "y"));
    }

    @Test
    void withMetadataIgnoresNullOrBlankInput() {
        TargetDescriptor target = TargetDescriptor.label("Save");

        assertSame(target, target.withMetadata(null, "value"));
        assertSame(target, target.withMetadata(" ", "value"));
        assertSame(target, target.withMetadata("key", null));
    }
}
