import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Chat client.
 * Two things must happen at the same time: (1) show messages coming from the
 * server and (2) read what the user types. So messages are received on a
 * separate thread while the main thread reads the keyboard.
 */
public class ChatClient {

    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = 5000;
        if (args.length > 1) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port '" + args[1] + "'. Using 5000.");
            }
        }

        try (Socket socket = new Socket(host, port);
             BufferedReader fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter toServer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to " + host + ":" + port);

            // Thread that prints everything the server sends
            Thread receiver = new Thread(() -> {
                try {
                    String message;
                    while ((message = fromServer.readLine()) != null) {
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    // socket closed - normal when we quit
                }
                System.out.println("Disconnected from server.");
                System.exit(0);
            });
            receiver.setDaemon(true);
            receiver.start();

            // Main thread: send what the user types
            String input;
            while ((input = keyboard.readLine()) != null) {
                toServer.println(input);
                if (input.trim().equalsIgnoreCase("/quit")) {
                    break;
                }
            }
        } catch (UnknownHostException e) {
            System.out.println("Unknown host: " + host);
        } catch (IOException e) {
            System.out.println("Could not connect to " + host + ":" + port
                    + " - is the server running? (" + e.getMessage() + ")");
        }
    }
}
