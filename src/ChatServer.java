import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Multi-client chat server.
 *
 * How it works:
 *  1. The server listens on a TCP port using a ServerSocket.
 *  2. Every time a client connects, accept() returns a new Socket.
 *  3. That Socket is given to a ClientHandler which runs on its own Thread,
 *     so many clients can chat at the same time.
 *  4. All connected users are kept in a thread-safe map (ConcurrentHashMap).
 */
public class ChatServer {

    private static final int DEFAULT_PORT = 5000;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final int port;
    // key = username in lower case (so "Ali" and "ali" count as the same user)
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public ChatServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            log("Server started on port " + port + ". Waiting for clients...");
            while (true) {
                Socket socket = serverSocket.accept();          // blocks until a client connects
                log("New connection from " + socket.getInetAddress().getHostAddress());
                new Thread(new ClientHandler(socket, this)).start(); // one thread per client
            }
        }
    }

    /** Adds the user. Returns false if the username is already taken. */
    boolean register(String username, ClientHandler handler) {
        return clients.putIfAbsent(username.toLowerCase(), handler) == null;
    }

    void remove(String username) {
        clients.remove(username.toLowerCase());
    }

    /** Sends a message to every connected user except the sender (pass null to send to all). */
    void broadcast(String message, ClientHandler except) {
        for (ClientHandler handler : clients.values()) {
            if (handler != except) {
                handler.send(message);
            }
        }
    }

    /** Sends a private message. Returns false if the target user is not online. */
    boolean sendPrivate(String from, String to, String message) {
        ClientHandler target = clients.get(to.toLowerCase());
        if (target == null) {
            return false;
        }
        target.send("[private from " + from + "] " + message);
        return true;
    }

    String userList() {
        return clients.values().stream()
                .map(ClientHandler::getUsername)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", "));
    }

    static void log(String message) {
        System.out.println("[" + LocalTime.now().format(TIME) + "] " + message);
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port '" + args[0] + "'. Using default " + DEFAULT_PORT);
            }
        }
        try {
            new ChatServer(port).start();
        } catch (IOException e) {
            System.out.println("Could not start server on port " + port + ": " + e.getMessage());
        }
    }
}
