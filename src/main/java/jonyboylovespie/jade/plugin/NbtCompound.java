package jonyboylovespie.jade.plugin;

import java.nio.charset.StandardCharsets;
import java.util.*;

abstract class NbtTag {
    abstract int getType();
}

final class NbtByte extends NbtTag { byte value; NbtByte(byte v) { value = v; } int getType() { return 1; } }
final class NbtShort extends NbtTag { short value; NbtShort(short v) { value = v; } int getType() { return 2; } }
final class NbtInt extends NbtTag { int value; NbtInt(int v) { value = v; } int getType() { return 3; } }
final class NbtLong extends NbtTag { long value; NbtLong(long v) { value = v; } int getType() { return 4; } }
final class NbtFloat extends NbtTag { float value; NbtFloat(float v) { value = v; } int getType() { return 5; } }
final class NbtDouble extends NbtTag { double value; NbtDouble(double v) { value = v; } int getType() { return 6; } }
final class NbtByteArray extends NbtTag { byte[] data; NbtByteArray(byte[] d) { data = d; } int getType() { return 7; } }
final class NbtString extends NbtTag { String value; NbtString(String v) { value = v; } int getType() { return 8; } }
final class NbtList extends NbtTag { int listType; List<NbtTag> list; NbtList(int t, List<NbtTag> l) { listType = t; list = l; } int getType() { return 9; } }
final class NbtIntArray extends NbtTag { int[] data; NbtIntArray(int[] d) { data = d; } int getType() { return 11; } }
final class NbtLongArray extends NbtTag { long[] data; NbtLongArray(long[] d) { data = d; } int getType() { return 12; } }

public final class NbtCompound extends NbtTag {
    private final Map<String, NbtTag> tags = new LinkedHashMap<>();

    public NbtCompound() {}
    int getType() { return 10; }

    public void put(String key, NbtTag tag) { tags.put(key, tag); }
    public void putInt(String key, int value) { tags.put(key, new NbtInt(value)); }
    public void putString(String key, String value) { tags.put(key, new NbtString(value)); }
    public void putBoolean(String key, boolean value) { tags.put(key, new NbtByte((byte) (value ? 1 : 0))); }
    public void putFloat(String key, float value) { tags.put(key, new NbtFloat(value)); }
    public void putDouble(String key, double value) { tags.put(key, new NbtDouble(value)); }
    public void putLong(String key, long value) { tags.put(key, new NbtLong(value)); }
    public void putByteArray(String key, byte[] value) { tags.put(key, new NbtByteArray(value)); }
    public void putTag(String key, NbtTag tag) { put(key, tag); }

    public NbtTag get(String key) { return tags.get(key); }
    public boolean contains(String key) { return tags.containsKey(key); }
    public Set<Map.Entry<String, NbtTag>> entrySet() { return tags.entrySet(); }
    public boolean isEmpty() { return tags.isEmpty(); }

    public int getIntOr(String key, int def) {
        NbtTag t = tags.get(key);
        return t instanceof NbtInt i ? i.value : def;
    }

    public NbtCompound copy() {
        NbtCompound c = new NbtCompound();
        c.tags.putAll(tags);
        return c;
    }

    public int sizeInBytes() {
        int total = 0;
        for (var e : tags.entrySet()) {
            total += 1 + 2 + e.getKey().getBytes(StandardCharsets.UTF_8).length + tagSize(e.getValue());
        }
        return total + 1;
    }

    private static int tagSize(NbtTag tag) {
        return switch (tag.getType()) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 4;
            case 4 -> 8;
            case 5 -> 4;
            case 6 -> 8;
            case 7 -> 4 + ((NbtByteArray) tag).data.length;
            case 8 -> 2 + ((NbtString) tag).value.getBytes(StandardCharsets.UTF_8).length;
            case 9 -> 1 + 4;
            case 10 -> ((NbtCompound) tag).sizeInBytes();
            default -> 0;
        };
    }

    @Override
    public String toString() {
        return tags.toString();
    }
}
