import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Session extends Thread {

    public static final Map<String, String> CONTENT_TYPES = new HashMap<>() {{
        put("jpg", "image/jpeg");
        put("html", "text/html");
        put("json", "application/json");
        put("txt", "text/plain");
        put("", "text/plain");

    }};
    private static final Logger LOG = LogManager.getLogger(Session.class);
    private static final String NOT_FOUND_MESSAGE = "NOT FOUND";
    private static final String INTERNAL_SERVER_ERROR = "INTERNAL SERVER ERROR";
    private static final String NOT_IMPLEMENTED = "NOT IMPLEMENTED";

    private Socket socket;
    private String directory;

    private BufferedReader in;
    private OutputStream outputStream;
    private String method;

    Session(Socket socket, String directory) {
        this.socket = socket;
        this.directory = directory;
    }


    @Override
    public void run() {
        String fileRequested = "";
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String input = in.readLine();
            StringTokenizer parse;
            try {
                parse = new StringTokenizer(input);

            } catch (NullPointerException e) {
                return;
            }
            method = parse.nextToken();
            fileRequested = parse.nextToken();
            LOG.info("Input is: " + input);
            LOG.info("Request method: " + method);

            switch (method) {
                case "GET":
                    var filePath = Path.of(this.directory + fileRequested);
                    if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                        var fileBytes = Files.readAllBytes(filePath);
                        var extension = this.getFileExtension(filePath);
                        var type = CONTENT_TYPES.get(extension);
                        outputStream = socket.getOutputStream();
                        setHeader(outputStream, 200, "OK", type, fileBytes);
                        LOG.info("GET request was accepted");
                        outputStream.close();
                    } else {
                        var type = CONTENT_TYPES.get("text");
                        outputStream = socket.getOutputStream();
                        this.setHeader(outputStream, 404, "Not Found", type, NOT_FOUND_MESSAGE.getBytes());
                        outputStream.close();
                        LOG.warn(NOT_FOUND_MESSAGE);
                        LOG.warn("File: " + fileRequested + "not found, load");
                        LOG.info("Connection closed");
                        throw new FileNotFoundException();
                    }
                    socket.close();
                    LOG.info("Connection closed");
                    break;
                case "POST":
                    filePath = Path.of(this.directory + fileRequested);
                    var extension = this.getFileExtension(filePath);
                    var type = CONTENT_TYPES.get(extension);
                    File file = new File(directory + fileRequested);
                    outputStream = socket.getOutputStream();
                    if (!file.exists() || file.isDirectory()) {
                        this.setHeader(outputStream, 500, "Internal Server Error", type, INTERNAL_SERVER_ERROR.getBytes());
                        socket.close();
                        LOG.warn(INTERNAL_SERVER_ERROR);
                        LOG.warn("File: " + fileRequested + "not found, load");
                        LOG.info("Connection closed");
                        throw new FileNotFoundException();
                    } else {
                        FileInputStream inputStream = new FileInputStream(file.getAbsolutePath());
                        var data = inputStream.readAllBytes();
                        FileOutputStream fileOutputStream = new FileOutputStream(directory + "/new" + fileRequested);
                        fileOutputStream.write(data);
                        outputStream = socket.getOutputStream();
                        setHeader(outputStream, 200, "OK", type, data);
                        LOG.info("POST request was accepted");
                        outputStream.close();
                    }
                    socket.close();
                    LOG.info("Connection closed");
                    break;
                case "OPTIONS":
                    filePath = Path.of(this.directory + fileRequested);
                    extension = this.getFileExtension(filePath);
                    type = CONTENT_TYPES.get(extension);
                    file = new File(directory + fileRequested);
                    outputStream = socket.getOutputStream();
                    if (!file.exists()) {
                        this.setHeader(outputStream, 404, "Not Found", type, NOT_FOUND_MESSAGE.getBytes());
                        socket.close();
                        LOG.warn(NOT_FOUND_MESSAGE);
                        LOG.warn("File: " + fileRequested + "not found, load");
                        LOG.info("Connection closed");
                        throw new FileNotFoundException();
                    } else {
                        outputStream = socket.getOutputStream();
                        setHeader(outputStream, 200, "OK", type, null);
                        LOG.info("OPTIONS request was accepted");
                        outputStream.close();
                    }
                    socket.close();
                    LOG.info("Connection closed");
                    break;
                default:
                    outputStream = socket.getOutputStream();
                    filePath = Path.of(this.directory + fileRequested);
                    extension = this.getFileExtension(filePath);
                    type = CONTENT_TYPES.get(extension);
                    this.setHeader(outputStream, 501, "Not Implemented", type, NOT_IMPLEMENTED.getBytes());
                    LOG.warn("Unknown method: " + method);
                    socket.close();
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getFileExtension(Path path) {
        var name = path.getFileName().toString();
        var extensionStart = name.lastIndexOf(".");
        return extensionStart == -1 ? "" : name.substring(extensionStart + 1);

    }

    private void setHeader(OutputStream outputStream, int statusCode, String statusText, String type, byte[] fileBytes) {
        PrintStream ps = new PrintStream(outputStream);
        ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);
        ps.println("Server: HTTP Server");
        ps.println("Date: " + new Date());
        ps.println("Connection: Keep-Alive");

        if (method.equals("GET")) {
            try {
                ps.printf("Content-Type: %s%n", type);
                ps.printf("Content-Length: %s%n%n", fileBytes.length);
                outputStream.write(fileBytes);
            } catch (IOException e) {
                try {
                    this.setHeader(socket.getOutputStream(), 500, "Internal Server Error", type, NOT_FOUND_MESSAGE.getBytes());
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                e.printStackTrace();
            }
        } else if (method.equals("POST")) {
            ps.printf("Content-Length: %s%n%n", 0);
        } else if (method.equals("OPTIONS")) {
            ps.println("Access-Control-Allow-Origin: localhost");
            ps.println("Access-Control-Allow-Methods: GET, POST, OPTIONS");
            ps.printf("Content-Length: %s%n%n", 0);
        } else {
            ps.printf("Content-Length: %s%n%n", 0);
        }
    }
}
