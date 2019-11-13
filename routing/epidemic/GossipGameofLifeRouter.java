/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.epidemic;

import core.*;
import java.util.*;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;

/**
 *
 * @author Yasintha Larasati, Sanata Dharma Univeristy
 */
public class GossipGameofLifeRouter implements RoutingDecisionEngine, Tombstone {

    public static final String MSG_COUNTER_PROPERTY = "counter";
    public static final String COPY_THRESHOLD = "copyThreshold";
    public static final String DELETE_THRESHOLD = "deleteThreshold";

    private final int copyThreshold;
    private final int deleteThreshold;
    /**
     * Set for Message Tombstone List
     */
    protected Set<String> tombstone;

    public GossipGameofLifeRouter(Settings s) {
        if (s.contains(COPY_THRESHOLD)) {
            this.copyThreshold = s.getInt(COPY_THRESHOLD);
        } else {
            this.copyThreshold = 3; //default threshold
        }
        if (s.contains(DELETE_THRESHOLD)) {
            this.deleteThreshold = s.getInt(DELETE_THRESHOLD);
        } else {
            this.deleteThreshold = -3; //default threshold
        }
        tombstone = new HashSet<>();
    }

    public GossipGameofLifeRouter(GossipGameofLifeRouter prototype) {
        this.copyThreshold = prototype.copyThreshold;
        this.deleteThreshold = prototype.deleteThreshold;
        tombstone = new HashSet<>();
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        GossipGameofLifeRouter de = getOtherDecisionEngineRouter(peer);
        Vector<String> messagesToDelete = new Vector<>();

        for (Message myMsg : thisHost.getMessageCollection()) {
            Integer msgCounter = (Integer) myMsg.getProperty(MSG_COUNTER_PROPERTY);
            if (peer.getRouter().hasMessage(myMsg.getId()) || de.tombstone.contains(myMsg.getId())) {
                msgCounter--;
            } else {
                msgCounter++;
            }
            myMsg.updateProperty(MSG_COUNTER_PROPERTY, msgCounter);
            Integer updatedMsgCounter = (Integer) myMsg.getProperty(MSG_COUNTER_PROPERTY);
//            System.out.println("Messge Counter " + myMsg.getId() + " = " + updatedMsgCounter);
            /**
             * Message marked for being deleted
             */
            if (updatedMsgCounter == deleteThreshold) {
                messagesToDelete.add(myMsg.getId());
                tombstone.add(myMsg.getId());
            }
        }
        DecisionEngineRouter thisRouter = (DecisionEngineRouter) thisHost.getRouter();
        /**
         * Message deleted
         */
        for (String msgId : messagesToDelete) {
            thisRouter.deleteMessage(msgId, false);
        }
        messagesToDelete.clear();
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
    }

    @Override
    public boolean newMessage(Message m) {
        m.addProperty(MSG_COUNTER_PROPERTY, 0);
        tombstone.add(m.getId());
        return true;
    }

    @Override
    public boolean isFinalDest(Message m, DTNHost aHost) {
        m.updateProperty(MSG_COUNTER_PROPERTY, 0);
        return m.getTo() == aHost;
    }

    @Override
    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        Collection<Message> messageCollection = thisHost.getMessageCollection();
        for (Message message : messageCollection) {
            if (m.getId().equals(message.getId()) && tombstone.equals(message.getId())) {
                return false;
            }
        }
        tombstone.add(m.getId());
        return true;
    }

    @Override
    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost) {
        GossipGameofLifeRouter de = getOtherDecisionEngineRouter(otherHost);

        Integer msgCounter = (Integer) m.getProperty(MSG_COUNTER_PROPERTY);

        if (msgCounter == copyThreshold) {
            m.updateProperty(MSG_COUNTER_PROPERTY, 0);
            return true;
        }
        return false;
    }

    @Override
    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        return false;
    }

    @Override
    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        return true;
    }

    @Override
    public RoutingDecisionEngine replicate() {
        return new GossipGameofLifeRouter(this);
    }

    private GossipGameofLifeRouter getOtherDecisionEngineRouter(DTNHost host) {
        MessageRouter otherRouter = host.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";
        return (GossipGameofLifeRouter) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    @Override
    public Set<String> getTombstone() {
        return tombstone;
    }
}
