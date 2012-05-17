package it.cicolella.souliss;

import it.freedomotic.api.EventTemplate;
import it.freedomotic.api.Protocol;
import it.freedomotic.events.ProtocolRead;
import it.freedomotic.app.Freedomotic;
import it.freedomotic.exceptions.UnableToExecuteException;
import it.freedomotic.reactions.Command;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.JsonNode;
import java.net.ConnectException;
import java.net.URL;
import java.nio.charset.Charset;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * Plugin for Souliss Library www.souliss.net author Mauro Cicolella -
 * www.emmecilab.net For more details please refer to
 * http://www.freedomotic.com/forum/6/811
 */
public class Souliss extends Protocol {

    private static ArrayList<Board> boards = null;
    private static int BOARD_NUMBER = 1;
    private static int POLLING_TIME = 1000;
    private Socket socket = null;
    private DataOutputStream outputStream = null;
    private BufferedReader inputStream = null;
    private String[] address = null;
    private int SOCKET_TIMEOUT = configuration.getIntProperty("socket-timeout", 1000);

    /**
     * Initializations
     */
    public Souliss() {
        super("Souliss", "/it.cicolella.souliss/souliss.xml");
        setPollingWait(POLLING_TIME);
    }

    private void loadBoards() {
        if (boards == null) {
            boards = new ArrayList<Board>();
        }
        setDescription("Reading status changes from"); //empty description
        for (int i = 0; i < BOARD_NUMBER; i++) {
            String ipToQuery;
            int portToQuery;
            ipToQuery = configuration.getTuples().getStringProperty(i, "ip-to-query", "192.168.1.201");
            portToQuery = configuration.getTuples().getIntProperty(i, "port-to-query", 80);
            Board board = new Board(ipToQuery, portToQuery);
            boards.add(board);
            setDescription(getDescription() + " " + ipToQuery + ":" + portToQuery + ";");
        }
    }

    /**
     * Connection to boards
     */
    private boolean connect(String address, int port) {

        Freedomotic.logger.info("Trying to connect to Souliss node on address " + address + ':' + port);
        try {
            //TimedSocket is a non-blocking socket with timeout on exception
            socket = TimedSocket.getSocket(address, port, SOCKET_TIMEOUT);
            socket.setSoTimeout(SOCKET_TIMEOUT); //SOCKET_TIMEOUT ms of waiting on socket read/write
            BufferedOutputStream buffOut = new BufferedOutputStream(socket.getOutputStream());
            outputStream = new DataOutputStream(buffOut);
            return true;
        } catch (IOException e) {
            Freedomotic.logger.severe("Unable to connect to host " + address + " on port " + port);
            return false;
        }
    }

    private void disconnect() {
        // close streams and socket
        try {
            inputStream.close();
            outputStream.close();
            socket.close();
        } catch (Exception ex) {
            //do nothing. Best effort
        }
    }

    /**
     * Sensor side
     */
    @Override
    public void onStart() {
        super.onStart();
        POLLING_TIME = configuration.getIntProperty("polling-time", 1000);
        BOARD_NUMBER = configuration.getTuples().size();
        setPollingWait(POLLING_TIME);
        loadBoards();
    }

    @Override
    public void onStop() {
        super.onStop();
        //release resources
        boards.clear();
        boards = null;
        setPollingWait(-1); //disable polling
        //display the default description
        setDescription(configuration.getStringProperty("description", "Souliss"));
    }

    @Override
    protected void onRun() {
        for (Board node : boards) {
            evaluateDiffs(getJsonStatusFile(node), node); //parses the xml and crosscheck the data with the previous read
        }
        try {
            Thread.sleep(POLLING_TIME);
        } catch (InterruptedException ex) {
            Logger.getLogger(Souliss.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private JsonNode getJsonStatusFile(Board board) {
        //get the json stream from the socket connection
        String statusFileURL = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = null;
        statusFileURL = "http://" + board.getIpAddress() + ":"
                + Integer.toString(board.getPort()) + "/status";
        Freedomotic.logger.info("Souliss Sensor gets nodes status from file " + statusFileURL);
        try {
            // add json server http
            rootNode = mapper.readValue(readJsonFromUrl("http://dimaiofamily.no-ip.org/status"), JsonNode.class);
        } catch (IOException ex) {
            Logger.getLogger(Souliss.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            Logger.getLogger(Souliss.class.getName()).log(Level.SEVERE, null, ex);
        }
        return rootNode;
    }

    public static String readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            jsonText = jsonText.substring(29, jsonText.length() - 1);
            //jsonText = jsonText.substring(1, jsonText.length() - 1);
            //Freedomotic.logger.severe("Souliss JSON " + jsonText.length() + " " + jsonText.substring(29, jsonText.length() - 1).toString());
            JSONObject json = new JSONObject(jsonText);
            return jsonText;
        } finally {
            is.close();
        }
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    private void evaluateDiffs(JsonNode rootNode, Board board) {
        int id = 0;
        int slot = 0;
        String typical = null;
        String val = null;
        //parses json
        if (rootNode != null && board != null) {
            id = 0;
            for (JsonNode node : rootNode.path("id")) {
                String hlt = node.path("hlt").getTextValue();
                System.out.println("Hlt: " + hlt + "\n");
                slot = 0;
                for (JsonNode node2 : node.path("slot")) {
                    typical = node2.path("typ").getTextValue();
                    val = node2.path("val").getTextValue();
                    System.out.println("id:" + id + " slot" + slot + " Typ: " + typical + " Val: " + val + "\n");
                    Freedomotic.logger.severe("Souliss monitorize id: " + id + " slot: " + slot + " typ: " + typical + " val: " + val);
                    // call for notify event
                    sendChanges(board,id,slot,val,typical);
                    slot++;
                }
                id++;
            }
        }
    }

    private void sendChanges(Board board, int id, int slot, String val, String typical) { //
        //reconstruct freedomotic object address
        String address = board.getIpAddress() + ":" + board.getPort() + ":" + id + ":" + slot;
        Freedomotic.logger.info("Sending Souliss protocol read event for object address '" + address + "'");
        //building the event ProtocolRead
        ProtocolRead event = new ProtocolRead(this, "souliss", address);
        event.addProperty("souliss.typical", typical);
        event.addProperty("souliss.val", val);
        switch (Integer.parseInt(typical)) {
            case 11:
                if (val.equals("0")) {
                    event.addProperty("isOn", "false");
                } else {
                    event.addProperty("isOn", "true");
                }
                break;
        }
        //publish the event on the messaging bus
        this.notifyEvent(event);
    }

    /**
     * Actuator side
     */
    @Override
    public void onCommand(Command c) throws UnableToExecuteException {
        //get connection paramentes address:port from received freedom command
        String delimiter = configuration.getProperty("address-delimiter");
        address = c.getProperty("address").split(delimiter);
        //connect to the ethernet board
        boolean connected = false;
        try {
            connected = connect(address[0], Integer.parseInt(address[1]));
        } catch (ArrayIndexOutOfBoundsException outEx) {
            Freedomotic.logger.severe("The object address '" + c.getProperty("address") + "' is not properly formatted. Check it!");
            throw new UnableToExecuteException();
        } catch (NumberFormatException numberFormatException) {
            Freedomotic.logger.severe(address[1] + " is not a valid ethernet port to connect to");
            throw new UnableToExecuteException();
        }

        if (connected) {
            String message = createMessage(c);
            String expectedReply = c.getProperty("expected-reply");
            try {
                String reply = sendToBoard(message);
                if ((reply != null) && (!reply.equals(expectedReply))) {
                    //TODO: implement reply check
                }
            } catch (IOException iOException) {
                setDescription("Unable to send the message to host " + address[0] + " on port " + address[1]);
                Freedomotic.logger.severe("Unable to send the message to host " + address[0] + " on port " + address[1]);
                throw new UnableToExecuteException();
            } finally {
                disconnect();
            }
        } else {
            throw new UnableToExecuteException();
        }
    }

    private String sendToBoard(String message) throws IOException {
        String receivedReply = null;
        if (outputStream != null) {
            outputStream.writeBytes(message);
            outputStream.flush();
            inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            try {
                receivedReply = inputStream.readLine(); // read device reply
            } catch (IOException iOException) {
                throw new IOException();
            }
        }
        return receivedReply;
    }

    // create message to send to the board
    // this part must be changed to relect board protocol
    public String createMessage(Command c) {
        String message = null;
        String id = null;
        String slot = null;
        String behavior = null;
        String url = null;
        Integer val = 0;

        id = address[2];
        slot = address[3];
        val = Integer.parseInt(c.getProperty("val"));

        //compose requested url
        url = "force?id=" + id + "&slot=" + slot + "&val=" + val;

        // http request sending to the board
        message = "GET /" + url + " HTTP 1.1\r\n\r\n";
        Freedomotic.logger.info("Sending 'GET /" + url + " HTTP 1.1' to Souliss board");
        return (message);
    }

    @Override
    protected boolean canExecute(Command c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void onEvent(EventTemplate event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}