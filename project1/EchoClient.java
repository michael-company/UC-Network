import java.net.*;
import java.io.*;

public class EchoClient {

  public static void main(String[] argv) {
    // Server IP and port.
    String serverIP = "";
    int serverPort = -1;

    // Process command-line arguments.
    for (String arg : argv) {
      String[] splitArg = arg.split("=");
      if (splitArg.length == 2 && splitArg[0].equals("--serverIP")) {
        serverIP = splitArg[1];
      } else if (splitArg.length == 2 && splitArg[0].equals("--serverPort")) {
        serverPort = Integer.parseInt(splitArg[1]);
      } else {
        System.err.println("Usage: java EchoClient --serverIP=<ipaddr> --serverPort=<port>");
        return;
      }
    }

    // Check IP address.
    if (serverIP.isEmpty()) {
      System.err.println("Must specify server IP address with --serverIP");
      return;
    }

    // Check port number.
    if (serverPort == -1) {
      System.err.println("Must specify server port with --serverPort");
      return;
    }
    if (serverPort <= 1024) {
      System.err.println("Avoid potentially reserved port number: " + serverPort + " (should be > 1024)");
      return;
    }

    try {
      Socket socket = new Socket(serverIP, serverPort);
      PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
      BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      BufferedReader stdInReader = new BufferedReader(new InputStreamReader(System.in));
      String input;
      while ((input = stdInReader.readLine()) != null) {
        writer.println(input);
        System.out.println("echo: " + socketReader.readLine());
      }
    } catch (UnknownHostException e) {
      e.printStackTrace();
      System.exit(1);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }
}
