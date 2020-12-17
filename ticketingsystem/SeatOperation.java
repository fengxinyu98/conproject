package ticketingsystem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

public class SeatOperation {
    private int stationnum;
    private AtomicStampedReference<RemainArray> ramainseatsptr;
    public long[][] Bitmask;
    private ConcurrentHashMap<Long, RemainArray> LocalRamain;

    public SeatOperation(int allseatnum, int stationnum) {
        this.stationnum = stationnum;
        int[][][] initramainseats = new int[2][stationnum][stationnum];
        this.ramainseatsptr = new AtomicStampedReference<>(new RemainArray(0, initramainseats),0);
        for (int i = 0; i < stationnum; i++) {
            for (int j = 0; j < i + 1; j++) {
                initramainseats[0][i][j] = 0;
                initramainseats[1][i][j] = 0;
            }
            for (int j = i + 1; j < stationnum; j++) {
                initramainseats[0][i][j] = allseatnum;
                initramainseats[1][i][j] = allseatnum;
            }
        }
        this.Bitmask = new long[stationnum][stationnum];
        for (int i = 0; i < stationnum; i++) {
            for (int j = i + 1; j < stationnum; j++) {
                this.Bitmask[i][j] = ((0x1 << (j - i)) - 1) << (stationnum - j - 1);
            }
        }
        this.LocalRamain = new ConcurrentHashMap<>();
    }

    public int getCurrentSeatsNum(int departure, int arrival) {
        while (true) {
            RemainArray curRemainArray = ramainseatsptr.getReference();
            int stamp = ramainseatsptr.getStamp();
            int[][] curremain = curRemainArray.getRemainseats()[stamp];
            int result = curremain[departure - 1][arrival - 1];
            if (ramainseatsptr.compareAndSet(curRemainArray, curRemainArray,stamp,stamp)) {
                return result;
            }
        }
    }

    public boolean hasRemainSeat(int departure, int arrival) {
        return getCurrentSeatsNum(departure, arrival) > 0;
    }

    public void refreshSeatNum(long oldseat, long newseat, boolean ops) {
        long threadid = Thread.currentThread().getId();
        RemainArray localRemainArray = LocalRamain.computeIfAbsent(threadid, k -> new RemainArray(0, new int[2][stationnum][stationnum]));
        int[][] localremain;
        int[][] curremain;
        int stamp;
        if (ops) {
            while (true) {
                RemainArray curRemainArray = ramainseatsptr.getReference();
                stamp = ramainseatsptr.getStamp();
                curremain = curRemainArray.getRemainseats()[stamp];
                localremain = localRemainArray.getRemainseats()[stamp];
                for (int i = 0; i < stationnum - 1; i++) {
                    for (int j = i + 1; j < stationnum; j++) {
                        if (((oldseat & Bitmask[i][j]) == 0) && ((newseat & Bitmask[i][j]) != 0)) {
                            localremain[i][j] = curremain[i][j] - 1;
                        } else {
                            localremain[i][j] = curremain[i][j];
                        }
                    }
                }
                if (ramainseatsptr.compareAndSet(curRemainArray, localRemainArray,stamp,1-stamp)) {
                    break;
                }

            }
        } else {
            while (true) {
                RemainArray curRemainArray = ramainseatsptr.getReference();
                stamp = ramainseatsptr.getStamp();
                curremain = curRemainArray.getRemainseats()[stamp];
                localremain = localRemainArray.getRemainseats()[stamp];
                for (int i = 0; i < stationnum - 1; i++) {
                    for (int j = i + 1; j < stationnum; j++) {
                        if (((oldseat & Bitmask[i][j]) != 0) && ((newseat & Bitmask[i][j]) == 0)) {
                            localremain[i][j] = curremain[i][j] + 1;
                        } else {
                            localremain[i][j] = curremain[i][j];
                        }
                    }
                }
                if (ramainseatsptr.compareAndSet(curRemainArray, localRemainArray,stamp,1-stamp)) {
                    break;
                }

            }
        }
    }
}

class RemainArray {
    private int tag;
    private int[][][] remainseats;

    public RemainArray(int tag, int[][][] remainseats) {
        this.tag = tag;
        this.remainseats = remainseats;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public int[][][] getRemainseats() {
        return remainseats;
    }

    public void setRemainseats(int[][][] remainseats) {
        this.remainseats = remainseats;
    }
}