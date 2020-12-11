package ticketingsystem;

import java.util.concurrent.atomic.AtomicLong;

public class Train {
    private int coachnum;
    private int seatnum;
    private int stationnum;
    private int allseatnum;
    private AtomicLong[] seats;
    private SeatOperation seatops;

    public Train(int coachnum, int seatnum, int stationnum) {
        this.coachnum = coachnum;
        this.seatnum = seatnum;
        this.stationnum = stationnum;
        this.allseatnum = coachnum * seatnum;
        seats = new AtomicLong[this.allseatnum];
        for (int i = 0; i < this.allseatnum; i++) {
            seats[i] = new AtomicLong(0);
        }
        this.seatops = new SeatOperation(allseatnum, stationnum);
    }

    public int trainBuyTicket(int begincoach, int beginseat, int departure, int arrival) {
        while (seatops.hasRemainSeat(departure, arrival)) {
            for (int i = 0; i < allseatnum; i++) {
                int expseatnum = (begincoach * seatnum + beginseat + i) % allseatnum;
                long curseat = seats[expseatnum].get();
                while (!isoccupied(curseat, departure, arrival)) {
                    if (seats[expseatnum].compareAndSet(curseat, setoccupied(curseat, departure, arrival))) {
                        seatops.refreshSeatNum(curseat, setoccupied(curseat, departure, arrival), true);
                        return expseatnum;
                    }
                    curseat = seats[expseatnum].get();
                }
            }
        }
        return -1;
    }

    public boolean trainRefundTicket(int coach, int seat, int departure, int arrival) {
        int expseatnum = (coach - 1) * seatnum + seat - 1;
        while(true){
            long curseat = seats[expseatnum].get();
            if(seats[expseatnum].compareAndSet(curseat, resetoccupied(curseat, departure, arrival))){
                seatops.refreshSeatNum(curseat, resetoccupied(curseat, departure, arrival), false);
                return true;
            }
        }
    }

    private boolean isoccupied(long curseat, int departure, int arrival) {
        return (curseat & seatops.Bitmask[departure - 1][arrival - 1]) != 0;
    }

    private long setoccupied(long curseat, int departure, int arrival) {
        return curseat | seatops.Bitmask[departure - 1][arrival - 1];
    }

    private long resetoccupied(long curseat, int departure, int arrival) {
        return curseat & (~seatops.Bitmask[departure - 1][arrival - 1]);
    }

    public int ticketinquiry(int departure, int arrival) {
        return seatops.getCurrentSeatsNum(departure, arrival);
    }
}
