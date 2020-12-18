package ticketingsystem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicStampedReference;

public class SeatOperation {
    private int stationnum;
    private AtomicStampedReference<int[][]> ramainseatsptr;
    public long[][] Bitmask;
    private ConcurrentHashMap<Long,int[][]> LocalRamain;

    public SeatOperation(int allseatnum, int stationnum) {
        this.stationnum = stationnum;
        int[][] initramainseats = new int[stationnum][stationnum];
        this.ramainseatsptr = new AtomicStampedReference<>(initramainseats, 0);
        for (int i = 0; i < stationnum; i++) {
            for (int j = 0; j < i + 1; j++) {
                initramainseats[i][j] = 0;
            }
            for (int j = i + 1; j < stationnum; j++) {
                initramainseats[i][j] = allseatnum;
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
        int[][] curremain;
        int stamp;
        while (true) {
            curremain = ramainseatsptr.getReference();
            stamp = ramainseatsptr.getStamp();
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
        long theardid = Thread.currentThread().getId();
        int[][] localremain = LocalRamain.computeIfAbsent(theardid,k->new int[stationnum][stationnum]);
        int[][] curremain;
        int stamp;
        if (ops) {
            while (true) {
                curremain = ramainseatsptr.getReference();
                stamp = ramainseatsptr.getStamp();
                    if (curremain == localremain) {
                        localremain = new int[stationnum][stationnum];
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
                            LocalRamain.remove(theardid);
                            LocalRamain.put(theardid,localremain);
                            break;
                        }
                    } else {
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
                }
            } else {
                while (true) {
                    curremain = ramainseatsptr.getReference();
                    stamp = ramainseatsptr.getStamp();
                        if (curremain == localremain) {
                            localremain = new int[stationnum][stationnum];
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
                                LocalRamain.remove(theardid);
                                LocalRamain.put(theardid,localremain);
                                break;
                            }
                        } else {
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
        }