package ticketingsystem;

import java.util.concurrent.atomic.AtomicStampedReference;

public class SeatOperation {
    private int stationnum;
    private int[][][] ramainseats;
    public long[][] Bitmask;
    private AtomicStampedReference<Integer> pointer;

    public SeatOperation(int allseatnum, int stationnum) {
        this.stationnum = stationnum;
        this.ramainseats = new int[2][stationnum][stationnum];
        for (int i = 0; i < stationnum; i++) {
            for (int j = 0; j < i + 1; j++) {
                this.ramainseats[0][i][j] = 0;
                this.ramainseats[1][i][j] = 0;
            }
            for (int j = i + 1; j < stationnum; j++) {
                this.ramainseats[0][i][j] = allseatnum;
                this.ramainseats[1][i][j] = allseatnum;
            }
        }
        this.Bitmask = new long[stationnum][stationnum];
        for (int i = 0; i < stationnum; i++) {
            for (int j = i + 1; j < stationnum; j++) {
                this.Bitmask[i][j] = ((0x1 << (j - i)) - 1) << (stationnum - j - 1);
            }
        }
        this.pointer = new AtomicStampedReference<>(0, 0);
    }

    public int getCurrentSeatsNum(int departure, int arrival) {
        if (departure >= arrival)
            return 0;
        int curpointer = pointer.getReference();
        int stamp = pointer.getStamp();
        int result = ramainseats[curpointer][departure - 1][arrival - 1];
        while (!pointer.compareAndSet(curpointer, curpointer, stamp, stamp)) {
            curpointer = pointer.getReference();
            stamp = pointer.getStamp();
            result = ramainseats[curpointer][departure - 1][arrival - 1];
        }
        return result;
    }

    public boolean hasRemainSeat(int departure, int arrival) {
        return getCurrentSeatsNum(departure, arrival) > 0;
    }

    public synchronized void refreshSeatNum(int departure, int arrival, long oldseat, long newseat, boolean ops) {
        if (ops) {
            int curpointer = 1 - pointer.getReference();
            for (int i = 0; i < stationnum - 1; i++) {
                for (int j = i + 1; j < stationnum; j++) {
                    if (((oldseat & Bitmask[i][j]) == 0) && ((newseat & Bitmask[i][j]) != 0)) {
                        ramainseats[curpointer][i][j] = ramainseats[1 - curpointer][i][j] - 1;
                    } else {
                        ramainseats[curpointer][i][j] = ramainseats[1 - curpointer][i][j];
                    }
                }
            }
            pointer.set(curpointer, pointer.getStamp() + 1);
        } else {
            int curpointer = 1 - pointer.getReference();
            for (int i = 0; i < stationnum - 1; i++) {
                for (int j = i + 1; j < stationnum; j++) {
                    if (((oldseat & Bitmask[i][j]) != 0) && ((newseat & Bitmask[i][j]) == 0)) {
                        ramainseats[curpointer][i][j] = ramainseats[1 - curpointer][i][j] + 1;
                    } else {
                        ramainseats[curpointer][i][j] = ramainseats[1 - curpointer][i][j];
                    }
                }
            }
            pointer.set(curpointer, pointer.getStamp() + 1);
        }
    }


}
