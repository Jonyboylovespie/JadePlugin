package jonyboylovespie.jade.plugin;

// NBT classes are in the same package

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

public final class PacketBuffer {

    public static int readVarInt(byte[] data, int[] offset) {
        int value = 0;
        int size = 0;
        int b;
        while (((b = data[offset[0]++] & 0xFF) & 0x80) == 0x80) {
            value |= (b & 0x7F) << (size++ * 7);
            if (size > 5) throw new RuntimeException("VarInt too big");
        }
        return value | ((b & 0x7F) << (size * 7));
    }

    public static byte[] writeVarInt(int value) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while ((value & ~0x7F) != 0) {
            baos.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        baos.write(value);
        return baos.toByteArray();
    }

    public static int readVarInt(ByteArrayInputStream in) {
        int value = 0;
        int size = 0;
        int b;
        while (((b = in.read()) & 0x80) == 0x80) {
            value |= (b & 0x7F) << (size++ * 7);
            if (size > 5) throw new RuntimeException("VarInt too big");
        }
        return value | ((b & 0x7F) << (size * 7));
    }

    public static void writeVarInt(ByteArrayOutputStream out, int value) {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }

    public static String readUtf(ByteArrayInputStream in) {
        int length = readVarInt(in);
        byte[] bytes = new byte[length];
        try {
            in.read(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeUtf(ByteArrayOutputStream out, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(out, bytes.length);
        try {
            out.write(bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static int[] readVarIntArray(ByteArrayInputStream in) {
        int length = readVarInt(in);
        int[] values = new int[length];
        for (int i = 0; i < length; i++) values[i] = readVarInt(in);
        return values;
    }

    public static void writeVarIntArray(ByteArrayOutputStream out, int[] values) {
        writeVarInt(out, values.length);
        for (int v : values) writeVarInt(out, v);
    }

    public static NbtCompound readNBT(byte[] data) {
        if (data.length == 0 || data[0] == 0) {
            return new NbtCompound();
        }
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
            int type = in.readUnsignedByte();
            return (NbtCompound) readPayload(type, in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read NBT", e);
        }
    }

    public static NbtCompound readNBT(ByteArrayInputStream bais) {
        try {
            int first = peek(bais);
            if (first == -1 || first == 0) {
                if (first == 0) bais.read();
                return new NbtCompound();
            }
            DataInputStream in = new DataInputStream(bais);
            int type = in.readUnsignedByte();
            return (NbtCompound) readPayload(type, in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read NBT", e);
        }
    }

    private static int peek(ByteArrayInputStream bais) {
        int pos = bais.available();
        if (pos == 0) return -1;
        bais.mark(1);
        int b = bais.read();
        bais.reset();
        return b;
    }

    public static byte[] writeNBT(NbtCompound tag) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeByte(tag.getType()); // 10
            writePayload(tag, out);
            out.flush();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write NBT", e);
        }
    }

    // Removed unused readNbtTag method


    private static NbtTag readPayload(int type, DataInput in) throws IOException {
        return switch (type) {
            case 1 -> new NbtByte(in.readByte());
            case 2 -> new NbtShort(in.readShort());
            case 3 -> new NbtInt(in.readInt());
            case 4 -> new NbtLong(in.readLong());
            case 5 -> new NbtFloat(in.readFloat());
            case 6 -> new NbtDouble(in.readDouble());
            case 7 -> { byte[] d = new byte[in.readInt()]; in.readFully(d); yield new NbtByteArray(d); }
            case 8 -> new NbtString(in.readUTF());
            case 9 -> {
                int listType = in.readByte();
                int size = in.readInt();
                var list = new ArrayList<NbtTag>(size);
                for (int i = 0; i < size; i++) list.add(readPayload(listType, in));
                yield new NbtList(listType, list);
            }
            case 10 -> {
                NbtCompound comp = new NbtCompound();
                int t;
                while ((t = in.readUnsignedByte()) != 0) {
                    String name = in.readUTF();
                    comp.put(name, readPayload(t, in));
                }
                yield comp;
            }
            case 11 -> { int[] d = new int[in.readInt()]; for (int i = 0; i < d.length; i++) d[i] = in.readInt(); yield new NbtIntArray(d); }
            case 12 -> { long[] d = new long[in.readInt()]; for (int i = 0; i < d.length; i++) d[i] = in.readLong(); yield new NbtLongArray(d); }
            default -> throw new IOException("Unknown NBT type: " + type);
        };
    }

    static void writeNbtTag(NbtCompound tag, DataOutput out) throws IOException {
        for (var entry : tag.entrySet()) {
            out.writeByte(entry.getValue().getType());
            out.writeUTF(entry.getKey());
            writePayload(entry.getValue(), out);
        }
        out.writeByte(0);
    }

    private static void writePayload(NbtTag tag, DataOutput out) throws IOException {
        switch (tag.getType()) {
            case 1 -> out.writeByte(((NbtByte) tag).value);
            case 2 -> out.writeShort(((NbtShort) tag).value);
            case 3 -> out.writeInt(((NbtInt) tag).value);
            case 4 -> out.writeLong(((NbtLong) tag).value);
            case 5 -> out.writeFloat(((NbtFloat) tag).value);
            case 6 -> out.writeDouble(((NbtDouble) tag).value);
            case 7 -> { byte[] d = ((NbtByteArray) tag).data; out.writeInt(d.length); out.write(d); }
            case 8 -> out.writeUTF(((NbtString) tag).value);
            case 9 -> {
                NbtList list = (NbtList) tag;
                out.writeByte(list.listType);
                out.writeInt(list.list.size());
                for (NbtTag t : list.list) writePayload(t, out);
            }
            case 10 -> writeNbtTag((NbtCompound) tag, out);
            case 11 -> { int[] d = ((NbtIntArray) tag).data; out.writeInt(d.length); for (int v : d) out.writeInt(v); }
            case 12 -> { long[] d = ((NbtLongArray) tag).data; out.writeInt(d.length); for (long v : d) out.writeLong(v); }
        }
    }

    private PacketBuffer() {}
}
