package ticketingsystem;

import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.atomic.AtomicStampedReference;

public class SeatOperation {
    private int stationnum;
    private AtomicReferenceArray<AtomicReferenceArray<AtomicStampedReference<String>>> ramainseats;
    public long[][] Bitmask;

    public SeatOperation(int allseatnum, int stationnum) {
        this.stationnum = stationnum;
        this.ramainseats = new AtomicReferenceArray<>(stationnum);
        for (int i = 0; i < stationnum; i++) {
            this.ramainseats.set(i, new AtomicReferenceArray<>(stationnum));
            for (int j = 0; j < i + 1; j++) {
                this.ramainseats.get(i).set(j, new AtomicStampedReference<>("0", 0));
            }
            for (int j = i + 1; j < stationnum; j++) {
                this.ramainseats.get(i).set(j, new AtomicStampedReference<>(String.valueOf(allseatnum), 0));
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
        if (departure >= arrival)
            return 0;
        AtomicStampedReference<String> temp = ramainseats.get(departure - 1).get(arrival - 1);
        int stamp = temp.getStamp();
        String result = temp.getReference();
        while (!temp.compareAndSet(result, result, stamp, stamp)) {
            temp = ramainseats.get(departure - 1).get(arrival - 1);
            stamp = temp.getStamp();
            result = temp.getReference();
        }
        return Integer.parseInt(result);
    }

    public boolean hasRemainSeat(int departure, int arrival) {
        return getCurrentSeatsNum(departure, arrival) > 0;
    }

    public void refreshSeatNum(int departure, int arrival, long oldseat, long newseat, boolean ops) {
        if (ops) {
            for (int i = 0; i < arrival - 1; i++) {
                for (int j = Math.max(i + 1, departure); j < stationnum; j++) {
                    if (((oldseat & Bitmask[i][j]) == 0) && ((newseat & Bitmask[i][j]) != 0)) {
                        AtomicStampedReference<String> temp = ramainseats.get(i).get(j);
                        int stamp = temp.getStamp();
                        String result = temp.getReference();
                        while (!temp.compareAndSet(result, String.valueOf(Integer.parseInt(result) - 1), stamp, stamp)) {
                            temp = ramainseats.get(i).get(j);
                            stamp = temp.getStamp();
                            result = temp.getReference();
                        }
                    }
                }
            }
        } else {
            for (int i = 0; i < arrival - 1; i++) {
                for (int j = Math.max(i + 1, departure); j < stationnum; j++) {
                    if (((oldseat & Bitmask[i][j]) != 0) && ((newseat & Bitmask[i][j]) == 0)) {
                        AtomicStampedReference<String> temp = ramainseats.get(i).get(j);
                        int stamp = temp.getStamp();
                        String result = temp.getReference();
                        while (!temp.compareAndSet(result, String.valueOf(Integer.parseInt(result) + 1), stamp, stamp)) {
                            temp = ramainseats.get(i).get(j);
                            stamp = temp.getStamp();
                            result = temp.getReference();
                        }
                    }
                }
            }
        }
    }


}
