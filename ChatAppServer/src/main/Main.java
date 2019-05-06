/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.io.*;

import java.net.Socket;
import connInfo.ConnInfo;
//import java.io.DataInputStream;
//import java.io.IOException;
import java.net.ServerSocket;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author Abhilash Saseedharan
 */

class PrimaryServer implements Runnable{
    private Socket s;
    private Vector<ConnInfo> contacts;
    public PrimaryServer(Socket s, Vector contacts) {
        this.s = s;
        this.contacts = contacts;
    }
    
    @Override
    public void run() {
        InputStream regstream = null;
        try {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            regstream = s.getInputStream();
            ObjectInputStream reg = new ObjectInputStream(regstream);
            ConnInfo v = (ConnInfo)reg.readObject();
            if(contacts.contains(v)){
                System.out.println("already exist");
                contacts.remove(v);
            }
            contacts.add(v);
            System.out.println(contacts);
            
            InputStream datainstream = s.getInputStream();
            ObjectInputStream datain = new ObjectInputStream(datainstream);
            OutputStream dataoutstream = s.getOutputStream();
            ObjectOutputStream dataout = new ObjectOutputStream(dataoutstream);  
//            System.out.println("hi");
            while(true){
                String st = new String ((String)datain.readObject());
                System.out.println(st);
                switch(st){
                    case "ls":
                        dataout.writeObject(contacts.toString());
                        break;
                    case "sel":
                        String reqname = (String)datain.readObject();
                        boolean nfflag = true;
                        for( ConnInfo  ci : contacts ){
                            if(ci.getUsername().equals(reqname)){
                                System.out.println("req found");
                                dataout.writeObject(ci);
                                nfflag = false;
                            }
                        }
                        if(nfflag){
                            System.out.println("not found");
                            dataout.writeObject(new ConnInfo("invalid", "invalid", 0));
                        }
                        break;
                }
                
            }
//            
            
            
        } catch (IOException ex) {
            Logger.getLogger(PrimaryServer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(PrimaryServer.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                regstream.close();
            } catch (IOException ex) {
                Logger.getLogger(PrimaryServer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    

}
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Vector contacts = new Vector<ConnInfo>();
        try {
            // TODO code application logic here
            ServerSocket ss = new ServerSocket(1254);
            while(true){
                Socket s = ss.accept();
                Thread t = new Thread(new PrimaryServer(s, contacts));
                t.start();
//            t.wait();
            }
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }

    }
    
}
