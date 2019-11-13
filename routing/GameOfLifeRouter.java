/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing;

import core.*;
import java.util.*;

/**
 *
 * @author Gregorius Bima, Sanata Dharma University
 */
public class GameOfLifeRouter extends ActiveRouter {

    public static final String PUBSUB_NS = "GameOfLifeRouter";
    public static final String MSG_COUNTER_PROPERTY = "counter";
    public static final String COPY_THRESHOLD = "copyThreshold";
    public static final String DELETE_THRESHOLD = "dropThreshold";

    private int copyThreshold;
    private int deleteThreshold;

    public GameOfLifeRouter(Settings s) {
        super(s);
        Settings routeSettings = new Settings(PUBSUB_NS);
        copyThreshold = routeSettings.getInt(COPY_THRESHOLD);
        deleteThreshold = routeSettings.getInt(DELETE_THRESHOLD);
    }

    public GameOfLifeRouter(GameOfLifeRouter r) {
        super(r);
        copyThreshold = r.copyThreshold;
        deleteThreshold = r.deleteThreshold;
    }

    public boolean createNewMessage(Message m) {
        super.createNewMessage(m);
        m.addProperty(MSG_COUNTER_PROPERTY, new Integer(0));
        return true;
    }

    public void changedConnection(Connection con) {
        DTNHost myHost = getHost();
        DTNHost otherHost = con.getOtherNode(myHost);
        Collection<Message> myCollection = myHost.getMessageCollection();
        GameOfLifeRouter otherRouter = (GameOfLifeRouter) otherHost.getRouter();
        if (con.isUp()) {
            for (Message message : myCollection) {
                Integer msgCounter = (Integer) message.getProperty(MSG_COUNTER_PROPERTY);
                if (otherRouter.hasMessage(message.getId())) {
                    msgCounter--;
                } else {
                    msgCounter++;
                }
                message.updateProperty(MSG_COUNTER_PROPERTY, msgCounter);
                
                Integer updatedMsgCounter = (Integer) message.getProperty(MSG_COUNTER_PROPERTY);
                System.out.println("Message "+message+" has counter = "+updatedMsgCounter);
                if (updatedMsgCounter == copyThreshold) {
                    message.updateProperty(MSG_COUNTER_PROPERTY, 0);
                    sendMessage(message.getId(), otherHost);
                }
                if (updatedMsgCounter == deleteThreshold) {
                    deleteMessage(message.getId(), true);
                } 
            }
        }
    }

    @Override
    public void update() {
        super.update();
        if (!canStartTransfer() || isTransferring()) {
            return; // nothing to transfer or is currently transferring 
        }

        // try messages that could be delivered to final recipient
        if (exchangeDeliverableMessages() != null) {
            return;
        }
    }

    @Override
    public MessageRouter replicate() {
        return new GameOfLifeRouter(this);
    }

}
