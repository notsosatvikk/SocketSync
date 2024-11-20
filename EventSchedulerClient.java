import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

// EventSchedulerClient class representing the client that communicates with the server
public class EventSchedulerClient {
    private String serverAddress;
    private int serverPort;

    public EventSchedulerClient(String serverAddress, int serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    // Method to read user input for events
    private Event readEventFromUser() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter event date (YYYY-MM-DD): ");
        String date = scanner.nextLine();
        System.out.print("Enter event name: ");
        String eventName = scanner.nextLine();
        return new Event(date, eventName);
    }

    // Start the event client
    public void startClient() {
        try (
            Socket socket = new Socket(serverAddress, serverPort);
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            // Read user input and send events to the server
            Scanner scanner = new Scanner(System.in);
            while (true) {
                // Read event from user
                Event event = readEventFromUser();
                
                // Send event to server
                out.writeObject(event);
                out.flush();
                
                // Receive server response
                String response = (String) in.readObject();
                System.out.println("Server response: " + response);
                
                // Receive scheduled events from server
                HashMap<String, String> scheduledEvents = (HashMap<String, String>) in.readObject();
                System.out.println("Scheduled Events received from server:");
                for (String date : scheduledEvents.keySet()) {
                    System.out.println(date + ": " + scheduledEvents.get(date));
                }

                // Ask user if they want to enter another event
                System.out.print("Do you want to enter another event? (yes/no): ");
                String answer = scanner.nextLine();
                if (!answer.equalsIgnoreCase("yes")) {
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Provide the server address and port where the server is running
        String serverAddress = "SERVER_IP_ADDRESS"; // Replace SERVER_IP_ADDRESS with the actual IP address
        int serverPort = 5000; // Use the same port number as configured in the server

        // Start the event client
        EventSchedulerClient eventClient = new EventSchedulerClient(serverAddress, serverPort);
        eventClient.startClient();
    }
}
