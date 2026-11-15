package net.am7n.shardclient.full.media;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.ptr.PointerByReference;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Optional;

public final class WindowsMedia {
    private static final String MANAGER_CLASS = "Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager";
    private static final String STATICS_IID = "2050c4ee-11a0-57de-aed7-c97c70338245";
    private static final String ASYNC_INFO_IID = "00000036-0000-0000-c000-000000000046";

    private static final long FILETIME_EPOCH = 116_444_736_000_000_000L;
    private static final int PLAYING = 4;

    private final NativeLibrary combase;
    private Pointer manager;
    private boolean initialised;

    private WindowsMedia(NativeLibrary combase) {
        this.combase = combase;
    }

    public static Optional<WindowsMedia> create() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return Optional.empty();
        }
        try {
            return Optional.of(new WindowsMedia(NativeLibrary.getInstance("combase")));
        } catch (Throwable ex) {
            return Optional.empty();
        }
    }

    public Optional<NowPlaying> read() throws MediaException {
        if (!initialised) {
            call(combase.getFunction("RoInitialize"), 1);
            initialised = true;
        }
        if (manager == null) {
            manager = requestManager();
        }
        PointerByReference sessionRef = new PointerByReference();
        check(vcall(manager, 6, sessionRef), "GetCurrentSession");
        Pointer session = sessionRef.getValue();
        if (session == null) {
            return Optional.empty();
        }
        try {
            String app = hstring(session, 6);
            Pointer properties = await(callOut(session, 7, "TryGetMediaPropertiesAsync"));
            String title;
            String artist;
            String album;
            try {
                title = hstring(properties, 6);
                artist = hstring(properties, 9);
                album = hstring(properties, 10);
            } finally {
                release(properties);
            }
            Pointer timeline = callOut(session, 8, "GetTimelineProperties");
            long end;
            long position;
            long updated;
            try {
                end = int64(timeline, 7);
                position = int64(timeline, 10);
                updated = int64(timeline, 11);
            } finally {
                release(timeline);
            }
            Pointer playback = callOut(session, 9, "GetPlaybackInfo");
            int status;
            try {
                IntByReference value = new IntByReference();
                check(vcall(playback, 7, value), "get_PlaybackStatus");
                status = value.getValue();
            } finally {
                release(playback);
            }
            if (title == null || title.isBlank()) {
                return Optional.empty();
            }
            long updatedMillis = updated > 0 ? (updated - FILETIME_EPOCH) / 10_000 : System.currentTimeMillis();
            return Optional.of(new NowPlaying(app, title, artist == null ? "" : artist, album == null ? "" : album,
                    end / 10_000, position / 10_000, updatedMillis, status == PLAYING));
        } finally {
            release(session);
        }
    }

    private Pointer requestManager() throws MediaException {
        PointerByReference className = new PointerByReference();
        check(call(combase.getFunction("WindowsCreateString"), new WString(MANAGER_CLASS), MANAGER_CLASS.length(),
                className), "WindowsCreateString");
        PointerByReference statics = new PointerByReference();
        try {
            check(call(combase.getFunction("RoGetActivationFactory"), className.getValue(), guid(STATICS_IID), statics),
                    "RoGetActivationFactory");
        } finally {
            call(combase.getFunction("WindowsDeleteString"), className.getValue());
        }
        try {
            return await(callOut(statics.getValue(), 6, "RequestAsync"));
        } finally {
            release(statics.getValue());
        }
    }

    private Pointer await(Pointer operation) throws MediaException {
        try {
            PointerByReference infoRef = new PointerByReference();
            check(vcall(operation, 0, guid(ASYNC_INFO_IID), infoRef), "QueryInterface(IAsyncInfo)");
            Pointer info = infoRef.getValue();
            try {
                IntByReference status = new IntByReference();
                long deadline = System.nanoTime() + 2_000_000_000L;
                while (true) {
                    check(vcall(info, 7, status), "get_Status");
                    if (status.getValue() != 0) {
                        break;
                    }
                    if (System.nanoTime() > deadline) {
                        throw new MediaException("timed out");
                    }
                    Thread.sleep(5);
                }
                if (status.getValue() != 1) {
                    throw new MediaException("async status " + status.getValue());
                }
            } finally {
                release(info);
            }
            PointerByReference result = new PointerByReference();
            check(vcall(operation, 8, result), "GetResults");
            if (result.getValue() == null) {
                throw new MediaException("no result");
            }
            return result.getValue();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MediaException("interrupted");
        } finally {
            release(operation);
        }
    }

    private Pointer callOut(Pointer object, int slot, String what) throws MediaException {
        PointerByReference out = new PointerByReference();
        check(vcall(object, slot, out), what);
        if (out.getValue() == null) {
            throw new MediaException(what + " returned nothing");
        }
        return out.getValue();
    }

    private String hstring(Pointer object, int slot) throws MediaException {
        PointerByReference out = new PointerByReference();
        check(vcall(object, slot, out), "string slot " + slot);
        Pointer handle = out.getValue();
        if (handle == null) {
            return "";
        }
        try {
            IntByReference length = new IntByReference();
            Pointer chars = (Pointer) combase.getFunction("WindowsGetStringRawBuffer")
                    .invoke(Pointer.class, new Object[]{handle, length});
            return chars == null ? "" : new String(chars.getCharArray(0, length.getValue()));
        } finally {
            call(combase.getFunction("WindowsDeleteString"), handle);
        }
    }

    private long int64(Pointer object, int slot) throws MediaException {
        LongByReference out = new LongByReference();
        check(vcall(object, slot, out), "time slot " + slot);
        return out.getValue();
    }

    private static int vcall(Pointer object, int slot, Object... args) {
        Pointer vtable = object.getPointer(0);
        Function function = Function.getFunction(vtable.getPointer((long) slot * com.sun.jna.Native.POINTER_SIZE),
                Function.ALT_CONVENTION);
        Object[] all = new Object[args.length + 1];
        all[0] = object;
        System.arraycopy(args, 0, all, 1, args.length);
        return function.invokeInt(all);
    }

    private static int call(Function function, Object... args) {
        return function.invokeInt(args);
    }

    private static void release(Pointer object) {
        if (object != null) {
            vcall(object, 2);
        }
    }

    private static void check(int hresult, String what) throws MediaException {
        if (hresult < 0) {
            throw new MediaException(what + " failed: 0x" + Integer.toHexString(hresult));
        }
    }

    private static Memory guid(String text) {
        String hex = text.replace("-", "");
        ByteBuffer buffer = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt((int) Long.parseLong(hex.substring(0, 8), 16));
        buffer.putShort((short) Integer.parseInt(hex.substring(8, 12), 16));
        buffer.putShort((short) Integer.parseInt(hex.substring(12, 16), 16));
        for (int i = 16; i < 32; i += 2) {
            buffer.put((byte) Integer.parseInt(hex.substring(i, i + 2), 16));
        }
        Memory memory = new Memory(16);
        memory.write(0, buffer.array(), 0, 16);
        return memory;
    }

    public static final class MediaException extends Exception {
        public MediaException(String message) {
            super(message);
        }
    }
}
