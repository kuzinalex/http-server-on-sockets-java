import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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

    public static final String NOT_FOUND_MESSAGE = "NOT FOUND";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL SERVER ERROR";

    private Socket socket;
    private String directory;

    private BufferedReader in;
    private PrintWriter out;
    private BufferedOutputStream dataOut;
    private String method;

    Session(Socket socket, String directory) {
        this.socket = socket;
        this.directory = directory;
    }


    @Override
    public void run() {
        //System.out.println("HUY");
        String fileRequested = "";
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());
            dataOut = new BufferedOutputStream(socket.getOutputStream());

            String input = in.readLine();
            while (in.ready()) {
                input += in.readLine();
            }

            StringTokenizer parse;
            try {
                parse = new StringTokenizer(input);

            } catch (NullPointerException e) {
                return;
            }

            method = parse.nextToken();
            System.out.println(method);
            fileRequested = parse.nextToken();
            System.out.println(fileRequested);
            while (parse.hasMoreTokens()) {
                System.out.println(parse.nextToken());
            }
            switch (method) {
                case "GET":
                    System.out.println("get metod epta");
                    var filePath = Path.of(this.directory + fileRequested);
                    if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                        System.out.println(filePath);
                        var fileBytes = Files.readAllBytes(filePath);
                        var extension = this.getFileExtension(filePath);
                        var type = CONTENT_TYPES.get(extension);
                        var output = socket.getOutputStream();
                        setHeader(output, 200, "OK", type, fileBytes);
//                        output.write(fileBytes);
                    } else {
                        var type = CONTENT_TYPES.get("text");
                        var output = socket.getOutputStream();
                        this.setHeader(output, 404, "Not Found", type, NOT_FOUND_MESSAGE.getBytes());
                    }
                    socket.close();
                    break;
                case "POST":
                    System.out.println("post metod epta");
                    System.out.println(fileRequested);
                    filePath = Path.of(this.directory + fileRequested);
                    var extension = this.getFileExtension(filePath);
                    var type = CONTENT_TYPES.get(extension);
                    File file = new File(directory + fileRequested);
                    if (!file.exists()) {
                        this.setHeader(socket.getOutputStream(), 500, "Internal Server Error", type, INTERNAL_SERVER_ERROR.getBytes());
                        socket.close();
                        throw new FileNotFoundException();
                    } else {
                        System.out.println(file.getAbsolutePath());
                        FileInputStream inputStream = new FileInputStream(file.getAbsolutePath());
                        var data = inputStream.readAllBytes();
                        FileOutputStream fileOutputStream = new FileOutputStream(directory + "/new" + fileRequested);
                        fileOutputStream.write(data);
                        var outputStream = socket.getOutputStream();
                        setHeader(outputStream, 200, "OK", type, data);
                    }
                    socket.close();
                    break;
                case "OPTIONS":
                    // var bytes=socket.getInputStream().readAllBytes();
                    setHeader(socket.getOutputStream(), 200, "OK", "");
                    System.out.println("OPTIONS");
                    System.out.println(socket.isClosed() + "" + socket.isBound() + "" + socket.isConnected());
                    socket.close();
                    System.out.println(socket.isClosed());
                    break;
                default:
                    // methodNotAllowed(method);
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

//    private String getRequestUrl(InputStream inputStream) {
//        var reader = new Scanner(inputStream).useDelimiter("\r\n");
//        var line = reader.next();
//        return line.split(" ")[1];
//    }

//    private void createResponse(Path file) throws IOException {
//        out.println("HTTP/1.1 " + 200 + " " + "OK");
//        out.println("Server: HTTP Server");
//        out.println(String.format("Date: %s", Instant.now()));
//        out.println("Access-Control-Allow-Origin: localhost");
//        out.println("Access-Control-Allow-Methods: GET, POST, OPTIONS");
//        out.println("Content-Type: text/html; charset=utf-8");
//        out.println();
//        var fileBytes = Files.readAllBytes(file);
//        var output = this.socket.getOutputStream();
//        output.write(fileBytes);
//        out.flush();
//
//        try {
//            Thread.sleep(3000);
//        } catch (InterruptedException ignored) {
//            System.out.println("ignored ex");
//        }
//    }

    private void setHeader(OutputStream outputStream, int statusCode, String statusText, String type, byte[] fileBytes) {
        PrintStream ps = new PrintStream(outputStream);
        ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);
        ps.println("Server: HTTP Server");
        ps.println("Date: " + new Date());
        ps.println("Access-Control-Allow-Origin: localhost");
        ps.println("Access-Control-Allow-Methods: GET, POST, OPTIONS");
        ps.printf("Content-Type: %s%n", type);
        ps.printf("Content-Length: %s%n%n", fileBytes.length);
        if (method.equals("GET")) {
            try {
                outputStream.write(fileBytes);
            } catch (IOException e) {
                try {
                    this.setHeader(socket.getOutputStream(), 500, "Internal Server Error", type, NOT_FOUND_MESSAGE.getBytes());
                } catch (IOException exception) {
                    exception.printStackTrace();
                }
                e.printStackTrace();
            }
        }
    }

    private void setHeader(OutputStream outputStream, int statusCode, String statusText, String type) {
        PrintStream ps = new PrintStream(outputStream);
        ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);
        ps.println("Allow: OPTIONS, GET, POST");
        ps.println("Cache-Control: max-age=604800");
        ps.println("Date: " + new Date());
        ps.println("Expires: " + new Date());
        ps.println("Server: HTTP Server");
        ps.println("x-ec-custom-error: 1");
//        ps.println("Access-Control-Allow-Origin: localhost");
//        ps.println("Access-Control-Allow-Methods: GET, POST, OPTIONS");
//        ps.printf("Content-Type: text/html");
        ps.println("Content-Length: " + 0);
        ps.close();
    }
}
