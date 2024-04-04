package Project.Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

import Project.Common.Constants;
import Project.Common.FlipPayload;
import Project.Common.RollPayload;
import Project.Common.ServerConstants;
import Project.Common.TextFX;
import Project.Common.TextFX.Color;

import java.util.List;

public enum Server {
    INSTANCE;

    int port = 3001;
    private List<Room> rooms = new ArrayList<Room>();
    private Room lobby = null;// default room
    private Queue<ServerThread> incomingClients = new LinkedList<ServerThread>();
    private boolean isRunning = true;
    private long nextClientId = 1;// <-- uniquely identifies clients (could use a UUID but we're keeping it basic)
    private Logger logger = Logger.getLogger(Server.class.getName());

    private void start(int port) {
        this.port = port;
        // server listening
        try (ServerSocket serverSocket = new ServerSocket(port);) {
            Socket incoming_client = null;
            logger.info("Server is listening on port " + port);
            startQueueManager();
            // create a lobby on start
            lobby = new Room(Constants.LOBBY);
            rooms.add(lobby);
            do {
                logger.info("waiting for next client");
                if (incoming_client != null) {
                    logger.info("Client connected");
                    ServerThread sClient = new ServerThread(incoming_client);

                    sClient.start();
                    incomingClients.add(sClient);
                    incoming_client = null;

                }
            } while ((incoming_client = serverSocket.accept()) != null);
        } catch (IOException e) {
            logger.severe("Error accepting connection");
            e.printStackTrace();
        } finally {
            logger.info("closing server socket");
        }
    }

    private void startQueueManager() {
        logger.info(TextFX.colorize("Starting queue manager", Color.PURPLE));
        new Thread() {
            @Override
            public void run() {
                while (isRunning) {
                    try {
                        Thread.sleep(10);
                    } catch (Exception e) {
                        logger.severe("Some thread problem");
                        e.printStackTrace();
                    }
                    if (incomingClients.size() > 0) {
                        ServerThread incomingClient = incomingClients.peek();
                        if (incomingClient.isRunning() && incomingClient.getClientName() != null) {
                            handleIncomingClient(incomingClient);
                            incomingClients.poll();// <-- remove the handled client
                        }
                    }
                }
                logger.info(TextFX.colorize("Terminating queue manager", Color.PURPLE));
            }
        }.start();
    }

    private void handleIncomingClient(ServerThread incomingClient) {
        incomingClient.setClientId(nextClientId);
        // incomingClient.sendClientId(nextClientId);
        nextClientId++;
        if (nextClientId < 0) {
            nextClientId = 1; // <-- by some lucky chance it overflows, restart the count
        }
        joinRoom(Constants.LOBBY, incomingClient);
    }
    /***
     * Helper function to check if room exists by case insensitive name
     * 
     * @param roomName The name of the room to look for
     * @return matched Room or null if not found
     */
    private Room getRoom(String roomName) {
        for (int i = 0, l = rooms.size(); i < l; i++) {
            if (rooms.get(i).getName().equalsIgnoreCase(roomName)) {
                return rooms.get(i);
            }
        }
        return null;
    }

    /***
     * Attempts to join a room by name. Will remove client from old room and add
     * them to the new room.
     * 
     * @param roomName The desired room to join
     * @param client   The client moving rooms
     * @return true if reassign worked; false if new room doesn't exist
     */
    protected synchronized boolean joinRoom(String roomName, ServerThread client) {
        Room newRoom = roomName.equalsIgnoreCase(Constants.LOBBY) ? lobby : getRoom(roomName);
        Room oldRoom = client.getCurrentRoom();
        if (newRoom != null) {
            if (oldRoom != null) {
                logger.info(client.getName() + " leaving room " + oldRoom.getName());
                oldRoom.removeClient(client);
            }
            logger.info(client.getName() + " joining room " + newRoom.getName());
            newRoom.addClient(client);
            return true;
        } else {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID,
                    String.format("Room %s wasn't found, please try another", roomName));
        }
        return false;
    }

    /**
     * Gets a result of rooms similar to the search string, up to 10
     * 
     * @param searchString
     * @return
     */
    protected synchronized List<String> listRooms(String searchString) {
        return listRooms(searchString, 10);
    }

    /**
     * Gets a result of rooms similar to the search string, between 1-100 results
     * 
     * @param searchString
     * @param limit
     * @return
     */
    protected synchronized List<String> listRooms(String searchString, int limit) {
        // TODO
        if (limit < 1 || limit > 100) {
            return null;
        }
        List<String> matchedRooms = new ArrayList<String>();
        Iterator<Room> iter = rooms.iterator();
        while (iter.hasNext()) {
            Room currentRoom = iter.next();
            logger.fine("Checking Room " + currentRoom.getName());
            // if we don't have a particular search, simply add the room
            if (searchString == null || searchString.isBlank()) {
                matchedRooms.add(currentRoom.getName());
            } // if we have a particular search and the room name is "like" the search string,
              // add the room
            else if (currentRoom.getName().toLowerCase().contains(searchString.toLowerCase())) {
                matchedRooms.add(currentRoom.getName());
            }
            if (matchedRooms.size() >= limit) {
                break;
            }
        }

        return matchedRooms;
    }

    /***
     * Attempts to create a room with given name if it doesn't exist already.
     * 
     * @param roomName The desired room to create
     * @return true if it was created and false if it exists
     */

     //oha2 4/1/2024
    protected synchronized boolean createNewRoom(String roomName) {
        if (getRoom(roomName) != null) {
            // TODO can't create room
            logger.warning(String.format("Room %s already exists", roomName));
            return false;
        } else {
            // Chatroom probably doesn't need gameroom and can just have this line
            // uncommented instead
            Room room = new Room(roomName);
            // other projects, any new room is a GameRoom
           // GameRoom room = new GameRoom(roomName);
            rooms.add(room);
            logger.info("Created new room: " + roomName);
            return true;
        }
    }

    protected synchronized void removeRoom(Room r) {
        if (rooms.removeIf(room -> room == r)) {
            logger.info("Removed empty room " + r.getName());
        }
    }

    /**
     * This would be used for sending messages across Rooms (most of your logic will
     * be in the Room class rather than here)
     * 
     * @param message
     */
    protected synchronized void broadcast(String message) {
        if (processCommand(message)) {

            return;
        }
        // loop over rooms and send out the message
        Iterator<Room> it = rooms.iterator();
        while (it.hasNext()) {
            Room room = it.next();
            if (room != null) {
                room.sendMessage(ServerConstants.FROM_ROOM, message);
            }
        }
    }
    //oha2 4/3/2024
    private boolean processCommand(String message) {
        logger.info("Checking command: " + message);
        
        // Check if the message is a command
        if (message.startsWith("/")) {
            String[] parts = message.split(" ");
            String command = parts[0].toLowerCase(); // Extract the command
    
  switch (command) {
    case "/roll":
        if (parts.length == 2) {
            String argument = parts[1].toLowerCase();
            if (argument.matches("\\d+d\\d+")) { // Check if it's in the format "NdN"
                // Process the roll command with NdN format
                String[] rollParams = argument.split("d");
                int numDice = Integer.parseInt(rollParams[0]);
                int numSides = Integer.parseInt(rollParams[1]);
                RollPayload rollPayload = new RollPayload(numDice, numSides);
                broadcast("Rolled " + numDice + "d" + numSides + ": " + rollPayload.toString());
            } else if (argument.matches("\\d+")) { // Check if it's in the format "0-X" or "1-X"
                int startValue = Integer.parseInt(argument);
                broadcast("Starting at: " + startValue);
            } else {
                // Invalid roll format
                logger.info("Invalid roll format: " + argument);
            }
        } else {
            // Invalid number of arguments for /roll command
            logger.info("Invalid number of arguments for /roll command");
        }
        return true;
    case "/flip":
        // Process the flip command
        String result = Math.random() < 0.5 ? "Heads" : "Tails";
        FlipPayload flipPayload = new FlipPayload(result);
        broadcast("Coin flip result: " + flipPayload.toString());
        return true;
    default:
        // Unknown command
        logger.info("Unknown command: " + command);
        return false;
}
        } else {
            // Process text formatting commands
            if (message.contains("*") || message.contains("!") || message.contains("_") || message.contains("#")) {
                // Apply formatting based on symbols
                message = applyFormatting(message);
                
                broadcast(message);
                
                return true;
            }
        }
        return false;
    }
    
    private String applyFormatting(String message) {
        // Replace * with <b> and </b> tags for bold
        message = message.replaceAll("\\*(.*?)\\*", "<b>$1</b>");
        
        // Replace ! with <i> and </i> tags for italic
        message = message.replaceAll("!(.*?)!", "<i>$1</i>");
        
        // Replace _ with <u> and </u> tags for underline
        message = message.replaceAll("_(.*?)_", "<u>$1</u>");

        //the color ones 
        message = message.replaceAll("#r(.*?)r#", "< color='red'>$1</color>");

        message = message.replaceAll("#b(.*?)b#", "< color='blue'>$1</color>");

        message = message.replaceAll("#g(.*?)g#", "< color='green'>$1</color>");
        
        return message;
    }
    

    public static void main(String[] args) {
        Server.INSTANCE.logger.info("Starting Server");
        Server server = Server.INSTANCE;// new Server();
        int port = 3000;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            // can ignore, will either be index out of bounds or type mismatch
            // will default to the defined value prior to the try/catch
        }
        server.start(port);
        Server.INSTANCE.logger.info("Server Stopped");
    }
}