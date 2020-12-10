package ticketingsystem;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class TicketingDS implements TicketingSystem {
    private int routenum;
    private int coachnum;
    private int seatnum;
    private int stationnum;
    private int threadnum;
    private Train[] trains;
    private int coachhashmark;
    private int seathashmark;
    private AtomicLong hashcount;
    private AtomicLong tidcount;
    private ConcurrentHashMap<Long, Ticket> soldtickets;

    public TicketingDS() {
        this.routenum = 5;
        this.coachnum = 8;
        this.seatnum = 100;
        this.stationnum = 10;
        this.threadnum = 16;
        trains = new Train[this.routenum];
        for (int i = 0; i < this.routenum; i++) {
            trains[i] = new Train(this.coachnum, this.seatnum, this.stationnum);
        }
        this.coachhashmark = (0x1 << (32 - Integer.numberOfLeadingZeros(this.coachnum - 1))) - 1;
        this.seathashmark = (0x1 << (32 - Integer.numberOfLeadingZeros(this.seatnum - 1))) - 1;
        this.hashcount = new AtomicLong(1);
        this.tidcount = new AtomicLong(1);
        this.soldtickets = new ConcurrentHashMap<>();
    }

    public TicketingDS(int routenum, int coachnum, int seatnum, int stationnum, int threadnum) {
        this.routenum = routenum;
        this.coachnum = coachnum;
        this.seatnum = seatnum;
        this.stationnum = stationnum;
        this.threadnum = threadnum;
        trains = new Train[routenum];
        for (int i = 0; i < routenum; i++) {
            trains[i] = new Train(coachnum, seatnum, stationnum);
        }
        this.coachhashmark = (0x1 << (32 - Integer.numberOfLeadingZeros(this.coachnum - 1))) - 1;
        this.seathashmark = (0x1 << (32 - Integer.numberOfLeadingZeros(this.seatnum - 1))) - 1;
        this.hashcount = new AtomicLong(1);
        this.tidcount = new AtomicLong(1);
        this.soldtickets = new ConcurrentHashMap<>();
    }

    @Override
    public Ticket buyTicket(String passenger, int route, int departure, int arrival) {
        Train curtrain = trains[route - 1];
        long hashtid = this.hashcount.getAndIncrement();
        long threadid = Thread.currentThread().getId();
        int begincoach = (int) ((hashtid ^ threadid) & this.coachhashmark);
        int beginseat = (int) ((hashtid ^ threadid) & this.seathashmark);
        int seat = curtrain.trainBuyTicket(begincoach, beginseat, departure, arrival);
        if (seat == -1)
            return null;
        Ticket ticket = new Ticket();
        ticket.tid = tidcount.getAndIncrement();
        ticket.passenger = passenger;
        ticket.route = route;
        ticket.coach = seat / seatnum + 1;
        ticket.seat = seat % seatnum + 1;
        ticket.departure = departure;
        ticket.arrival = arrival;
        soldtickets.put(ticket.tid, ticket);
        return ticket;
    }

    @Override
    public int inquiry(int route, int departure, int arrival) {
        return trains[route - 1].ticketinquiry(departure, arrival);
    }

    @Override
    public boolean refundTicket(Ticket ticket) {
        if (soldtickets.containsKey(ticket.tid)) {
            Ticket exsitticket = soldtickets.get(ticket.tid);
            if ((exsitticket.passenger.equals(ticket.passenger)) &&
                    (exsitticket.route == ticket.route) &&
                    (exsitticket.coach == ticket.coach) &&
                    (exsitticket.seat == ticket.seat) &&
                    (exsitticket.departure == ticket.departure) &&
                    (exsitticket.arrival == ticket.arrival)
            ) {
                soldtickets.remove(ticket.tid,ticket);
                return trains[ticket.route - 1].trainRefundTicket(ticket.coach, ticket.seat, ticket.departure, ticket.arrival);

            }
            return false;
        }
        return false;
    }
}
