import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * The type Web server.
 */
public class WebServer extends Thread {

    // thread variables
    private BufferedReader clientInput;
    private DataOutputStream serverOutput;
    private Socket aClient;

    /**
     * Instantiates a new Web server.
     *
     * @param clientSocket the client socket
     */
    public WebServer(Socket clientSocket) {
        setaClient(clientSocket);
    }

    /**
     * The entry point of application.
     *
     * @param args the input arguments
     * @throws Exception the exception
     */
    public static void main(String[] args) throws Exception {

        int portNumber = 0;

        // check input/arguments correctness
        if (args.length != 1) {
            System.err.println("Invalid arguments. Eg: java WebServer --serverPort=1234");
            System.exit(1);
        }
        //check for port
        else if (args[0].substring(0, 13).equals("--serverPort="))
            portNumber = Integer.parseInt(args[0].substring(13));
        else if (args[0].substring(0, 7).equals("--port="))
            // also to handle the flag `--port=[PORT_NUMBER]`, just decomment ow
            portNumber = Integer.parseInt(args[0].substring(7));
        else {
            System.exit(1);
        }

        try {
            //create server socket
            ServerSocket serverSocket = new ServerSocket(portNumber);
            //multiple client
            while (true) {
                //establish connection
                Socket socketClient = serverSocket.accept();
                try {
                    //start a thread when there is a new socketClient
                    (new WebServer(socketClient)).start();
                    System.out.println("Accepted a client connection.");
                } catch (Exception e) {
                    System.err.println("Exception: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("IO Exception: " + e.getMessage());
            System.exit(1);
        }
        System.out.println("Disconnected a client connection");
    }

    public void run() {
        try {
            //get input as in project 0
            setClientInput(new BufferedReader(new InputStreamReader(getAClient().getInputStream())));
            setServerOutput(new DataOutputStream(getAClient().getOutputStream()));
            String aRequestString = getClientInput().readLine();
            String headerRequest = aRequestString;
            StringTokenizer tokenizer = new StringTokenizer(headerRequest);
            String requestType = tokenizer.nextToken();
            String resourcePath = tokenizer.nextToken();

            ArrayList<ArrayList<String>> redirects = new ArrayList<>();

            if (new File("www/redirect.defs").isFile()) {
                BufferedReader aBufferReader = new BufferedReader(new FileReader("www/redirect.defs"));
                try {
                    String fileString = aBufferReader.readLine();
                    while (fileString != null) {
                        StringTokenizer aLine = new StringTokenizer(fileString);
                        ArrayList<String> anArrayList = new ArrayList<>();
                        ///cats
                        ///http://en.wikipedia.org/wiki/Cat
                        anArrayList.add(aLine.nextToken());
                        ///uchicago/cs
                        ///http://www.cs.uchicago.edu/
                        anArrayList.add(aLine.nextToken());
                        redirects.add(anArrayList);
                        fileString = aBufferReader.readLine();
                    }
                } finally {
                    //always close buffer after finnish adding
                    aBufferReader.close();
                }
            }

            //GET request
            if (requestType.equals("GET")) {
                URI uri = new URI(null, null, resourcePath, null);
                String fileName;
                boolean redirect = false;
                for (int i = 0; i < redirects.size(); ++i) {
                    if ((String.format(uri.toASCIIString())).equals(redirects.get(i).get(0))) {
                        fileName = redirects.get(i).get(1);
                        redirect = true;
                        processRedirect(fileName);
                    }
                }

                //always redirect if needed
                if (!redirect) {
                    if (String.format(uri.toASCIIString()).contains("redirect.defs")) {
                        processHead(404, "File Not Found");
                    } else {
                        //relative path
                        fileName = "./www/" + String.format(uri.toASCIIString());

                        if (new File(fileName).isFile()) {
                            respond_get(200, fileName);
                        } else {
                            respond_get(404, "File Not Found");
                        }
                    }
                }

            }
            //HEAD request
            else if (requestType.equals("HEAD")) {
                URI uri = new URI(null, null, resourcePath, null);
                String fileName;
                boolean redirect = false;
                for (int i = 0; i < redirects.size(); ++i) {
                    if ((String.format(uri.toASCIIString())).equals(redirects.get(i).get(0))) {
                        fileName = redirects.get(i).get(1);
                        redirect = true;
                        processRedirect(fileName);
                    }
                }

                //always redirect if needed
                if (!redirect) {
                    if (String.format(uri.toASCIIString()).contains("redirect.defs")) {
                        processHead(404, "File Not Found");
                    } else {
                        fileName = "./www/" + String.format(uri.toASCIIString());

                        if (new File(fileName).isFile()) {
                            processHead(200, fileName);
                        } else {
                            processHead(404, "File Not Found");
                        }
                    }
                }
            }

            //Any POST request or other unknown op code returns a 403
            //Any invalid request also returns a 403
            else {
                respond_get(403, "Forbidden");
            }
            //after processing the request, close connection
            getAClient().close();
        } catch (IOException e) {
            System.err.println("IO Exception: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        }
    }

    /**
     * Respond get.
     *
     * @param status   the status
     * @param response the response
     */
    public void respond_get(int status, String response) {

        String statusEntry;
        String connectionType = "Connection: close \r\n";
        String serverDetails = "Linux Ubuntu 14.04LTS \r\n";
        String fileName;
        String contentType;
        String contentLength = null;
        boolean foundStatus = false;

        if (status == 200) {
            statusEntry = "HTTP/1.1 200 OK\r\n";
            foundStatus = true;
        } else if (status == 403)
            statusEntry = "HTTP/1.1 403 Forbidden\r\n";
        else
            statusEntry = "HTTP/1.1 404 Not Found\r\n";

        //file found
        if (foundStatus) {
            fileName = response;
            try {
                FileInputStream fileIn = new FileInputStream(fileName);
                contentLength = "Content-Length: " + Integer.toString(fileIn.available()) + "\r\n";
                fileIn.close();
            } catch (FileNotFoundException e) {
                System.err.println("File not found: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }

            //obtain the classified type
            TypeHandler typeHandler = new TypeHandler(fileName).invoke();
            String currentTimeString = typeHandler.getCurrentTimeString() + "\r\n";
            contentType = typeHandler.getContentType();

            try {
                outputString(statusEntry, connectionType, serverDetails, contentType, contentLength, currentTimeString);

                byte[] dataBuffer = new byte[10000];
                int bytesRead;

                FileInputStream fileIn = new FileInputStream(fileName);

                while ((bytesRead = fileIn.read(dataBuffer)) != -1) {
                    getServerOutput().write(dataBuffer, 0, bytesRead);
                }
                fileIn.close();
            } catch (IOException e) {
                System.err.println("Exception: " + e.getMessage());
            }
        }

        //file not found
        else {
            // current time
            SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String simpleDateFormatString = dateFormater.format(new Date()) + "\r\n";
            contentLength = "Content-Length: \r\n";
            contentType = "Content-Type: \r\n";
            try {
                outputString(statusEntry, connectionType, serverDetails, contentType, contentLength, simpleDateFormatString);
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }
        }
    }

    //write all required content
    private void outputString(String statusString, String connectionType, String serverDetails, String contentType, String contentLength, String currentTimeString) throws IOException {
        getServerOutput().writeBytes(statusString);
        getServerOutput().writeBytes(connectionType);
        getServerOutput().writeBytes(currentTimeString);
        getServerOutput().writeBytes(serverDetails);
        getServerOutput().writeBytes(contentLength);
        getServerOutput().writeBytes(contentType);
        getServerOutput().writeBytes("\r\n");
    }

    /**
     * Process head.
     *
     * @param status   the status
     * @param response the response
     */
    public void processHead(int status, String response) {

        String returnNum;
        String connectionType = "Connection: close\r\n";
        String serverSys = "Linux Ubuntu 14.04LTS\r\n";
        String fileName;
        String contentType;
        String contentLength = null;

        boolean found = false;

        if (status == 200) {
            returnNum = "HTTP/1.1 200 OK" + "\r\n";
            found = true;
        } else {
            returnNum = "HTTP/1.1 404 Not Found" + "\r\n";
        }
        //file found
        if (found) {
            fileName = response;

            try {
                FileInputStream aFileStream = new FileInputStream(fileName);
                contentLength = "Content-Length: " + Integer.toString(aFileStream.available()) + "\r\n";
                aFileStream.close();
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }

            //obtain the classified type
            TypeHandler typeHandler = new TypeHandler(fileName).invoke();
            String currentTime = typeHandler.getCurrentTimeString() + "\r\n";

            contentType = typeHandler.getContentType();

            try {
                outputString(returnNum, connectionType, serverSys, contentType, contentLength, currentTime);
            } catch (IOException e) {
                System.err.println("Exception: " + e.getMessage());
            }
        }

        //file not found
        else {
            SimpleDateFormat simpleDateFormatString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            String currentTime = simpleDateFormatString.format(new Date()) + "\r\n";
            contentLength = "Content-Length: \r\n";
            contentType = "Content-Type: \r\n";

            try {
                outputString(returnNum, connectionType, serverSys, contentType, contentLength, currentTime);
            } catch (IOException e) {
                System.err.println("IOException: " + e.getMessage());
            }
        }
    }

    /**
     * Process redirect.
     *
     * @param fileName the file name
     */
    public void processRedirect(String fileName) {
        String statusString = "HTTP/1.1 301 Moved Permanently\r\n";
        String locationString = "Location: " + fileName + "\r\n";
        try {
            getServerOutput().writeBytes(statusString);
            getServerOutput().writeBytes(locationString);
            getServerOutput().writeBytes("\r\n");

        } catch (IOException e) {
            System.err.println("Exception: " + e.getMessage());
        }
    }

    /**
     * Gets client input.
     *
     * @return the client input
     */
    public BufferedReader getClientInput() {
        return clientInput;
    }

    /**
     * Sets client input.
     *
     * @param clientInput the client input
     */
    public void setClientInput(BufferedReader clientInput) {
        this.clientInput = clientInput;
    }

    /**
     * Gets server output.
     *
     * @return the server output
     */
    public DataOutputStream getServerOutput() {
        return serverOutput;
    }

    /**
     * Sets server output.
     *
     * @param serverOutput the server output
     */
    public void setServerOutput(DataOutputStream serverOutput) {
        this.serverOutput = serverOutput;
    }

    /**
     * Gets a client.
     *
     * @return the a client
     */
    public Socket getAClient() {
        return aClient;
    }

    /**
     * Sets client.
     *
     * @param aClient the a client
     */
    public void setaClient(Socket aClient) {
        this.aClient = aClient;
    }

    private class TypeHandler {
        private String fileName;
        private String contentType;
        private String currentTimeString;

        /**
         * Instantiates a new Type handler.
         *
         * @param fileName the file name
         */
        public TypeHandler(String fileName) {
            this.fileName = fileName;
        }

        /**
         * Gets content type.
         *
         * @return the content type
         */
        public String getContentType() {
            return contentType;
        }

        /**
         * Gets current time string.
         *
         * @return the current time string
         */
        public String getCurrentTimeString() {
            return currentTimeString;
        }

        /**
         * Invoke type handler.
         *
         * @return the type handler
         */
        public TypeHandler invoke() {
            // MIME types handlers
            if (fileName.endsWith(".html"))
                contentType = "Content-Type: text/html\r\n";
            else if (fileName.endsWith(".htm"))
                contentType = "Content-Type: text/htm\r\n";
            else if (fileName.endsWith(".css"))
                contentType = "Content-Type: text/css\r\n";
            else if (fileName.endsWith(".txt"))
                contentType = "Content-Type: text/plain\r\n";
            else if (fileName.endsWith(".pdf"))
                contentType = "Content-Type: application/pdf\r\n";
            else if (fileName.endsWith(".doc"))
                contentType = "Content-Type: application/doc\r\n";
            else if (fileName.endsWith(".docx"))
                contentType = "Content-Type: application/docx\r\n";
            else if (fileName.endsWith(".png"))
                contentType = "Content-Type: image/png\r\n";
            else if (fileName.endsWith(".jpg"))
                contentType = "Content-Type: image/jpg\r\n";
            else if (fileName.endsWith(".jpeg"))
                contentType = "Content-Type: image/jpeg\r\n";
            else if (fileName.endsWith(".gif"))
                contentType = "Content-Type: image/gif\r\n";
            else if (fileName.endsWith(".mp3"))
                contentType = "Content-Type: audio/mp3\r\n";
            else if (fileName.endsWith(".mp4"))
                contentType = "Content-Type: video/mp4\r\n";
            else if (fileName.endsWith(".avi"))
                contentType = "Content-Type: video/avi\r\n";
                //so on
            else
                //unspecified type
                contentType = "Content-Type: \r\n";

            // current time
            SimpleDateFormat simpleDateFormatString = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            currentTimeString = simpleDateFormatString.format(new Date()) + "\r\n";
            return this;
        }
    }
}
