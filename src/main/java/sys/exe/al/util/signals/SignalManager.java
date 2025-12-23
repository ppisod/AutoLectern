package sys.exe.al.util.signals;

import java.util.EnumSet;
import java.util.Set;

public class SignalManager {
    private int signalBits = 0;

    public enum Signal {
        PROF(1),
        TRADE(1 << 1),
        TRADE_OK(1 << 2),
        ITEM(1 << 3);

        public final int bitMask;

        Signal(int bitMask) {
            this.bitMask = bitMask;
        }
    }

    public void signal (Signal signal) {
        signalBits |= signal.bitMask;
    }

    public void signal (Signal... signals) {
        for (Signal signal : signals) {
            signalBits |= signal.bitMask;
        }
    }

    public boolean isSet (Signal signal) {
        return (signalBits & signal.bitMask) != 0;
    }

    public boolean allSet (Signal... signals) {
        for (Signal signal : signals) {
            if (!isSet(signal)) return false;
        }
        return true;
    }

    public boolean anySet (Signal... signals) {
        for (Signal signal : signals) {
            if (isSet(signal)) return true;
        }
        return false;
    }

    public void clear (Signal... signals) {
        for (Signal signal : signals) {
            signalBits &= ~signal.bitMask;
        }
    }

    public void clearAll () {
        signalBits = 0;
    }

    public int getBits () {
        return signalBits;
    }

    public void setBits (int bits) {
        this.signalBits = bits;
    }

    public Set<Signal> getActiveSignals () {
        Set<Signal> active = EnumSet.noneOf(Signal.class);
        for (Signal signal : Signal.values()) {
            if (isSet(signal)) {
                active.add(signal);
            }
        }
        return active;
    }

    public boolean isEmpty () {
        return signalBits == 0;
    }
}