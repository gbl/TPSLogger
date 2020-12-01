/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.swingapp.tpsanalyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author gbl
 */
public class LogData {
    
    private final ArrayList<LogEntry> data;
    
    public LogData(File logFile) throws IOException {
        data =  new ArrayList<>();
        int badLines = 0;

        BufferedReader reader = new BufferedReader(new FileReader(logFile));
        
        String line;
        while ((line = reader.readLine()) != null) {
            LogEntry logEntry;
            
            String[] entries  = line.split("\\s+---\\s+");
            String[] stats = entries[0].split(" +");
            
            try {
                data.add(logEntry = new LogEntry(stats[0], stats[1], stats[2], stats[3]));
            } catch (ParseException ex) {
                badLines ++;
                continue;
            }
            
            String[] playerInfo;
            for (int i=1; i<entries.length; i++) {
                playerInfo = entries[i].split(" +");
                logEntry.addPlayer(playerInfo[0], new WorldCoordinate(playerInfo[1], Integer.parseInt(playerInfo[2]), Integer.parseInt(playerInfo[3]), Integer.parseInt(playerInfo[4])));
            }
        }
    }
    
    public List<Date> getTimestamps() { return getTimestamps(0, Long.MAX_VALUE); }

    public List<Date> getTimestamps(long minTime, long maxTime) {
        List<Date> list = new ArrayList<>(data.size());
        for (LogEntry entry: data) {
            long stamp = entry.getTimestamp().getTime();
            if (stamp >= minTime && stamp <= maxTime) {
                list.add(entry.getTimestamp());
            }
        }
        return list;
    }
    
    public List<Float> getMspt() { return getMspt(0, Long.MAX_VALUE); }
    
    public List<Float> getMspt(long minTime, long maxTime) {
        List<Float> mspt = new ArrayList<>(data.size());
        for (LogEntry entry:data) {
            long stamp = entry.getTimestamp().getTime();
            if (stamp >= minTime && stamp <= maxTime) {
                mspt.add(entry.getMspt());
            }
        }
        return mspt;
    }
    
    public List<Float> getTps() { return getTps(0, Long.MAX_VALUE); } 

    public List<Float> getTps(long minTime, long maxTime) {
        List<Float> tps = new ArrayList<>(data.size());
        for (LogEntry entry:data) {
            long stamp = entry.getTimestamp().getTime();
            if (stamp >= minTime && stamp <= maxTime) {
                tps.add(entry.getTps());
            }
        }
        return tps;
    }
    
    public List<Integer> getPlayerCount() { return getPlayerCount(0, Long.MAX_VALUE); } 

    public List<Integer> getPlayerCount(long minTime, long maxTime) {
        List<Integer> tps = new ArrayList<>(data.size());
        for (LogEntry entry:data) {
            long stamp = entry.getTimestamp().getTime();
            if (stamp >= minTime && stamp <= maxTime) {
                tps.add(entry.getPlayerCount());
            }
        }
        return tps;
    }
    
    public Date getSmallestTimestamp() { return data.get(0).getTimestamp(); }
    public Date getLargestTimestamp() { return data.get(data.size()-1).getTimestamp(); }
    
    public Map<String, WorldCoordinate> getFirstPlayersAfter(long minTime) {
        int i=0;
        while (data.get(i).getTimestamp().getTime() < minTime) {
            i++;
        }
        return data.get(i).getPlayers();
    };
    
    public Map<String, WorldCoordinate> getLastPlayersBefore(long maxTime) {
        int i=data.size()-1;
        while (data.get(i).getTimestamp().getTime() > maxTime) {
            i--;
        }
        return data.get(i).getPlayers();
    };
    
    public Triple<List<Date>, List<Float>, List<String>> getEvents(long minTime, long maxTime) {
        List<Date> dates = new ArrayList<>();
        List<Float> values = new ArrayList<>();
        List<String> notes = new ArrayList<>();
        
        int i=0;
        while (i<data.size() && data.get(i).getTimestamp().getTime() < minTime) {
            i++;
        }
        if (i == data.size()) {
            return new Triple(dates, values, notes);
        }
        Map<String, WorldCoordinate> players = data.get(i).getPlayers();
        i++;

        while (i<data.size() && data.get(i).getTimestamp().getTime() <= maxTime) {
            Map<String, WorldCoordinate> nextPlayers = data.get(i).getPlayers();
            Set<String> allPlayers = new HashSet<>();
            allPlayers.addAll(players.keySet());
            allPlayers.addAll(nextPlayers.keySet());
            int yPos = players.size();
            for (String player: allPlayers) {
                if (!players.containsKey(player)) {
                    dates.add(data.get(i).getTimestamp());
                    values.add((float)yPos++);
                    notes.add("+ "+player);
                }
                else if (!nextPlayers.containsKey(player)) {
                    dates.add(data.get(i).getTimestamp());
                    values.add((float)yPos++);
                    notes.add("- "+player);
                }
                else if (players.get(player).getWorldIndex() != nextPlayers.get(player).getWorldIndex()) {
                    dates.add(data.get(i).getTimestamp());
                    values.add((float)yPos++);
                    notes.add("W "+player+" "+players.get(player).getWorld()+" -> "+nextPlayers.get(player).getWorld());
                }
            }
            players = nextPlayers;
            i++;
        }
        return new Triple(dates, values, notes);
    }
}
