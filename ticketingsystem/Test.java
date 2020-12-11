package ticketingsystem;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;
import java.lang.Thread;
import java.util.concurrent.atomic.AtomicInteger;

public class Test {

    final static int threadnum = 64;
    final static int routenum = 20; // route is designed from 1 to 20
    final static int coachnum = 10; // coach is arranged from 1 to 10
    final static int seatnum = 100; // seat is allocated from 1 to 100
    final static int stationnum = 16; // station is designed from 1 to 16

    final static int testnum = 100000;
    final static int retpc = 10; // return ticket operation is 10% percent
    final static int buypc = 40; // buy ticket operation is 30% percent
    final static int inqpc = 100; //inquiry ticket operation is 60% percent

    private static long[] buyTime = new long[threadnum];
    private static long[] refundTime = new long[threadnum];
    private static long[] inquiryTime = new long[threadnum];
    private static int[] buyNum = new int[threadnum];
    private static int[] refundNum = new int[threadnum];
    private static int[] inquiryNum = new int[threadnum];

    static String passengerName() {
        Random rand = new Random();
        long uid = rand.nextInt(testnum);
        return "passenger" + uid;
    }

    public static void main(String[] args) throws InterruptedException{

        Thread[] threads = new Thread[threadnum];

        final TicketingDS tds = new TicketingDS(routenum, coachnum, seatnum, stationnum, threadnum);


        final long startTime = System.nanoTime();
        //long preTime = startTime;

        for (int i = 0; i < threadnum; i++) {
            threads[i] = new Thread(new Runnable() {
                public void run() {
                    int id = ThreadId.get();
                    Random rand = new Random();
                    Ticket ticket = new Ticket();
                    ArrayList<Ticket> soldTicket = new ArrayList<Ticket>();

                    //System.out.println(ThreadId.get());
                    for (int i = 0; i < testnum; i++) {
                        int sel = rand.nextInt(inqpc);
                        if (0 <= sel && sel < retpc && soldTicket.size() > 0) { // return ticket
                            int select = rand.nextInt(soldTicket.size());
                            long preTime = System.nanoTime() - startTime;
                            if ((ticket = soldTicket.remove(select)) != null) {
                                preTime = System.nanoTime() - startTime;
                                tds.refundTicket(ticket);
                                long postTime = System.nanoTime() - startTime;
                                //System.out.println(preTime + " " + postTime + " " + ThreadId.get() + " " + "TicketRefund" + " " + ticket.tid + " " + ticket.passenger + " " + ticket.route + " " + ticket.coach  + " " + ticket.departure + " " + ticket.arrival + " " + ticket.seat);
                                long curRefundTime = postTime - preTime;
                                refundTime[id] += curRefundTime;
                                refundNum[id] += 1;
                            } else {
                                preTime = System.nanoTime() - startTime;
                                long postTime = System.nanoTime() - startTime;
                                //System.out.println(preTime + " " + postTime + " " + ThreadId.get() + " " + "ErrOfRefund");
                                long curRefundTime = postTime - preTime;
                                refundTime[id] += curRefundTime;
                                refundNum[id] += 1;
                            }
                        } else if (retpc <= sel && sel < buypc) { // buy ticket
                            String passenger = passengerName();
                            int route = rand.nextInt(routenum) + 1;
                            int departure = rand.nextInt(stationnum - 1) + 1;
                            int arrival = departure + rand.nextInt(stationnum - departure) + 1; // arrival is always greater than departure
                            long preTime = System.nanoTime() - startTime;
                            if ((ticket = tds.buyTicket(passenger, route, departure, arrival)) != null) {
                                long postTime = System.nanoTime() - startTime;
                                //System.out.println(preTime + " " + postTime + " " + ThreadId.get() + " " + "TicketBought" + " " + ticket.tid + " " + ticket.passenger + " " + ticket.route + " " + ticket.coach + " " + ticket.departure + " " + ticket.arrival + " " + ticket.seat);
                                long curBuyTime = postTime - preTime;
                                buyTime[id] += curBuyTime;
                                buyNum[id] += 1;
                                soldTicket.add(ticket);
                            } else {
                                long postTime = System.nanoTime() - startTime;
                                //System.out.println(preTime + " " + postTime + " " + ThreadId.get() + " " + "TicketSoldOut" + " " + route + " " + departure+ " " + arrival);
                                long curBuyTime = postTime - preTime;
                                buyTime[id] += curBuyTime;
                                buyNum[id] += 1;
                            }
                        } else if (buypc <= sel && sel < inqpc) { // inquiry ticket

                            int route = rand.nextInt(routenum) + 1;
                            int departure = rand.nextInt(stationnum - 1) + 1;
                            int arrival = departure + rand.nextInt(stationnum - departure) + 1; // arrival is always greater than departure
                            long preTime = System.nanoTime() - startTime;
                            int leftTicket = tds.inquiry(route, departure, arrival);
                            long postTime = System.nanoTime() - startTime;
                            //System.out.println(preTime + " " + postTime + " " + ThreadId.get() + " " + "RemainTicket" + " " + leftTicket + " " + route+ " " + departure+ " " + arrival);
                            long curInquiryTime = postTime - preTime;
                            inquiryTime[id] += curInquiryTime;
                            inquiryNum[id] += 1;
                        }
                    }

                }
            });
            threads[i].start();
        }

        for (int i = 0; i < threadnum; i++) {
            threads[i].join();
        }

        final long endTime = System.nanoTime();

        long allTime = endTime - startTime;

        int allNum = 0;

        long totalRefundTime = 0;
        long totalBuyTime = 0;
        long totalInquiryTime = 0;

        for (int i = 0; i < threadnum; i++) {
            totalRefundTime += refundTime[i] / refundNum[i];
            totalBuyTime += buyTime[i] / buyNum[i];
            totalInquiryTime += inquiryTime[i] / inquiryNum[i];
            allNum += refundNum[i];
            allNum += buyNum[i];
            allNum += inquiryNum[i];
        }

        long aveRefundTime = totalRefundTime / threadnum;
        long aveBuyTime = totalBuyTime / threadnum;
        long aveInquiryTime = totalInquiryTime / threadnum;
        long throughput = (long) ((double) allNum / ((double) allTime / 1000000000));

        System.out.println("平均查询用时" + aveInquiryTime + "ns");
        System.out.println("平均买票用时" + aveBuyTime + "ns");
        System.out.println("平均退票用时" + aveRefundTime + "ns");
        System.out.println("系统总吞吐量为" + throughput + "次/秒");

    }

}
