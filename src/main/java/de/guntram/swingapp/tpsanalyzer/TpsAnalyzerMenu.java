/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.swingapp.tpsanalyzer;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

/**
 *
 * @author gbl
 */
public class TpsAnalyzerMenu extends JMenuBar implements Runnable {
    
    private JMenu fileMenu;
    private JMenuItem open, openFTP, openSFTP, exit;
    private Runnable openHandler, ftpHandler, sftpHandler, exitHandler;
    
    public TpsAnalyzerMenu() {
        
        openHandler = ftpHandler = sftpHandler = exitHandler = this;
        
        fileMenu = new JMenu("File");

        open=new JMenuItem("Open ..."); fileMenu.add(open); open.addActionListener((e) -> openHandler.run());
        openFTP = new JMenuItem("Open from FTP ..."); fileMenu.add(openFTP); openFTP.addActionListener((e) -> ftpHandler.run());
        openSFTP = new JMenuItem("Open from SFTP ..."); fileMenu.add(openSFTP); openSFTP.addActionListener((e) -> sftpHandler.run());
        fileMenu.addSeparator();
        exit = new JMenuItem("Exit"); fileMenu.add(exit); exit.addActionListener((e) -> exitHandler.run());
        
        open.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        openFTP.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
        openSFTP.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        
        add(fileMenu);
    }
    
    public void setOpenHandler(Runnable x) { openHandler = x; }
    public void setFtpHandler(Runnable x) { ftpHandler = x; }
    public void setSFtpHandler(Runnable x) { sftpHandler = x; }
    public void setExitHandler(Runnable x) { exitHandler = x; }
    
    
    @Override
    public void run() {}
}
