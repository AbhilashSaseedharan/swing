
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import connInfo.ConnInfo;
import java.awt.TextArea;
import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyVetoException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLayeredPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

class Receiver implements Runnable{
    private final ConnInfo cinfo;
    private Socket socket;
    private ChatBox chatbox;
    public Receiver(ConnInfo cp, Socket socket) {
        this.cinfo = cp;
        this.socket = socket;
    }
    public Receiver(ConnInfo cp, Socket socket, ChatBox cb) {
        this.cinfo = cp;
        this.socket = socket;
        this.chatbox = cb;
    }
    @Override
    public void run() {
        InputStream datastream = null;
        try {
            //        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//            Socket s = cinfo.getSocket();
            datastream = socket.getInputStream();
            DataInputStream indata = new DataInputStream(datastream);
            while(true){
                String data = new String (indata.readUTF());
                chatbox.getTextareamap().get(cinfo.getUsername()).append("\n"+cinfo.getUsername().toUpperCase()+": "+data+"\n");
                System.out.println(data);
            }
        } catch (IOException ex) {
            Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                datastream.close();
            } catch (IOException ex) {
                Logger.getLogger(Receiver.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    
}
class Sender implements Runnable{
    private final ConnInfo conninfo;
    private Socket socket;
    private ChatBox chatbox;
//    public Sender(ConnInfo ci, Socket socket) {
//        this.conninfo = ci;
//        this.socket = socket;
//    }
    public Sender(ConnInfo ci, Socket socket, ChatBox cb) {
        this.conninfo = ci;
        this.socket = socket;
        this.chatbox = cb;
    }

    
    @Override
    public void run() {
        OutputStream datastream = null;
        try {
            //        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//            Socket s = conninfo.getSocket();
            datastream = socket.getOutputStream();
            DataOutputStream dataout = new DataOutputStream (datastream);
//            String data = "hi there";
//            Scanner sc=new Scanner(System.in);
//            System.out.println(chatbox + "jojoj");
            int sel = chatbox.getClientpipein().read();
            while(sel == 1){
                System.out.println(conninfo.getUsername() + " : ");
//                String data = sc.nextLine();
                String data = chatbox.getInputBox().getText();
                chatbox.getInputBox().setText("");
                dataout.writeUTF(data);
                chatbox.getTextareamap().get(conninfo.getUsername()).append("\n YOU: "+data+"\n");
                sel = chatbox.getClientpipein().read();
            }
        } catch (IOException ex) {
            Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                datastream.close();
            } catch (IOException ex) {
                Logger.getLogger(Sender.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

class Chatter implements Runnable{
    private ChatBox chatbox;
    private final ConnInfo partner;
    private Socket socket;
    
    public Chatter(ConnInfo partner, Socket socket, ChatBox cb) {
        this.partner = partner;
        this.socket = socket;
        this.chatbox = cb;
    }
    @Override
    public void run() {
//        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        Thread senderthread = new Thread(new Sender(partner, socket, chatbox));
        Thread recvthread = new Thread(new Receiver(partner, socket, chatbox));
        senderthread.start();
        recvthread.start();
    }
    
    
}

class Server implements Runnable{
    private int port;
    private ChatBox chatBox;

    public Server(int port, ChatBox cb) {
        this.port = port;
        this.chatBox = cb;
    }
    @Override
    public void run() {
        try {
            //        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            ServerSocket s = new ServerSocket(port);
            Socket s1=s.accept();
            InputStream inname = s1.getInputStream();
            DataInputStream inputname = new DataInputStream(inname);
            String name = new String (inputname.readUTF());
            TextArea ta;
            if(!(chatBox.getTextareamap().containsKey(name))){
                chatBox.getJchatLayeredPane().add(ta = new TextArea());
                ta.setEditable(false);
                ta.setSize(200, 400);
                chatBox.getTextareamap().put(name, ta);
                if(!(chatBox.getModel().contains(name))){
                    chatBox.getModel().add(0, name);
                    chatBox.getContactList().setModel(chatBox.getModel());
                    chatBox.getContactList().setVisible(true);
                }
            }
            else{
                ta = chatBox.getTextareamap().get(name);
            }
            ta.setVisible(false);
            Thread t = new Thread(new Chatter(new ConnInfo(name, s1), s1, chatBox));
            t.start();
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}

class Client implements Runnable{
    private ConnInfo host; 
    private Socket serversocket;
    private ChatBox chatbox;
    public Client(ConnInfo host, Socket serverSocket, ChatBox cb) {
        this.host = host;
        this.serversocket = serverSocket;
        this.chatbox = cb;
    }
    

    @Override
    public void run() {
        ObjectOutputStream dataout = null;
        try {
            //        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            OutputStream dataoutstream = serversocket.getOutputStream();
            dataout = new ObjectOutputStream(dataoutstream);
            InputStream datainstream = serversocket.getInputStream();
            ObjectInputStream datain = new ObjectInputStream(datainstream);
            while(true){
                try {
                   int sel = chatbox.getServerpipein().read();

                    switch(sel){
                        case 1:                    
                            System.err.println("ls sent");
                            dataout.writeObject(new String("ls"));
                            String st = (String)datain.readObject();
                            System.out.println(st);
                            
                            String[] conn = st.split(",");

                            
                            chatbox.setModel(new DefaultListModel<String>());
                            for(String str : conn){
                                 chatbox.getModel().addElement(str.split(":")[0].replaceFirst("\\[", "").trim());
                            }    
                            chatbox.getContactList().setModel(chatbox.getModel());     
                            chatbox.getContactList().setSelectedIndex(0);
                   
                            break;
                        case 2:
                            System.err.println("sel sent");
                            dataout.writeObject(new String("sel"));
                            String name = chatbox.getReq();
                            dataout.writeObject(name);
                            ConnInfo ci = (ConnInfo)datain.readObject();
                            System.out.println(ci);
                            
                            Socket chatsocket = new Socket(ci.getIp(),ci.getPort());
                            
                            OutputStream outname = chatsocket.getOutputStream();
                            DataOutputStream outputname = new DataOutputStream(outname);
                            outputname.writeUTF(host.getUsername());
                            
                            Thread t = new Thread(new Chatter(ci, chatsocket, chatbox) );
                            t.start();
                            break;
                        default:
                            System.err.println("INVALID");
                    }
//                    break;
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                dataout.close();
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
}

class User {
    private String name;
    private InetAddress ipAddr;
    Socket serversocket ;
    ChatBox chatbox;
    public User(String name, ChatBox cb) throws IOException {
        try {
            this.name = name;
            this.ipAddr = InetAddress.getLocalHost();
            this.chatbox = cb;
            this.chatbox.getInitBox().setClosed(true);
//            serversocket = new Socket("localhost",1254);
        } catch (UnknownHostException ex) {
            Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
        } catch (PropertyVetoException ex) {
            Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void register(){
        try {
            Thread t1 = new Thread(new Server(5007, chatbox));
            t1.start();
            serversocket = new Socket("localhost",1254);
            OutputStream sout = serversocket.getOutputStream();
            ObjectOutputStream reg = new ObjectOutputStream(sout);
            ConnInfo c = new ConnInfo(name, "localhost", 5007);
            reg.writeObject(c);
            
            
//            reg.close();
//            sout.close();
            
//            chatbox.getContactList().addKeyListener(new KeyAdapter(){
            chatbox.getContactList().addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    JList list = (JList)evt.getSource();
                    if (evt.getClickCount() == 2) {

                        try {
                            // Double-click detected
                            int index = list.locationToIndex(evt.getPoint());
                            System.out.println(list.getModel().getElementAt(index));
                            String name = (String) list.getModel().getElementAt(index);
                            if(chatbox.getCurrentchat()!=null){
                                chatbox.getTextareamap().get(chatbox.getCurrentchat()).setVisible(false);
                            }
                            chatbox.setReq(name);
                            chatbox.getServerpipeout().write(2);
                            chatbox.setCurrentchat(name);
                            TextArea ta;
//                            JPanel jp;
//                            JTextField jtf;
//                            JButton jb;
                            if(!(chatbox.getTextareamap().containsKey(name))){
                                ta = new TextArea("hello",100,50);
//                                jtf = new JTextField(75);
//                                jb = new JButton(">>");
                                
//                                jb.setLocation(550, 350);
//                                jtf.setLocation(300, 400);
                                
//                                jb.setBounds(60, 400, 220, 30);
                                chatbox.getJchatLayeredPane().add(ta);
                                ta.setEditable(false);
                                ta.setSize(200, 400);
                                chatbox.getTextareamap().put(name, ta);
                                
//                                chatbox.getJchatLayeredPane().add(jp = new JPanel());
//                                jp.add(ta);
//                                jp.add(jp);
//                                
//                                chatbox.getTextareapannelmap().put(name, jp);     
                            }
                            else{
                                ta = chatbox.getTextareamap().get(name);
//                                jp = chatbox.getTextareapannelmap().get(name);
                            }
                            ta.setVisible(true);
                        } catch (IOException ex) {
                            Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    } else if (evt.getClickCount() == 3) {

                        // Triple-click detected
                        int index = list.locationToIndex(evt.getPoint());
                    }
                }
            });
            
            
            Thread t = new Thread(new Client(c, serversocket , chatbox));
            t.start();
//            t.join();
//            serversocket.close();
        } catch (IOException ex) {
            Logger.getLogger(User.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
    }
}
/**
 *
 * @author Abhilash Saseedharan
 */
public class ChatBox extends javax.swing.JApplet {
    private Map<String, TextArea> textareamap;
    private Map<String, JPanel> textareapannelmap;
    private User user;
    private String currentchat;
    private PipedInputStream serverpipein;
    private PipedOutputStream serverpipeout;
    private PipedInputStream clientpipein;
    private PipedOutputStream clientpipeout;
    private DefaultListModel<String> model;
    private String req;
    /**
     * Initializes the applet ChatBox
     */
    @Override
    public void init() {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(ChatBox.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(ChatBox.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(ChatBox.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(ChatBox.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the applet */
        serverpipein = new PipedInputStream();
        serverpipeout = new PipedOutputStream();
        clientpipein = new PipedInputStream();
        clientpipeout = new PipedOutputStream();
        textareamap = new HashMap<>();
        textareapannelmap = new HashMap<>();
        try {
            serverpipeout.connect(serverpipein);
            clientpipeout.connect(clientpipein);
        } catch (IOException ex) {
            Logger.getLogger(ChatBox.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            java.awt.EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    initComponents();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method is called from within the init() method to initialize the
     * form. WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jLayeredPane2 = new javax.swing.JLayeredPane();
        initBox = new javax.swing.JInternalFrame();
        nameText = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        nameOkBut = new javax.swing.JButton();
        jLayeredPane1 = new javax.swing.JLayeredPane();
        contactScroll = new javax.swing.JScrollPane();
        ContactList = new javax.swing.JList();
        inputBox = new javax.swing.JTextField();
        sendButton = new javax.swing.JButton();
        jchatLayeredPane = new javax.swing.JLayeredPane();
        refreshBut = new javax.swing.JButton();

        initBox.setName("initBox"); // NOI18N
        initBox.setVisible(true);

        nameText.setText("jTextField1");
        nameText.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nameTextActionPerformed(evt);
            }
        });

        jLabel1.setText("NAME");

        nameOkBut.setText("OK");
        nameOkBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nameOkButActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout initBoxLayout = new javax.swing.GroupLayout(initBox.getContentPane());
        initBox.getContentPane().setLayout(initBoxLayout);
        initBoxLayout.setHorizontalGroup(
            initBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(initBoxLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(initBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, initBoxLayout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 72, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(nameText, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, initBoxLayout.createSequentialGroup()
                        .addComponent(nameOkBut)
                        .addGap(39, 39, 39))))
        );
        initBoxLayout.setVerticalGroup(
            initBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(initBoxLayout.createSequentialGroup()
                .addGap(13, 13, 13)
                .addGroup(initBoxLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(nameText, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(44, 44, 44)
                .addComponent(nameOkBut)
                .addContainerGap(123, Short.MAX_VALUE))
        );

        nameText.getAccessibleContext().setAccessibleName("namebox");
        nameOkBut.getAccessibleContext().setAccessibleName("ok_but_name");

        javax.swing.GroupLayout jLayeredPane2Layout = new javax.swing.GroupLayout(jLayeredPane2);
        jLayeredPane2.setLayout(jLayeredPane2Layout);
        jLayeredPane2Layout.setHorizontalGroup(
            jLayeredPane2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jLayeredPane2Layout.createSequentialGroup()
                .addContainerGap(119, Short.MAX_VALUE)
                .addComponent(initBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(80, Short.MAX_VALUE))
        );
        jLayeredPane2Layout.setVerticalGroup(
            jLayeredPane2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jLayeredPane2Layout.createSequentialGroup()
                .addContainerGap(84, Short.MAX_VALUE)
                .addComponent(initBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(150, Short.MAX_VALUE))
        );
        jLayeredPane2.setLayer(initBox, javax.swing.JLayeredPane.DEFAULT_LAYER);

        initBox.getAccessibleContext().setAccessibleName("initBox");

        contactScroll.setViewportView(ContactList);

        inputBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                inputBoxActionPerformed(evt);
            }
        });

        sendButton.setText(">>");
        sendButton.setToolTipText("");
        sendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jchatLayeredPaneLayout = new javax.swing.GroupLayout(jchatLayeredPane);
        jchatLayeredPane.setLayout(jchatLayeredPaneLayout);
        jchatLayeredPaneLayout.setHorizontalGroup(
            jchatLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 274, Short.MAX_VALUE)
        );
        jchatLayeredPaneLayout.setVerticalGroup(
            jchatLayeredPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        refreshBut.setText("REFRESH");
        refreshBut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                refreshButActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jLayeredPane1Layout = new javax.swing.GroupLayout(jLayeredPane1);
        jLayeredPane1.setLayout(jLayeredPane1Layout);
        jLayeredPane1Layout.setHorizontalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jLayeredPane1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(contactScroll, javax.swing.GroupLayout.PREFERRED_SIZE, 202, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(refreshBut))
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jLayeredPane1Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(inputBox, javax.swing.GroupLayout.PREFERRED_SIZE, 233, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(sendButton, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jLayeredPane1Layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(jchatLayeredPane, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(19, Short.MAX_VALUE))
        );
        jLayeredPane1Layout.setVerticalGroup(
            jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jLayeredPane1Layout.createSequentialGroup()
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jLayeredPane1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jchatLayeredPane))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jLayeredPane1Layout.createSequentialGroup()
                        .addComponent(contactScroll, javax.swing.GroupLayout.PREFERRED_SIZE, 428, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(refreshBut)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jLayeredPane1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(inputBox, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sendButton))
                .addGap(36, 36, 36))
        );
        jLayeredPane1.setLayer(contactScroll, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(inputBox, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(sendButton, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(jchatLayeredPane, javax.swing.JLayeredPane.DEFAULT_LAYER);
        jLayeredPane1.setLayer(refreshBut, javax.swing.JLayeredPane.DEFAULT_LAYER);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jLayeredPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(63, 63, 63))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLayeredPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap()))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLayeredPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jLayeredPane1)
                    .addContainerGap()))
        );
    }// </editor-fold>                        

    private void nameOkButActionPerformed(java.awt.event.ActionEvent evt) {                                          
        try {
            // TODO add your handling code here:
            String name = this.nameText.getText();
            name.trim();
            user = new User(name, this);
            user.register();
            
//            System.out.println(serverpipeout);
            serverpipeout.write(1);
          //  this.initBox.setClosed(true);
            //            this.remove(this.jInternalFrame1);
        } catch (IOException ex) {
            Logger.getLogger(ChatBox.class.getName()).log(Level.SEVERE, null, ex);
        } 
        //catch (PropertyVetoException ex) {
//            Logger.getLogger(ChatBox.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }                                         

    private void nameTextActionPerformed(java.awt.event.ActionEvent evt) {                                         

    }                                        

    private void inputBoxActionPerformed(java.awt.event.ActionEvent evt) {                                         
        // TODO add your handling code here:
    }                                        

    private void sendButtonActionPerformed(java.awt.event.ActionEvent evt) {                                           
        try {
            // TODO add your handling code here:
            clientpipeout.write(1);
        } catch (IOException ex) {
            Logger.getLogger(ChatBox.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }                                          

    private void refreshButActionPerformed(java.awt.event.ActionEvent evt) {                                           
        try {
            // TODO add your handling code here:
            serverpipeout.write(1);
        } catch (IOException ex) {
            Logger.getLogger(ChatBox.class.getName()).log(Level.SEVERE, null, ex);
        }
    }                                          

    public JInternalFrame getInitBox() {
        return initBox;
    }

    public JList getContactList() {
        return ContactList;
    }

    
    public PipedInputStream getServerpipein() {
        return serverpipein;
    }

    public PipedOutputStream getServerpipeout() {
        return serverpipeout;
    }

    public DefaultListModel<String> getModel() {
        return model;
    }

    public void setModel(DefaultListModel<String> model) {
        this.model = model;
    }

    public String getReq() {
        return req;
    }

    public void setReq(String req) {
        this.req = req;
    }

    public PipedInputStream getClientpipein() {
        return clientpipein;
    }

    public PipedOutputStream getClientpipeout() {
        return clientpipeout;
    }

    public JTextField getInputBox() {
        return inputBox;
    }

    public JLayeredPane getJchatLayeredPane() {
        return jchatLayeredPane;
    }

    public Map<String, TextArea> getTextareamap() {
        return textareamap;
    }

    public String getCurrentchat() {
        return currentchat;
    }

    public void setCurrentchat(String currentchat) {
        this.currentchat = currentchat;
    }

    public Map<String, JPanel> getTextareapannelmap() {
        return textareapannelmap;
    }
    
    
    // Variables declaration - do not modify                     
    private javax.swing.JList ContactList;
    private javax.swing.JScrollPane contactScroll;
    private javax.swing.JInternalFrame initBox;
    private javax.swing.JTextField inputBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLayeredPane jLayeredPane1;
    private javax.swing.JLayeredPane jLayeredPane2;
    private javax.swing.JLayeredPane jchatLayeredPane;
    private javax.swing.JButton nameOkBut;
    private javax.swing.JTextField nameText;
    private javax.swing.JButton refreshBut;
    private javax.swing.JButton sendButton;
    // End of variables declaration                   
}
