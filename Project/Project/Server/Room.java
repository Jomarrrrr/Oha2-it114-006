package Project.Server;

import java.util.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import Project.Common.Constants;
import Project.Client.Client;

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
                    case "flip":

                        Random num = new Random();
                        int randomNum = num.nextInt(2) + 1;
                        String face = "test";
                        if (randomNum == 1) {
                            face = "<b style=color:blue>You got heads!</b>";
                            sendMessage(null, String.format("" + face));
                        } else {
                            face = "<b style=color:orange>You got tails!</b>";
                            sendMessage(null, String.format("" + face));
                        }
                        sendMessage(null, String.format("testo"));

                        break;
                    case "roll":
                        String[] parts = message.split("\\s+");
                        if (parts.length == 2) {
                            // Format 1: /roll 0 - X or 1 - X
                            if (parts[1].matches("\\d+")) {
                                int sides = Integer.parseInt(parts[1]);
                                if (sides > 0) {
                                    int result = (int) (Math.random() * sides) + 1;
                                    sendMessage(null, "<b style=color:blue>Rolled a " + sides + "-sided die, result: "
                                            + result + "</b>");

                                }
                            }
                        } else if (parts.length == 2 && parts[1].matches("\\d+d\\d+")) {
                            // Format 2: /roll #d#
                            String[] diceParts = parts[1].split("d");
                            int numDice = Integer.parseInt(diceParts[0]);
                            int sides = Integer.parseInt(diceParts[1]);
                            if (numDice > 0 && sides > 0) {
                                int total = 0;
                                StringBuilder rollResults = new StringBuilder("<b style=color:blue>Rolled " + numDice
                                        + " " + sides + "-sided dice, results: ");
                                for (int i = 0; i < numDice; i++) {
                                    int roll = (int) (Math.random() * sides) + 1;
                                    total += roll;
                                    rollResults.append(roll).append(", ");
                                }
                                rollResults.delete(rollResults.length() - 2, rollResults.length()); // Remove last ", "
                                rollResults.append("Total: ").append(total).append("</b>");
                                sendMessage(null, rollResults.toString());

                            }
                        }
                        // Invalid roll format
                        else {
                            sendMessage(null,
                                    "<b style=color:red>Invalid roll format. Usage: /roll followed by a number , or #d#</b>");
                        }
                        break;
//oha2 4/25
                    case "mute":
                        String[] splitMsg = message.split(" ");

                        String mutedClient = splitMsg[1];
                        client.mutedList.add(mutedClient);

                        // sends a message to the muted user and the client that muted them
                        Iterator<ServerThread> iter = clients.iterator();
                        while (iter.hasNext()) {
                            ServerThread c = iter.next();
                            if (c.getClientName().equals(mutedClient)
                                    || c.getClientName().equals(client.getClientName())) {
                                c.sendMessage(client.getClientId(), " <i>muted " + mutedClient + "</i>");
                            }
                        }
                        sendMessage(client, "<i><b>muted " + mutedClient + "</b></i>");

                        break;
                    case "unmute":

                        String[] splitArr = message.split(" ");
                        String unmutedClient = splitArr[1];
                        for (String name : client.mutedList) {
                            if (name.equals(unmutedClient)) {
                                client.mutedList.remove(unmutedClient);

                                // sends a message to the unmuted user and the client that unmuted them
                                Iterator<ServerThread> iter1 = clients.iterator();
                                while (iter1.hasNext()) {
                                    ServerThread c = iter1.next();
                                    if (c.getClientName().equals(unmutedClient)
                                            || c.getClientName().equals(client.getClientName())) {
                                        c.sendMessage(client.getClientId(), " <i>unmuted " + unmutedClient + "</i>");
                                    }
                                }
                                // sendMessage(client,"<i>unmuted "+unmutedClient+"</i>");

                                break;
                            }
                        }
                        break;
                    default:
                        wasCommand = false;
                        break;
                }
            } else {
                // oha2 4/22
                if (message.contains("*") || message.contains("!") || message.contains("_") || message.contains("#")) {
                    // Replace * with <b> and </b> tags for bold
                    message = message.replaceAll("\\*(.*?)\\*", "<b>$1</b>");

                    // Replace ! with <i> and </i> tags for italic
                    message = message.replaceAll("!(.*?)!", "<i>$1</i>");

                    // Replace _ with <u> and </u> tags for underline
                    message = message.replaceAll("_(.*?)_", "<u>$1</u>");

                    // the color ones
                    message = message.replaceAll("#r(.*?)r#", "<font style=color:red>$1</font>");

                    message = message.replaceAll("#b(.*?)b#", "<font style=color:blue>$1</font>");

                    message = message.replaceAll("#g(.*?)g#", "<font style=color:green>$1</font>");

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

        if (message.startsWith("@")) {
            sendPrivateMessage(sender, message);
            return;
        }
        if(sender == null){
            long from = (sender == null) ? Constants.DEFAULT_CLIENT_ID : sender.getClientId();
            Iterator<ServerThread> iter = clients.iterator();
            while (iter.hasNext()) {
                ServerThread client = iter.next();
                boolean messageSent = client.sendMessage(from, message);
        }
    }

        /// String from = (sender == null ? "Room" : sender.getClientName());
        long from = (sender == null) ? Constants.DEFAULT_CLIENT_ID : sender.getClientId();
        Iterator<ServerThread> iter = clients.iterator();
        while (iter.hasNext()) {
            ServerThread client = iter.next();
            if (!client.isMuted(sender.getClientName())) {
                boolean messageSent = client.sendMessage(sender.getClientId(), message);

                if (!messageSent) {
                    handleDisconnect(iter, client);
                }
            }
        }
    }
//oha2 4/25
    protected synchronized void sendPrivateMessage(ServerThread sender, String message) {
        if (!isRunning) {
            return;
        }
        info("Sending message private");
        
        long from = (sender == null) ? Constants.DEFAULT_CLIENT_ID : sender.getClientId();
        Iterator<ServerThread> iter = clients.iterator();
        String recipient = null;
        String[] ws = message.split(" ");
        if (client.isMuted(sender.getClientName())){
            return;
        }
        for (String w : ws) {
            if (w.startsWith("@")) {
                recipient = w.substring(1);

                while (iter.hasNext()) {
                    ServerThread c = iter.next();
                    if (c.getClientName().equals(recipient)) {
                        c.sendMessage(from, message);

                    }
                }

            }
        }
        // sender.sendMesseage(sender, message);
        /// String from = (sender == null ? "Room" : sender.getClientName());

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