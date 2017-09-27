import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * The type Ping client.
 *
 * Just run the given commands such as
 * java PingClient --server_ip=127.0.0.1 --server_port=56789 --count=10 --period=1000 --timeout=2000
 *
 * It will then successfully print both the indidivual and aggregate statistics
 *
 */


// multithreaded PingClient
public class PingClient implements Runnable {

    // declare variables
    private DatagramSocket socket;
    private String hostname;
    private InetAddress host;
    private int port;
    private int packets;
    private int delays;
    private int timeouts;
    private int transmitted;
    private int received;

    /**
     * Instantiates a new Ping client.
     *
     * @param hostname the hostname
     * @param port     the port
     * @param PACKETS  the packets
     * @param DELAY    the delay
     * @param TIMEOUT  the timeout
     */
// constructor
    public PingClient(String hostname, int port, int PACKETS, int DELAY, int TIMEOUT) {
        this.hostname = hostname;
        this.port = port;
        this.packets = PACKETS;
        this.delays = DELAY;
        this.timeouts = TIMEOUT;
        this.transmitted = 0;
        this.received = 0;
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     */
// main
    public static void main(String[] args) {

        // Default values to variables
        String host = "localhost";
        int port = 56789;
        int PACKETS = 10;
        int DELAY = 1000;
        int TIMEOUT = 1000;

        // Process command-line arguments
        for (String arg : args) {
            String[] splitArg = arg.split("=");
            if (splitArg.length == 2 && splitArg[0].equals("--server_ip")) {
                host = String.valueOf(splitArg[1]);
            } else if (splitArg.length == 2 && splitArg[0].equals("--server_port")) {
                port = Integer.parseInt(splitArg[1]);
            } else if (splitArg.length == 2 && splitArg[0].equals("--count")) {
                PACKETS = Integer.parseInt(splitArg[1]);
            } else if (splitArg.length == 2 && splitArg[0].equals("--period")) {
                DELAY = Integer.parseInt(splitArg[1]); // milliseconds
            } else if (splitArg.length == 2 && splitArg[0].equals("--timeout")) {
                TIMEOUT = Integer.parseInt(splitArg[1]); // milliseconds
            } else {
                System.err.println("Eg: java PingClient --server_ip=127.0.0.1 --server_port=56789 --count=10 --period=1000 --timeout=10000");
                return;
            }
        }
        Runnable c = new PingClient(host, port, PACKETS, DELAY, TIMEOUT);
        c.run();
    }

    //generate string ie. Each ping message contains a payload of data that includes the keyword PING, a sequence number, and a timestamp
    private static String generateSendString(int seq) {
        StringBuffer buf = new StringBuffer("PING ");
        buf.append(seq);
        buf.append(' ');
        buf.append(System.currentTimeMillis());
        buf.append('\n');
        return buf.toString();
    }

    // read the received data
    private static byte[] readReceivedDataBytes(DatagramPacket request) {
        byte[] buf = request.getData();
        return buf;
    }

    // read and print the received data
    private static void printData(DatagramPacket request, long delayTime) throws Exception {
        // get references
        byte[] buf = request.getData();

        // get the data as a stream of bytes.
        ByteArrayInputStream bais = new ByteArrayInputStream(buf);

        // get a stream of characters.
        InputStreamReader isr = new InputStreamReader(bais);

        // get the character data a line at a time.
        BufferedReader br = new BufferedReader(isr);

        // get message data is contained in a single line
        String line = br.readLine();
        String cont = new String(line.substring(line.indexOf(" ") + 1, line.indexOf("14")));

        // Print for testing
        if (!cont.equals("")) {
            System.out.println(
                    "PONG " +
                            request.getAddress().getHostAddress() +
                            ": " + "seq=" + cont
                            + "time=" + delayTime + " ms");
        } else {
            System.out.println(
                    "PONG " +
                            request.getAddress().getHostAddress() +
                            ": " + "seq=" + "1 "
                            + "time=" + delayTime + " ms");
        }
    }

    public void run() {

        //set and obtain the information from socket and host
        if (host == null) {
            try {
                host = InetAddress.getByName(hostname);
            } catch (UnknownHostException e) {
                System.err.println(hostname + ": Host name lookup failure");
                return;
            }
        }

        try {
            socket = new DatagramSocket();
            //For any given request, the client should wait up to timeout milliseconds to receive a reply.
            //Set up the timeout accordingly to input
            socket.setSoTimeout(timeouts);
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

        //initialize storage variable before sending and receving perspective packet
        System.out.println("PING " + hostname);
        int sequenceNumber = 1;
        long totalPingTime = 0;
        long min = 0;
        long max = 0;
        long originalPackets = packets;
        long elapsedTimeA = 0;

        ArrayList<Long> roundTripTimes = new ArrayList<Long>();

        // send and receive whenever packet goes out and goes in
        while (true) {
            String sendString = generateSendString(sequenceNumber);
            byte[] sendData = sendString.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, host, port);

            long sendTime = System.currentTimeMillis();
            long pingTime = 0;
            long cElapsed = 0;

            //cumulative elapsed time is defined as the interval between sending of the first request and reception/time-out of the last response
            if (originalPackets - 1 == packets) {
                elapsedTimeA = sendTime;
            }

            //sending a packet
            try {
                socket.send(sendPacket);
                transmitted++;
            } catch (IOException e) {
                System.err.println("Sending ping request to " + hostname + ": " + e);
            }

            //receiving a packet
            byte[] receiveData = new byte[1024];
            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
            try {
                socket.receive(receivePacket);

                pingTime = System.currentTimeMillis() - sendTime;

                roundTripTimes.add(pingTime);

                received++;

                //no more packet
                if (received == originalPackets || packets == 1) {
                    cElapsed = System.currentTimeMillis() - elapsedTimeA;
                }

                //check for rtt stat update
                if (roundTripTimes.size() != 0) {
                    min = Collections.min(roundTripTimes);
                    max = Collections.max(roundTripTimes);
                }

                //going deeply into the unique packet on their unique streams received back from server for replied info etc
                printData(receivePacket, pingTime);

                totalPingTime += pingTime;

                //straight forward way produce corresponding result, just decomment it if you like
                //System.out.println("PONG " + hostname + ": seq=" + sequenceNumber + " time=" + pingTime + " ms");

            } catch (SocketTimeoutException e) {
                //obtain the elapsed time after knowing server response, not sent, and move on
                cElapsed = System.currentTimeMillis() - elapsedTimeA;
            } catch (IOException e) {
                System.err.println("Receiving ping response from " + hostname + ": " + e);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // next packet sequence number
            ++sequenceNumber;

            // just decrement remaining packet amount after finish processing one packet
            --packets;

            // print stat after finnished processing all packets
            if (packets == 0) {

                //packet loss rounded to the nearest percentage point
                double decDiff = ((transmitted - received) * 1.0) / transmitted;

                int loss = (int) Math.round(decDiff * 100);

                System.out.println("\n--- " + hostname + " ping statistics ---");
                System.out.println(transmitted + " transmitted, " + received + " received, " + loss + "% loss, time " + cElapsed + " ms");
                if (received == 0) {
                    System.out.println("rtt min/avg/max = " + 0 + "/" + 0 + "/" + 0);
                } else {
                    System.out.println("rtt min/avg/max = " + min + "/" + totalPingTime / received + "/" + max);
                }

                break;
            }
            try {
                //delay accordingly
                Thread.sleep(delays);
            } catch (InterruptedException e) {
                System.err.println("InterruptedException "+e);
                System.out.println("PONG " + hostname + ": seq=" + sequenceNumber + " time=" + pingTime + " ms");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
