package ru.dokwork.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Обрабатывает запрос клиента.
 */
public class ClientSession implements Runnable {
    private static final Map<String, String> CONTENT_TYPES = new HashMap<>() {{
        put("jpg", "image/jpeg");
        put("html", "text/html");
        put("json", "application/json");
        put("txt", "text/plain");
        put("", "text/plain");
    }};
    @Override
    public void run() {
        try {
            /* Получаем заголовок сообщения от клиента */
            String header = readHeader();
            System.out.println(header + "\n");
            /* Получаем из заголовка указатель на интересующий ресурс */
            String uri = getURIFromHeader(header);
            String requestType = getRequestType(header);
            System.out.println(requestType);
            System.out.println("Resource: " + uri + "\n");
            /* Отправляем содержимое ресурса клиенту */
            int code = send(uri, requestType);
            System.out.println("Result code: " + code + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public ClientSession(Socket socket) throws IOException {
        this.socket = socket;
        initialize();
    }

    private void initialize() throws IOException {
        /* Получаем поток ввода, в который помещаются сообщения от клиента */
        in = socket.getInputStream();
        /* Получаем поток вывода, для отправки сообщений клиенту */
        out = socket.getOutputStream();
    }

    /**
     * Считывает заголовок сообщения от клиента.
     *
     * @return строка с заголовком сообщения от клиента.
     * @throws IOException
     */
    private String readHeader() throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder builder = new StringBuilder();
        String ln = null;
        while (true) {
            ln = reader.readLine();
            if (ln == null || ln.isEmpty()) {
                break;
            }
            builder.append(ln + System.getProperty("line.separator"));
        }
        return builder.toString();
    }

    /**
     * Вытаскивает идентификатор запрашиваемого ресурса из заголовка сообщения от
     * клиента.
     *
     * @param header заголовок сообщения от клиента.
     * @return идентификатор ресурса.
     */
    private String getURIFromHeader(String header) {
        int from = header.indexOf(" ") + 1;
        int to = header.indexOf(" ", from);
        String uri = header.substring(from, to);
        int paramIndex = uri.indexOf("?");
        if (paramIndex != -1) {
            uri = uri.substring(0, paramIndex);
        }
        return DEFAULT_FILES_DIR + uri;
    }

    private String getRequestType(String header) {
        int lastIndex = header.indexOf(" ");
        return header.substring(0, lastIndex);
    }

    /**
     * Отправляет ответ клиенту. В качестве ответа отправляется http заголовок и
     * содержимое указанного ресурса. Если ресурс не указан, отправляется
     * перечень доступных ресурсов.
     *
     * @param uri         идентификатор запрашиваемого ресурса.
     * @param requestType
     * @return код ответа. 200 - если ресурс был найден, 404 - если нет.
     * @throws IOException
     */
    private int send(String uri, String requestType) throws IOException {
        int code = 0;
        //if (requestType.equals("GET")) {
            InputStream strm = HttpServer.class.getResourceAsStream(uri);
            code = (strm != null) ? 200 : 404;
            String fileExtension = getFileExtension(Path.of(uri));
            String contentType = CONTENT_TYPES.get(fileExtension);
            //String contentType = (uri.endsWith("xml")) ? "text/xml" : "text/html";
            String header = getHeaderGET(code, contentType);
            PrintStream answer = new PrintStream(out, true, "UTF-8");
            answer.print(header);
            if (code == 200) {
                int count = 0;
                byte[] buffer = new byte[1024];
                while ((count = strm.read(buffer)) != -1) {
                    out.write(buffer, 0, count);
                }
                strm.close();
            }
       // }
      //  if (requestType.equals("POST")){

      //  }
        return code;
    }

    /**
     * Возвращает http заголовок ответа.
     *
     * @param code        код результата отправки.
     * @param contentType тип содержимого ответа.
     * @return http заголовок ответа.
     */
    private String getHeaderGET(int code, String contentType) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("HTTP/1.1 " + code + " " + getAnswer(code) + "\n");
        buffer.append("Date: " + new Date() + "\n");
        buffer.append("Accept-Ranges: none\n");
        buffer.append("Content-Type: " + contentType + "\n");
        buffer.append("\n");
        return buffer.toString();
    }
    private String getHeaderPOST(int code, String contentType) {
        StringBuilder buffer = new StringBuilder();
        buffer.append("HTTP/1.1 " + code + " " + getAnswer(code) + "\n");
        buffer.append("Date: " + new Date() + "\n");
        buffer.append("Accept-Ranges: none\n");
        buffer.append("Content-Type: " + contentType + "\n");
        buffer.append("\n");
        return buffer.toString();
    }
    private String getFileExtension(Path path) {
        var name = path.getFileName().toString();
        var extensionStart = name.lastIndexOf(".");
        return extensionStart == -1 ? "" : name.substring(extensionStart + 1);
    }
    /**
     * Возвращает комментарий к коду результата отправки.
     *
     * @param code код результата отправки.
     * @return комментарий к коду результата отправки.
     */
    private String getAnswer(int code) {
        switch (code) {
            case 200:
                return "OK";
            case 404:
                return "Not Found";
            default:
                return "Internal Server Error";
        }
    }

    private Socket socket;
    private InputStream in = null;
    private OutputStream out = null;

    private static final String DEFAULT_FILES_DIR = "/www";
}
