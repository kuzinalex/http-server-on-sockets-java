import java.io.IOException;
import java.net.ServerSocket;

public class HttpServer {
    private int port;

    private String directory;

    public HttpServer(int port, String directory) {
        this.port = port;
        this.directory = directory;
    }

    void start(){
        try( var server= new ServerSocket(this.port)){
            while (true){
                var socket=server.accept();
                var thread=new Session(socket,this.directory);
                thread.start();
            }

        }catch (IOException exception){
            exception.printStackTrace();
        }
    }

    public static void main(String[] args) {
        var port=Integer.parseInt(args[0]);
        var directory=args[1];
        new HttpServer(port,directory).start();
    }
}
