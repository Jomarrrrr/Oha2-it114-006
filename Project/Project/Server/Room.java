package Project.Server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import Project.Common.Constants;

public class Room implements AutoCloseable {
    // protected static Server server;// used to refer to accessible server
    // functions
    private String name;
    private List<ServerThread> clients = new ArrayList<ServerThread>();

    private boolean isRunning = false;
    // Commands
    private final static String COMMAND_TRIGGER = "/";
    // private final static String CREATE_ROOM = "createroom";
    // private final static String JOIN_ROOM = "joinroom";
    // private final static String DISCONNECT = "disconnect";
    // private final static String LOGOUT = "logout";
    // private final static String LOGOFF = "logoff";
    private Logger logger = Logger.getLogger(Room.class.getName());

    public Room(String name) {
        this.name = name;
        isRunning = true;
    }

    private void info(String message) {
        logger.info(String.format("Room[%s]: %s", name, message));
    }

    public String getName() {
        return name;
    }

    protected synchronized void addClient(ServerThread client) {
        if (!isRunning) {
            return;
        }
        client.setCurrentRoom(this);
        client.sendJoinRoom(getName());// clear first
        if (clients.indexOf(client) > -1) {
            info("Attempting to add a client that already exists");
        } else {
            clients.add(client);
            // connect status second
            sendConnectionStatus(client, true);
            syncClientList(client);
        }


    }

    protected synchronized void removeClient(ServerThread client) {
        if (!isRunning) {
            return;
        }
        clients.remove(client);
        // we don't need to broadcast it to the server
        // only to our own Room
        if (clients.size() > 0) {
            // sendMessage(client, "left the room");
            sendConnectionStatus(client, false);
        }
        checkClients();
    }

    /***
     * Checks the number of clients.
     * If zero, begins the cleanup process to dispose of the room
     */
    private void checkClients() {
        // Cleanup if room is empty and not lobby
        if (!name.equalsIgnoreCase(Constants.LOBBY) && clients.size() == 0) {
            close();
        }
    }

    /***
     * Helper function to process messages to trigger different functionality.
     * 
     * @param message The original message being sent
     * @param client  The sender of the message (since they'll be the ones
     *                triggering the actions)
     */
    private boolean processCommands(String message, ServerThread client) {
        boolean wasCommand = false;
        try {
            if (message.startsWith(COMMAND_TRIGGER)) {
                String[] comm = message.split(COMMAND_TRIGGER);
                String part1 = comm[1];
                String[] comm2 = part1.split(" ");
                String command = comm2[0];
                // String roomName;
                wasCommand = true;
                switch (command) {

                    /*
                     * case CREATE_ROOM:
                     * roomName = comm2[1];
                     * Room.createRoom(roomName, client);
                     * break;
                     * case JOIN_ROOM:
                     * roomName = comm2[1];
                     * Room.joinRoom(roomName, client);
                     * break;
                     */
                    /*
                     * case DISCONNECT:
                     * case LOGOUT:
                     * case LOGOFF:
                     * Room.disconnectClient(client, this);
                     * break;
                     */
                    default:
                        wasCommand = false;
                        break;
                    }
                } else {
                    if (message.contains("*") || message.contains("!") || message.contains("_") || message.contains("#")) {
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
                    
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return wasCommand;
        }

    // Command helper methods
    private synchronized void syncClientList(ServerThread joiner) {
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread st = iter.next();
            if (st.getClientId() != joiner.getClientId()) {
                joiner.sendClientMapping(st.getClientId(), st.getClientName());
            }
        }
    }
    protected static void createRoom(String roomName, ServerThread client) {
        if (Server.INSTANCE.createNewRoom(roomName)) {
            // server.joinRoom(roomName, client);
            Room.joinRoom(roomName, client);
        } else {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, String.format("Room %s already exists", roomName));
        }
    }
    public static void flip(String message, ServerThread client) {
        try {
            int coin = rand.nextInt(2);
            if (coin == 1){
                String heads = "<b style=color:blue>You got heads!</b>";
                sendMessage(client, heads);
            } else {
                String tails = "<b style=color:orange>You got tails!</b>";
                sendMessage(client, tails);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void roll(String message, ServerThread client) {
        String[] parts = message.split("\\s+");
        if (parts.length == 2) {
            // Format 1: /roll 0 - X or 1 - X
            if (parts[1].matches("\\d+")) {
                int sides = Integer.parseInt(parts[1]);
                if (sides > 0) {
                    int result = (int) (Math.random() * sides) + 1;
                    client.sendMessage(Constants.DEFAULT_CLIENT_ID, "Rolled a " + sides + "-sided die, result: " + result);
                    return;
                }
            }
        } else if (parts.length == 2 && parts[1].matches("\\d+d\\d+")) {
            // Format 2: /roll #d#
            String[] diceParts = parts[1].split("d");
            int numDice = Integer.parseInt(diceParts[0]);
            int sides = Integer.parseInt(diceParts[1]);
            if (numDice > 0 && sides > 0) {
                int total = 0;
                StringBuilder rollResults = new StringBuilder("Rolled " + numDice + " " + sides + "-sided dice, results: ");
                for (int i = 0; i < numDice; i++) {
                    int roll = (int) (Math.random() * sides) + 1;
                    total += roll;
                    rollResults.append(roll).append(", ");
                }
                rollResults.delete(rollResults.length() - 2, rollResults.length()); // Remove last ", "
                rollResults.append("Total: ").append(total);
                client.sendMessage(Constants.DEFAULT_CLIENT_ID, rollResults.toString());
                return;
            }
        }
        // Invalid roll format
        client.sendMessage(Constants.DEFAULT_CLIENT_ID, "Invalid roll format. Usage: /roll 0-X or 1-X, or #d#");
    }


    protected static void joinRoom(String roomName, ServerThread client) {
        if (!Server.INSTANCE.joinRoom(roomName, client)) {
            client.sendMessage(Constants.DEFAULT_CLIENT_ID, String.format("Room %s doesn't exist", roomName));
        }
    }

    protected static List<String> listRooms(String searchString, int limit) {
        return Server.INSTANCE.listRooms(searchString, limit);
    }

    protected static void disconnectClient(ServerThread client, Room room) {
        client.setCurrentRoom(null);
        client.disconnect();
        room.removeClient(client);
    }


    // end command helper methods

    /***
     * Takes a sender and a message and broadcasts the message to all clients in
     * this room. Client is mostly passed for command purposes but we can also use
     * it to extract other client info.
     * 
     * @param sender  The client sending the message
     * @param message The message to broadcast inside the room
     */
    protected synchronized void sendMessage(ServerThread sender, String message) {
        if (!isRunning) {
            return;
        }
        info("Sending message to " + clients.size() + " clients");
        if (sender != null && processCommands(message, sender)) {
            // it was a command, don't broadcast
            return;
        }

        /// String from = (sender == null ? "Room" : sender.getClientName());
        long from = (sender == null) ? Constants.DEFAULT_CLIENT_ID : sender.getClientId();
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread client = iter.next();
            boolean messageSent = client.sendMessage(from, message);
            if (!messageSent) {
                handleDisconnect(iter, client);
            }
        }
    }

    protected synchronized void sendConnectionStatus(ServerThread sender, boolean isConnected) {
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread client = iter.next();
            boolean messageSent = client.sendConnectionStatus(sender.getClientId(), sender.getClientName(),
                    isConnected);
            if (!messageSent) {
                handleDisconnect(iter, client);
            }
        }
    }

    private void handleDisconnect(Iterator<ServerThread> iter, ServerThread client) {
        iter.remove();
        info("Removed client " + client.getClientName());
        checkClients();
        sendMessage(null, client.getClientName() + " disconnected");
    }

    public void close() {
        Server.INSTANCE.removeRoom(this);
        // server = null;
        isRunning = false;
        clients = null;
    }

        
}