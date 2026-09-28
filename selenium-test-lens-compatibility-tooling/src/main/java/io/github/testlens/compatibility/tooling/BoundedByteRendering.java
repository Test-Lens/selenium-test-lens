package io.github.testlens.compatibility.tooling;

import java.io.IOException;
import java.io.OutputStream;

/** Two-pass exact-size rendering avoids retaining a growable buffer and its final copy together. */
final class BoundedByteRendering {
    private BoundedByteRendering() { }

    static byte[] render(long maximum, Renderer renderer) throws IOException {
        if (maximum < 0 || maximum > Integer.MAX_VALUE) throw new IllegalArgumentException("invalid output bound");
        CountingOutput count = new CountingOutput(maximum);
        renderer.render(count);
        byte[] result = new byte[(int) count.count];
        FixedOutput output = new FixedOutput(result);
        renderer.render(output);
        if (output.position != result.length) throw new IOException("NON_DETERMINISTIC_OUTPUT_SIZE");
        return result;
    }

    @FunctionalInterface interface Renderer { void render(OutputStream output) throws IOException; }

    static final class LimitExceeded extends IOException {
        LimitExceeded() { super("OUTPUT_LIMIT_EXCEEDED"); }
    }

    private static final class CountingOutput extends OutputStream {
        private final long maximum; private long count;
        private CountingOutput(long maximum) { this.maximum = maximum; }
        @Override public void write(int value) throws IOException { add(1); }
        @Override public void write(byte[] value, int offset, int length) throws IOException { add(length); }
        private void add(int length) throws IOException { if (count + length > maximum) throw new LimitExceeded(); count += length; }
    }

    private static final class FixedOutput extends OutputStream {
        private final byte[] target; private int position;
        private FixedOutput(byte[] target) { this.target = target; }
        @Override public void write(int value) throws IOException { ensure(1); target[position++] = (byte) value; }
        @Override public void write(byte[] value, int offset, int length) throws IOException { ensure(length); System.arraycopy(value, offset, target, position, length); position += length; }
        private void ensure(int length) throws IOException { if (position + length > target.length) throw new IOException("NON_DETERMINISTIC_OUTPUT_SIZE"); }
    }
}
