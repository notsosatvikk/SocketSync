import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

// Event class representing an event to be scheduled
class Event implements Serializable {
    private static final long serialVersionUID = 1L;
    private String date;
    private String eventName;

    public Event(String date, String eventName) {
        this.date = date;
        this.eventName = eventName;
    }

    public String getDate() {
        return date;
    }

    public String getEventName() {
        return eventName;
    }
}

// EventScheduler class responsible for managing events
class EventScheduler {
    private HashMap<String, String> eventMap; // Date -> EventName

    public EventScheduler() {
        eventMap = new HashMap<>();
    }

    public synchronized boolean scheduleEvent(Event event) {
        String date = event.getDate();
        if (!eventMap.containsKey(date)) {
            eventMap.put(date, event.getEventName());
            return true; // Event scheduled successfully
        }
        return false; // Event already scheduled for the given date
    }

    public synchronized HashMap<String, String> getScheduledEvents() {
        return new HashMap<>(eventMap);
    }
}

// EventSchedulerServer class representing the server that handles event scheduling
public class EventSchedulerServer extends JFrame {
    private int port;
    private EventScheduler eventScheduler;
    public static JTextArea eventLogArea; // Change to public static

    public EventSchedulerServer(int port) {
        this.port = port;
        this.eventScheduler = new EventScheduler();

        setTitle("Event Scheduler Server");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        eventLogArea = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(eventLogArea);
        add(scrollPane, BorderLayout.CENTER);
    }

    // Start the event server
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            appendToEventLog("Event Scheduler Server started on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                appendToEventLog("New client connected: " + socket);

                // Handle client connection in a separate thread
                new Thread(new ClientHandler(socket, eventScheduler)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Append text to the event log area
    public static void appendToEventLog(String text) {
        eventLogArea.append(text + "\n");
    }

    public static void main(String[] args) {
        // Start the event server
        EventSchedulerServer eventServer = new EventSchedulerServer(5000);
        eventServer.setVisible(true);
        eventServer.startServer();
    }
}

// ClientHandler class for handling individual client connections
class ClientHandler implements Runnable {
    private Socket socket;
    private EventScheduler eventScheduler;

    public ClientHandler(Socket socket, EventScheduler eventScheduler) {
        this.socket = socket;
        this.eventScheduler = eventScheduler;
    }

    @Override
    public void run() {
        try (
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            while (true) {
                // Receive event request from client
                try {
                    Event event = (Event) in.readObject();
                    EventSchedulerServer.appendToEventLog("Received event from client: " + event.getEventName());

                    // Prompt server to approve scheduling
                    int option = JOptionPane.showConfirmDialog(null, "Do you want to schedule this event?", "Event Approval", JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.YES_OPTION) {
                        // Schedule the event
                        boolean success = eventScheduler.scheduleEvent(event);
                        if (success) {
                            out.writeObject("Event scheduled successfully.");
                        } else {
                            out.writeObject("Event already scheduled for the given date.");
                        }
                    } else {
                        out.writeObject("Event scheduling declined by server.");
                    }
                    out.flush();

                    // Send scheduled events to client
                    HashMap<String, String> scheduledEvents = eventScheduler.getScheduledEvents();
                    out.writeObject(scheduledEvents);
                    out.flush();

                    // Print scheduled events on server side
                    EventSchedulerServer.appendToEventLog("Scheduled Events:");
                    for (String date : scheduledEvents.keySet()) {
                        EventSchedulerServer.appendToEventLog(date + ": " + scheduledEvents.get(date));
                    }
                } catch (EOFException e) {
                    EventSchedulerServer.appendToEventLog("Client disconnected.");
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
