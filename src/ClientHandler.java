import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Handles ONE connected client. Each ClientHandler runs on its own thread.
 * It reads what the client types and reacts to it (chat message or command).
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;
    private PrintWriter out;
    private String username;          // set after the client picks a valid name

    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    public String getUsername() {
        return username;
    }

    /** Sends one line of text to this client. */
    public void send(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

            this.out = writer;

            if (!askForUsername(in)) {
                return;                          // client disconnected before choosing a name
            }

            send("Welcome, " + username + "! Type /help to see commands.");
            send("Online now: " + server.userList());
            server.broadcast("*** " + username + " joined the chat ***", this);
            ChatServer.log(username + " joined");

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                if (line.equalsIgnoreCase("/quit")) {
                    break;
                }
                handleInput(line);
            }
        } catch (IOException e) {
            // Happens when a client closes the window suddenly - not a crash, just log it.
            ChatServer.log("Connection problem with " + (username != null ? username : "a client")
                    + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    /** Keeps asking until the client gives a valid, unused username. */
    private boolean askForUsername(BufferedReader in) throws IOException {
        while (true) {
            send("Enter your username (2-15 letters, digits or _):");
            String name = in.readLine();
            if (name == null) {
                return false;
            }
            name = name.trim();
            if (!name.matches("[A-Za-z0-9_]{2,15}")) {
                send("Invalid username. Use 2-15 letters, digits or underscore.");
            } else if (!server.register(name, this)) {
                send("Username '" + name + "' is already taken. Try another one.");
            } else {
                this.username = name;
                return true;
            }
        }
    }

    private void handleInput(String line) {
        if (line.startsWith("/")) {
            handleCommand(line);
        } else {
            server.broadcast(username + ": " + line, this);
        }
    }

    private void handleCommand(String line) {
        String[] parts = line.split("\\s+", 3);
        String command = parts[0].toLowerCase();

        switch (command) {
            case "/help":
                send("Commands:");
                send("  /msg <user> <text>  - send a private message");
                send("  /users              - list online users");
                send("  /quit               - leave the chat");
                break;
            case "/users":
                send("Online now: " + server.userList());
                break;
            case "/msg":
                if (parts.length < 3) {
                    send("Usage: /msg <user> <text>");
                } else if (parts[1].equalsIgnoreCase(username)) {
                    send("You cannot send a private message to yourself.");
                } else if (server.sendPrivate(username, parts[1], parts[2])) {
                    send("[private to " + parts[1] + "] " + parts[2]);
                } else {
                    send("User '" + parts[1] + "' is not online.");
                }
                break;
            default:
                send("Unknown command. Type /help.");
        }
    }

    /** Cleans up when the client leaves (normally or suddenly). */
    private void disconnect() {
        if (username != null) {
            server.remove(username);
            server.broadcast("*** " + username + " left the chat ***", this);
            ChatServer.log(username + " left");
        }
        try {
            socket.close();
        } catch (IOException ignored) {
            // nothing more we can do
        }
    }
}
