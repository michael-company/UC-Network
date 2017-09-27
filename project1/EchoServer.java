import java.net.*;
import java.io.*;

public class EchoServer {

  public static void main(String[] argv) {
    // Server port.
    int port = -1;

    // Process command-line arguments.
    for (String arg : argv) {
      String[] splitArg = arg.split("=");
      if (splitArg.length == 2 && splitArg[0].equals("--port")) {
        port = Integer.parseInt(splitArg[1]);
      } else {
        System.err.println("Usage: java EchoServer --port=<port>");
        return;
      }
    }

    // Check port number.
    if (port == -1) {
      System.err.println("Must specify port number with --port");
      return;
    }
    if (port <= 1024) {
      System.err.println("Avoid potentially reserved port number: " + port + " (should be > 1024)");
      return;
    }

    try {
      // Bind to new server socket to server port.
      ServerSocket serverSocket = new ServerSocket(port);

      // Accept requests indefinitely.
      while (true) {
        System.out.println("Echo server listening on port " + port + " ...");
        Socket socket = serverSocket.accept();
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        String input;
        while ((input = reader.readLine()) != null) {
          writer.println(input);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
