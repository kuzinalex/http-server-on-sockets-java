import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Session extends Thread {

    public static final Map<String, String> CONTENT_TYPES = new HashMap<>() {{
        put("jpg", "image/jpeg");
        put("html", "text/html");
        put("json", "application/json");
        put("txt", "text/plain");
        put("", "text/plain");

    }};

    public static final String NOT_FOUND_MESSAGE = "NOT FOUND";

    private Socket socket;

    private String directory;

    Session(Socket socket, String directory) {
        this.socket = socket;
        this.directory = directory;
    }


    @Override
    public void run() {
        try (var input = this.socket.getInputStream(); var output = this.socket.getOutputStream();) {
            var url = this.getRequestUrl(input);
            var filePath = Path.of(this.directory + url);
            if (Files.exists(filePath) && !Files.isDirectory(filePath)) {
                var extension=this.getFileExtension(filePath);
                var type=CONTENT_TYPES.get(extension);
                var fileBytes=Files.readAllBytes(filePath);
                this.setHeader(output,200,"OK",type,fileBytes.length);
                output.write(fileBytes);
            } else {
                var type = CONTENT_TYPES.get("text");
                this.setHeader(output, 404, "Not Found", type, NOT_FOUND_MESSAGE.length());
                output.write(NOT_FOUND_MESSAGE.getBytes());
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

    private String getRequestUrl(InputStream inputStream) {
        var reader = new Scanner(inputStream).useDelimiter("\r\n");
        var line = reader.next();
        return line.split(" ")[1];
    }

    private void setHeader(OutputStream outputStream, int statusCode, String statusText, String type, long length) {
        var ps = new PrintStream(outputStream);
        ps.printf("HTTP/1.1 %s %s%n", statusCode, statusText);
        ps.printf("Content-Type: %s%n", type);
        ps.printf("Content-Length: %s%n%n", length);
    }
}
