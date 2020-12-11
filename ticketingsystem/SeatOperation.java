package ticketingsystem;

import java.util.concurrent.atomic.AtomicStampedReference;

public class SeatOperation {
    private int stationnum;
    private AtomicStampedReference<int[][]> ramainseatsptr;
    public long[][] Bitmask;

    public SeatOperation(int allseatnum, int stationnum) {
        this.stationnum = stationnum;
        int[][] ramainseats = new int[stationnum][stationnum];
        this.ramainseatsptr = new AtomicStampedReference<>(ramainseats, 0);
        for (int i = 0; i < stationnum; i++) {
            for (int j = 0; j < i + 1; j++) {
                ramainseats[i][j] = 0;
                ramainseats[i][j] = 0;
            }
            for (int j = i + 1; j < stationnum; j++) {
                ramainseats[i][j] = allseatnum;
                ramainseats[i][j] = allseatnum;
            }
        }
        this.Bitmask = new long[stationnum][stationnum];
        for (int i = 0; i < stationnum; i++) {
            for (int j = i + 1; j < stationnum; j++) {
                this.Bitmask[i][j] = ((0x1 << (j - i)) - 1) << (stationnum - j - 1);
            }
        }
    }

    public int getCurrentSeatsNum(int departure, int arrival) {
        while (true) {
            int[][] curremain = ramainseatsptr.getReference();
            int stamp = ramainseatsptr.getStamp();
            int result = curremain[departure - 1][arrival - 1];
            if (ramainseatsptr.compareAndSet(curremain, curremain, stamp, stamp)) {
                return result;
            }
        }
    }

    public boolean hasRemainSeat(int departure, int arrival) {
        return getCurrentSeatsNum(departure, arrival) > 0;
    }

    public void refreshSeatNum(long oldseat, long newseat, boolean ops) {
        if (ops) {
            while (true) {
                int[][] curremain = ramainseatsptr.getReference();
                int stamp = ramainseatsptr.getStamp();
                int[][] localremain = new int[stationnum][stationnum];
                for (int i = 0; i < stationnum - 1; i++) {
                    for (int j = i + 1; j < stationnum; j++) {
                        if (((oldseat & Bitmask[i][j]) == 0) && ((newseat & Bitmask[i][j]) != 0)) {
                            localremain[i][j] = curremain[i][j] - 1;
                        } else {
                            localremain[i][j] = curremain[i][j];
                        }
                    }
                }
                if (ramainseatsptr.compareAndSet(curremain, localremain, stamp, stamp + 1)) {
                    break;
                }
            }
        } else {
            while (true) {
                int[][] curremain = ramainseatsptr.getReference();
                int stamp = ramainseatsptr.getStamp();
                int[][] localremain = new int[stationnum][stationnum];
                for (int i = 0; i < stationnum - 1; i++) {
                    for (int j = i + 1; j < stationnum; j++) {
                        if (((oldseat & Bitmask[i][j]) != 0) && ((newseat & Bitmask[i][j]) == 0)) {
                            localremain[i][j] = curremain[i][j] + 1;
                        } else {
                            localremain[i][j] = curremain[i][j];
                        }
                    }
                }
                if (ramainseatsptr.compareAndSet(curremain, localremain, stamp, stamp + 1)) {
                    break;
                }
            }
        }
    }


}