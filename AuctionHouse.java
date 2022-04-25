package auctions;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Auction house class contains methods and constructor for a new auction house
 * object.
 */
public class AuctionHouse implements Runnable{

    private final Socket socket;
    private static final Map<Socket, Integer> agents = new HashMap<>();
    private DataInputStream in;
    private DataOutputStream out;
    private AuctionServer auctionServer;
    private final Auction auction;
    private final int auctionHouseID;
    private String auctionHouseHostname;
    private ConcurrentHashMap<Integer, Socket> itemHighBidders
            = new ConcurrentHashMap<>();


    /**
     * Auciton house constructor. Creates a new auction house with
     * a new auction and server for clients to connect to. Runs auction
     * on a separate thread.
     * @param socket socket of the auction house
     * @param hostName host auction house is connected to
     * @throws IOException for input output
     */
    public AuctionHouse(Socket socket, String hostName) throws IOException {
        this.auction = new Auction();
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());
        this.auctionServer = new AuctionServer(auction, this);
        this.auctionHouseHostname = hostName;
        this.out.writeUTF(ClientType.AUCTION_HOUSE.name() + " "
                + hostName
                + " " + this.auctionServer.getServer().getLocalPort());
        this.out.flush();
        this.auctionHouseID = Integer.parseInt(in.readUTF());
        System.out.println("______ AUCTION HOUSE " + this.auctionHouseID + " ______");
        System.out.println("[*] Account Made in Bank");
        Thread thread = new Thread(this);
        thread.start();

    }

    /**
     * Adds an agent to the auction.
     * @param id
     * @param agent
     */
    protected static void addAgent(int id, Socket agent){
        System.out.println("[*] Agent " + id + " added to Auction House");
        agents.put(agent, id);
    }

    /**
     * Sends outbid message to the bank.
     * @param agent that was outbid
     * @throws IOException IO
     */
    protected void sendOutbid(Socket agent) throws IOException {
        this.out.writeUTF(Message.BID_OUTBID.name() + " " + agents.get(agent));
        this.out.flush();
    }

    /**
     * Sends a list of item info to clients and then
     * processes their input.
     */
    @Override
    public void run() {
        while(true){
            try {
                checkItemTimers(itemHighBidders);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Removes an agent from the auction house.
     * @param agent to be removed
     */
    protected void removeAgent(Socket agent, int agentID){
        System.out.println("[X] Agent " + agentID + " has left the auction");
        try {
            out.writeUTF(Message.EXIT_MESSAGE.name() + " " + agentID);
            out.flush();
        }catch(IOException ex){
            ex.printStackTrace();
        }
        agents.remove(agent);
    }

    /**
     * Returns the a map of the item id and the agent with the current
     * highest bid on that item.
     * @return itemhighbidders
     */
    protected ConcurrentHashMap<Integer, Socket> getItemHighBidders(){
        return this.itemHighBidders;
    }

    /**
     * Checks the timers on each item in the auction. If a timer has reached
     * 30 seconds a message is sent to the winning agent and another message
     * is sent to the bank to transfer funds.
     * @param itemHighBidders item ids linked to agent sockets
     * @throws IOException for input output
     */
    private void checkItemTimers(ConcurrentHashMap<Integer, Socket> itemHighBidders)
            throws IOException {
        ArrayList<Integer> itemsToRemove = new ArrayList<>();
        for(Integer itemID: itemHighBidders.keySet()){
            if(this.auction.getItem(itemID).isTimeStarted() &&
                    this.auction.getItem(itemID).getElapsedTime() >= 30){
                DataOutputStream clientOutput;
                clientOutput = new DataOutputStream(this.socket.getOutputStream());
                clientOutput.writeUTF(Message.TRANSFER_FUNDS.name() + " " +
                        agents.get(itemHighBidders.get(itemID))+ " "
                        + this.auctionHouseID + " " +
                        this.auction.getItem(itemID).getCurrentBid() + " "
                        + this.auction.getItem(itemID).getName());

                clientOutput.flush();
                itemsToRemove.add(itemID);
                String newItemName = this.auction.addNewItem();
                System.out.println("[*] Bidding for " +
                        this.auction.getItem(itemID).getName() +" is over");

                System.out.println("[*] Agent " +
                        agents.get(itemHighBidders.get(itemID)) +
                        " has won " + this.auction.getItem(itemID).getName() +
                        " for " + this.auction.getItem(itemID).getCurrentBid()
                        + " bidcoin");

                System.out.println("[*] Adding " +
                        newItemName + " to the auction");

            }
        }
        for(Integer i: itemsToRemove){
            this.auction.removeItem(i);
            itemHighBidders.remove(i);
        }
    }

    /**
     * Processes the input from an agent. Input should follow format
     * item id number and then the bid i.e."1 45"
     * @param itemID item num
     * @param bid bid to check
     * @param client client bid is from
     * @return true or false
     * @throws IOException for output
     */
    protected boolean processInput(int itemID,int bid, Socket client)
            throws IOException{
        DataOutputStream clientOutput = new DataOutputStream(client.
                getOutputStream());
        Item item = this.auction.getItem(itemID);
        if(item == null){
            return false;
        }
        if(bid > item.getMinBid() && bid > item.getCurrentBid()){
            item.startTimer();
            clientOutput.writeUTF(Message.BID_ACCEPTED.name());
            clientOutput.flush();
            this.auction.getItem(itemID).setCurrentBid(bid);
            return true;
        }else{
            clientOutput.writeUTF(Message.BID_DENIED.name());
        }
        return false;
    }

    /**
     * Inner class to run server for agents to connect to.
     * Implements runnable to continuously have new sockets connecting.
     */
    private static class AuctionServer implements Runnable{
        int portNumber = 0;
        ServerSocket auctionServer;
        private Auction auction;
        private AuctionHouse auctionHouse;

        /**
         * AuctionServer constructor creates a new serverSocket and accepts
         * new sockets on a separate thread.
         * @throws IOException for IO
         */
        public AuctionServer(Auction auction, AuctionHouse auctionHouse)
                throws IOException {
            this.auctionServer = new ServerSocket(portNumber);
            this.auction = auction;
            this.auctionHouse = auctionHouse;
            Thread acceptor = new Thread(this);
            acceptor.start();
        }

        /**
         * Returns the serverSocket.
         * @return serverSocket
         */
        protected ServerSocket getServer(){
            return this.auctionServer;
        }

        /**
         * Continuously accepts new agents and adds them to the auction.
         */
        @Override
        public void run() {
            while(true){
                try {
                    Socket newAgent = this.auctionServer.accept();
                    DataInputStream input = new DataInputStream(
                            newAgent.getInputStream());
                    int agentID = Integer.parseInt(input.readUTF());
                    AuctionHouse.addAgent(agentID, newAgent);
                    DataOutputStream out = new DataOutputStream(newAgent.getOutputStream());
                    out.writeUTF("" + this.auctionHouse.auctionHouseID);
                    out.flush();
                    AgentAuctionRunner runner = new AgentAuctionRunner(newAgent,
                            this.auction, auctionHouse, agentID);
                    Thread thread = new Thread(runner);
                    thread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Starts AuctionHouse and creates a new socket based on command line args.
     * @param args command line args
     */
    public static void main(String[] args) throws IOException {
        AuctionHouse auctionHouse;
        String hostName = null;
        String bankHostName = null;
        int portNumber = 9999;

        if(args.length == 2){
            hostName = args[0];
            bankHostName = args[1];
        }else{
            System.out.println("Incorrect arguments. Try again.");
            return;
        }
        boolean validConnection = false;
        while(!validConnection){
            try{
                auctionHouse = new AuctionHouse(new Socket(bankHostName, portNumber),
                        hostName);
                validConnection = true;
            }catch (Exception ex){
                BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("[X] Something went wrong connecting to the Bank");
                System.out.println("[*] Enter the Bank's hostname");
                bankHostName = stdIn.readLine();
                System.out.println("[*] Attempting Connection Again");
            }
        }
    }
}
