/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.swingapp.tpsanalyzer;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author gbl
 */
public class LogEntry {
    
    private Date timestamp;
    private float tps;
    private float mspt;
    private Map<String, WorldCoordinate> players;
    
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd HH:mm:ss");
    
    public LogEntry(String date, String time, String tps, String mspt) throws ParseException {
        this.timestamp=sdf.parse(date + " " + time);
        this.tps = Float.parseFloat(tps);
        this.mspt = Float.parseFloat(mspt);
        this.players = new HashMap<>();
    }
    
    public void addPlayer(String name, WorldCoordinate coord) {
        this.players.put(name, coord);
    }
    
    public Date getTimestamp() { return timestamp; }
    public float getTps() { return tps; }
    public float getMspt() { return mspt; }
    public int getPlayerCount() { return players.size(); }
    public Map<String, WorldCoordinate> getPlayers() { return players; }
}
