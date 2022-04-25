package auctions;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

/**
 * Allows each agent to be independently accessing the auctions.
 */
public class AgentAuctionRunner implements Runnable{


    private Auction auction;
    private Socket agent;
    private AuctionHouse auctionHouse;
    private int agentID;


    /**
     * Creates a new AgentAuctionRunner
     * @param agent agent bidding
     * @param auction auction being bid on
     * @param auctionHouse that auction is running in
     */
    public AgentAuctionRunner(Socket agent, Auction auction,
                              AuctionHouse auctionHouse, int agentID){
        this.auction = auction;
        this.agent = agent;
        this.auctionHouse = auctionHouse;
        this.agentID = agentID;
    }

    /**
     * Accepts the agent input and outputs to the agent and bank so that
     * the agent can make bids and access the current items for sale.
     */
    @Override
    public void run() {

        while (true) {
            DataOutputStream clientOutput = null;
            DataInputStream clientInput;

            try {
                clientInput = new DataInputStream(this.agent.getInputStream());
                String input = clientInput.readUTF();
                if (input.equals(Message.GET_AUCTION_STATE.name())) {
                    clientOutput = new DataOutputStream(this.agent.getOutputStream());
                    clientOutput.writeUTF(this.auction.getAuctionState() +
                            "[*] Type the item ID and " +
                            "the value of your bid to participate.");
                    clientOutput.flush();
                }else if (input.equals(Message.EXIT_MESSAGE.name())) {
                    this.auctionHouse.removeAgent(agent, this.agentID);
                    break;
                }
                else {
                    String[] splitInput = input.split("\\s+");
                    int itemID = Integer.parseInt(splitInput[0]);
                    int bid = Integer.parseInt(splitInput[1]);
                    if (this.auctionHouse.processInput(itemID, bid, this.agent)) {
                        if (this.auctionHouse.getItemHighBidders().get(itemID) != null) {
                            auctionHouse.sendOutbid(this.auctionHouse.
                                    getItemHighBidders().get(itemID));
                        }
                        this.auctionHouse.getItemHighBidders().put(itemID, this.agent);
                    }
                }
            } catch (Exception e) {
                this.auctionHouse.removeAgent(agent, this.agentID);
                break;
            }
        }

    }
}
