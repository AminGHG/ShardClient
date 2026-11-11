package net.am7n.shardclient.protocol;

/** A message that cannot be read: truncated, oversized or carrying a value outside its bounds. */
public final class ProtocolException extends Exception {

    public ProtocolException(String message) {
        super(message);
    }
}
