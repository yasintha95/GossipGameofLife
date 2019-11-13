/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package report;

import core.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 * @author Yasintha Larasati, Sanata Dharma Univeristy
 */

  public class MessageRelayedReportPertime extends Report implements MessageListener, UpdateListener{

    
   public static final String INTERVAL_COUNT = "interval";
    public static final int DEFAULT_INTERVAL_COUNT = 3600;
    public static final String ENGINE_SETTING = "decisionEngine";
    private double lastRecord = Double.MIN_VALUE;
    private int interval;

    private Map<Double, Integer> nrofRelay;
    private int nrofRelayed;

    /**
     * Constructor.
     */
    public MessageRelayedReportPertime() {
        Settings s = getSettings();
        
        if (s.contains(INTERVAL_COUNT)) {
            interval = s.getInt(INTERVAL_COUNT);
        } else {
            interval = -1;
        }
        if (interval < 0) {
            interval = DEFAULT_INTERVAL_COUNT;
        }
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.interval = interval;
        this.nrofRelay = new HashMap<>();
        this.nrofRelayed = 0;
    }

    @Override
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
    }

    @Override
    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {

    }

    @Override
    public void messageTransferred(Message m, DTNHost from, DTNHost to, boolean firstDelivery) {
        this.nrofRelayed++;

    }

    @Override
    public void newMessage(Message m) {
    }

    @Override
    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
    }

    @Override
    public void done() {
        String statsText = "TIME\tRELAYED\n";
        for (Map.Entry<Double, Integer> entry : nrofRelay.entrySet()) {
            double key = entry.getKey();
            Integer value = entry.getValue();
            statsText += key + "\t" + value + "\n";
        }
        write(statsText);
        super.done();
    }

    @Override
    public void updated(List<DTNHost> hosts) {
        if (getSimTime() - lastRecord >= interval) {
            this.lastRecord = getSimTime() - getSimTime() % interval;
            nrofRelay.put(lastRecord, nrofRelayed);
            this.nrofRelayed = 0;
        }
    }
  }

