/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.swingapp.tpsanalyzer;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.JFileChooser;
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
public class TpsAnalyzer extends JFrame implements MouseListener, MouseMotionListener, MouseWheelListener {
    
    static LogData logData = null;

    public static void main(String[] args) {

        try {
            logData = new LogData(new File("msptlog-2020-11-27.log"));
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
        }
        
        new TpsAnalyzer("TPS Log Analyzer").run();
    }
    
    private long pressXPos;
    JPanel chartPanel;
    XYChart chart;
    XYSeries tpsSeries, msptSeries, playerSeries, markerSeries, targetSeries;
    Triple<List<Date>, List<Float>, List<String>> events;
    long curMinTime, curMaxTime;
    
    TpsAnalyzerMenu menu;
    JFileChooser chooser;

    JTable table;
    String[][] tableData;
    String[] headers = { "Player", "Action", "Pos1", "Pos2" };

    private TpsAnalyzer(String title) {
        super(title);
    }

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
        
        events = logData.getEvents(curMinTime, curMaxTime);
        markerSeries = chart.addSeries("Events", events.left, events.right);
        markerSeries.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Scatter);

        List<Date> completeDateRange = new ArrayList<>(); completeDateRange.add(xData.get(0)); completeDateRange.add(xData.get(xData.size()-1));
        List<Integer> fifty = new ArrayList<>(); fifty.add(50); fifty.add(50);
        targetSeries = chart.addSeries("Target MSPT", completeDateRange, fifty);
        targetSeries.setMarker(SeriesMarkers.NONE);
        targetSeries.setLineStyle(new BasicStroke(0.3f));
        
        chooser = new JFileChooser(".");

        recalcTableData();
        SwingUtilities.invokeLater(() -> {
            getContentPane().setLayout(new BorderLayout());
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            chartPanel = new XChartPanel<>(chart);
            chartPanel.addMouseListener(this);
            chartPanel.addMouseMotionListener(this);
            chartPanel.addMouseWheelListener(this);
            
            getContentPane().add(chartPanel, BorderLayout.CENTER);
            
            table = new JTable(1, 4);
            
            getContentPane().add(table, BorderLayout.PAGE_END);
            table.getColumnModel().getColumn(0).setMinWidth(200);
            table.getColumnModel().getColumn(1).setMinWidth(200);
            table.getColumnModel().getColumn(2).setMinWidth(200);
            table.getColumnModel().getColumn(3).setMinWidth(200);
            
            menu = new TpsAnalyzerMenu();
            connectMenuHandlers();
            setJMenuBar(menu);
            pack();
            setVisible(true);
        });
    }
    
    private void connectMenuHandlers() {
        menu.setExitHandler(() -> System.exit(0));
        menu.setOpenHandler(() -> {
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                LogData tempData;
                try {
                    tempData = new LogData(chooser.getSelectedFile());
                } catch (IOException ex) {
                    ex.printStackTrace();
                    return;
                }                
                logData = tempData;
                curMinTime = logData.getSmallestTimestamp().getTime();
                curMaxTime = logData.getLargestTimestamp().getTime();
                realizeNewMinMax();
            }
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
    
    @Override
    public void mouseMoved(MouseEvent e) {
        double mouseX = e.getX();
//        long pos = (long) chart.getChartXFromCoordinate(e.getX());
        long now = System.currentTimeMillis();
        List<String> output = new ArrayList<>();
        for (int i=0; i<events.left.size(); i++) {
            long then = events.left.get(i).getTime();
            double eventX = chart.getScreenXFromChart(then);
            if (Math.abs(mouseX - eventX) < 10) {
                output.add(events.note.get(i));
            }
        }
        System.out.println(now + " found "+output.size()+" texts in "+(System.currentTimeMillis()-now)+" ms");
        System.out.println(String.join("\n", output));
    }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    private void realizeNewMinMax() {
        List<Date> xData = logData.getTimestamps(curMinTime, curMaxTime);
        List<Float> yMspt = logData.getMspt(curMinTime, curMaxTime);
        List<Float> yTps = logData.getTps(curMinTime, curMaxTime);
        List<Integer> players = logData.getPlayerCount(curMinTime, curMaxTime);
        events = logData.getEvents(curMinTime, curMaxTime);
        
        chart.updateXYSeries("TPS", xData, yTps, null);
        chart.updateXYSeries("MSPT", xData, yMspt, null);
        chart.updateXYSeries("Players", xData, players, null);
        chart.updateXYSeries("Events", events.left, events.right, null);
        
        List<Date> completeDateRange = new ArrayList<>(); completeDateRange.add(xData.get(0)); completeDateRange.add(xData.get(xData.size()-1));
        List<Integer> fifty = new ArrayList<>(); fifty.add(50); fifty.add(50);
        chart.updateXYSeries("Target MSPT", completeDateRange, fifty, null);
        
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
