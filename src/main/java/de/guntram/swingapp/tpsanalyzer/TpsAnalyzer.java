/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.swingapp.tpsanalyzer;

import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.knowm.xchart.XChartPanel;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.markers.SeriesMarkers;

/**
 *
 * @author gbl
 */
public class TpsAnalyzer implements MouseListener, MouseWheelListener {
    
    static LogData logData = null;

    public static void main(String[] args) {

        try {
            logData = new LogData(new File("msptlog-2020-11-27.log"));
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        
        new TpsAnalyzer().run();
    }
    
    private long pressXPos;
    JPanel chartPanel;
    XYChart chart;
    XYSeries tpsSeries, msptSeries, playerSeries;
    long curMinTime, curMaxTime;
    
    JTable table;
    String[][] tableData;
    String[] headers = { "Player", "Action", "Pos1", "Pos2" };

    private void run() {
        
        List<Date> xData = logData.getTimestamps();
        List<Float> yMspt = logData.getMspt();
        List<Float> yTps = logData.getTps();
        List<Integer> players = logData.getPlayerCount();
        
        curMinTime = xData.get(0).getTime();
        curMaxTime = xData.get(xData.size()-1).getTime();
                
        chart = new XYChartBuilder().width(600).height(400).title("TPS and MSPT by time").xAxisTitle("x").build();
        chart.getStyler().setYAxisMax(100.0);
        tpsSeries = chart.addSeries("TPS", xData, yTps);
        tpsSeries.setMarker(SeriesMarkers.NONE);
        msptSeries = chart.addSeries("MSPT", xData, yMspt);
        msptSeries.setMarker(SeriesMarkers.NONE);
        playerSeries = chart.addSeries("Players", xData, players);
        playerSeries.setMarker(SeriesMarkers.NONE);

        recalcTableData();
        SwingUtilities.invokeLater(() -> {
            JFrame frame =  new JFrame("TPS Log analyzer");
            frame.setLayout(new BorderLayout());
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            chartPanel = new XChartPanel<>(chart);
            chartPanel.addMouseListener(this);
            chartPanel.addMouseWheelListener(this);
            
            frame.add(chartPanel, BorderLayout.CENTER);
            
            table = new JTable(1, 4);
            
            frame.add(table, BorderLayout.PAGE_END);
            table.getColumnModel().getColumn(0).setMinWidth(200);
            table.getColumnModel().getColumn(1).setMinWidth(200);
            table.getColumnModel().getColumn(2).setMinWidth(200);
            table.getColumnModel().getColumn(3).setMinWidth(200);
            
            frame.pack();
            frame.setVisible(true);
        });
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        return;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (true || e.getButton() == 0) {
            this.pressXPos = (long) chart.getChartXFromCoordinate(e.getX());
            // System.out.println("pressed at "+pressXPos);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (true || e.getButton() == 0) {
            long releaseXPos = (long) chart.getChartXFromCoordinate(e.getX());
            // System.out.println("released at "+releaseXPos);
            
            if (releaseXPos < pressXPos + 10000) {  // don't zoom in more than 10 visible seconds
                return;
            }
            
            curMinTime = pressXPos;
            curMaxTime = releaseXPos;
            realizeNewMinMax();
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        return;
    }

    @Override
    public void mouseExited(MouseEvent e) {
        return;
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        int notches = e.getWheelRotation();
        long xPos= (long) chart.getChartXFromCoordinate(e.getX());

        if (notches < 0) {  // zoom in
            curMinTime = curMinTime + (xPos - curMinTime) / 10;
            curMaxTime = curMaxTime - (curMaxTime - xPos) / 10;
        } else if (notches > 0) {
            curMinTime = curMinTime - (xPos - curMinTime) / 10;
            curMaxTime = curMaxTime + (curMaxTime - xPos) / 10;
        }
        if (curMinTime < logData.getSmallestTimestamp().getTime()) {
            curMinTime = logData.getSmallestTimestamp().getTime();
        }
        if (curMaxTime > logData.getLargestTimestamp().getTime()) {
            curMaxTime = logData.getLargestTimestamp().getTime();
        }
        realizeNewMinMax();
    }
    
    private void realizeNewMinMax() {
        List<Date> xData = logData.getTimestamps(curMinTime, curMaxTime);
        List<Float> yMspt = logData.getMspt(curMinTime, curMaxTime);
        List<Float> yTps = logData.getTps(curMinTime, curMaxTime);
        List<Integer> players = logData.getPlayerCount(curMinTime, curMaxTime);
        chart.updateXYSeries("TPS", xData, yTps, null);
        chart.updateXYSeries("MSPT", xData, yMspt, null);
        chart.updateXYSeries("Players", xData, players, null);
        chartPanel.repaint();
        
        recalcTableData();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        for (int i=0; i<tableData.length; i++) {
            model.addRow(tableData[i]);
        }
        model.fireTableDataChanged();
    }
    
    private void recalcTableData() {
        
        Map<String, WorldCoordinate> first = logData.getFirstPlayersAfter(curMinTime);
        Map<String, WorldCoordinate> last  = logData.getLastPlayersBefore(curMaxTime);
        
        Set<String> mergedPlayernames = new TreeSet<>();
        mergedPlayernames.addAll(first.keySet());
        mergedPlayernames.addAll(last.keySet());
        
        tableData = new String[mergedPlayernames.size()][];
        int i=0;
        for (String name: mergedPlayernames) {
            tableData[i]=new String[4];
            tableData[i][0] = name;
            if (!first.containsKey(name)) {
                tableData[i][1] = "logged in at";
                tableData[i][3] = last.get(name).toString();
            } else if (!last.containsKey(name)) {
                tableData[i][1] = "logged out at";
                tableData[i][2] = first.get(name).toString();
            } else {
                if (first.get(name).getWorldIndex() != last.get(name).getWorldIndex()) {
                    tableData[i][1] = "switched worlds";
                } else {
                    int xblocks = Math.abs(first.get(name).getX() - last.get(name).getX());
                    int zblocks = Math.abs(first.get(name).getZ() - last.get(name).getZ());
                    tableData[i][1] = "moved "+(xblocks+zblocks)+" blocks";
                }
                tableData[i][2] = first.get(name).toString();
                tableData[i][3] = last.get(name).toString();
            }
            ++i;
        }
    }
}
