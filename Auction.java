package auctions;
import java.util.*;

/**
 * Auction class contains methods and constructor for an auction object.
 * Auction stores and removes items and also provides user readable output.
 */
public class Auction {


    private final String[] posItemsForSale = {"Table", "Chair", "Car", "Computer",
                                     "Bed","TV","PlayStation","Washing Machine",
                                     "Watch","Piano","Camera","Xbox"};
    private final Map<Integer, Item> itemsBeingAuctioned;
    private static int itemID = 0;

    /**
     * Auction constructor creates new auction and adds items.
     */
    public Auction(){
        itemsBeingAuctioned = new HashMap<>();
        addItemsToAuction();
    }

    /**
     * Adds 3 random items to a new auction and sets the minimum bid to 10.
     */
    private void addItemsToAuction(){
        int stdAuctionSize = 3;
        int tempMinBid = 10;
        Random randIdx = new Random();
        for(int i = 0; i < stdAuctionSize; i++){
            Item item = new Item(posItemsForSale[randIdx.nextInt(
                    posItemsForSale.length - 1)], tempMinBid, itemID);
            this.itemsBeingAuctioned.put(itemID, item);
            itemID++;
        }
    }

    /**
     * Returns a string of the items in the auction for the user to read
     * and bid on.
     * @return string of auction state
     */
    protected String getAuctionState(){
        StringBuilder auctionState = new StringBuilder();
        for(Item item: itemsBeingAuctioned.values()){
            String currentItem = String.format("Item: %.15s\n", item.getName());
            String itemID = String.format("ID: %.2s\n", item.getItemID());
            String itemMinBid = String.format("Minimum Bid: %.4s\n",
                    item.getMinBid());
            String itemCurrentBid = String.format("Current Bid: %.5s\n\n",
                    item.getCurrentBid());
            auctionState.append(currentItem);
            auctionState.append(itemID);
            auctionState.append(itemMinBid);
            auctionState.append(itemCurrentBid);

        }
        return auctionState.toString();
    }

    /**
     * Returns an item based on the given id number.
     * @param itemIDNum id of item
     * @return item
     */
    protected Item getItem(int itemIDNum){
        return this.itemsBeingAuctioned.get(itemIDNum);
    }

    /**
     * Removes item with given id from the auction.
     * @param itemID id of item to be removed
     */
    protected void removeItem(Integer itemID){
        itemsBeingAuctioned.remove(itemID);
    }

    /**
     * Adds a new random item and sets the min bid to 10.
     */
    protected String addNewItem(){
        int tempMinBid = 10;
        Random randIdx = new Random();
        Item item = new Item(posItemsForSale[randIdx.nextInt(
                posItemsForSale.length - 1)], tempMinBid, itemID);
        this.itemsBeingAuctioned.put(itemID, item);
        itemID++;
        return item.getName();
    }


}
