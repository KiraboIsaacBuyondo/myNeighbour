import org.jgroups.*;
import org.jgroups.util.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MyNeighbour extends JFrame implements Receiver {
    private JChannel channel;
    private JTextArea groupChatArea;
    private JTextArea onlineMembersArea;
    private JTextField messageField;
    private String userName;
    private String groupName;
    private DefaultListModel<String> groupListModel;

    public MyNeighbour() throws Exception {
        setTitle("My Neighbour");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel screen1Panel = createScreen1Panel();
        JPanel screen2Panel = createScreen2Panel();

        /*contentPane is the container to which the panels are added,
        and has a CardLayout set as its layout manager.*/

        Container contentPane = getContentPane();
        contentPane.setLayout(new CardLayout());
        contentPane.add(screen1Panel, "Screen 1");
        contentPane.add(screen2Panel, "Screen 2");

        JPanel adminPanel = createAdminPanel();
        contentPane.add(adminPanel, "Admin");

        channel = new JChannel();
        channel.setReceiver(this);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                try {
                    channel.disconnect();
                    channel.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // creating screen 1 (start page)
    private JPanel createScreen1Panel() throws SQLException {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = new JLabel("My Neighbour");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));

        // displaying groups on screen1 from the database
        groupListModel = new DefaultListModel<>(); // DefaultListModel extends AbstractListModel and implements the ListModel interface.

        Database.retrieveGroupNames(); //calling myMethod method from database class
        for (String group : Database.Groups) { //Groups is a static ArrayList in Database class, hence can be accessed by just calling the class name
            groupListModel.addElement(group);
        }

        JList<String> groupList = new JList<>(groupListModel);
        JScrollPane groupListScrollPane = new JScrollPane(groupList);
        contentPanel.add(groupListScrollPane, BorderLayout.CENTER);

        JButton connectButton = new JButton("Connect");
        contentPanel.add(connectButton, BorderLayout.EAST);

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                groupName = groupList.getSelectedValue(); // user selects group here

                // Show dialog box to capture user's name
                String input = JOptionPane.showInputDialog(MyNeighbour.this, "Enter your name:");
                if (input != null && !input.isEmpty()) {
                    userName = input; // store the entered name
                    try {
                        channel.name(userName);
                        channel.connect(groupName);
                        MyNeighbour.this.userName = userName;
                        CardLayout cardLayout = (CardLayout) getContentPane().getLayout();
                        cardLayout.show(getContentPane(), "Screen 2"); // show screen 2 content



                        // Display messages from past (#Uniqueness Telegram like)
                        channel.getState(null, 10000);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                        // error message in case of connection failure
                        JOptionPane.showMessageDialog(MyNeighbour.this,
                                "Failed to connect to the group. Please try again.",
                                "Connection Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    // error message in case of no userName or group selected
                    JOptionPane.showMessageDialog(MyNeighbour.this,
                            "Please enter your name.",
                            "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JButton adminButton = new JButton("Admin");
        contentPanel.add(adminButton, BorderLayout.SOUTH);

        adminButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String password = JOptionPane.showInputDialog(MyNeighbour.this, "Enter admin password:");
                if (password != null && !password.isEmpty() && password.equals("admin123")) {
                    showAdminPage();
                } else {
                    JOptionPane.showMessageDialog(MyNeighbour.this,
                            "Invalid password. Please try again.",
                            "Authentication Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        panel.add(contentPanel, BorderLayout.CENTER);
        return panel;
    }

    // creating screen 2
    private JPanel createScreen2Panel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title section
        JLabel titleLabel = new JLabel("Group Chat");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);

        // Group chat section
        JPanel chatPanel = new JPanel(new BorderLayout());
        groupChatArea = new JTextArea();
        groupChatArea.setFont(new Font("Arial", Font.PLAIN, 14));
        groupChatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(groupChatArea);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        // Online members section
        onlineMembersArea = new JTextArea();
        onlineMembersArea.setFont(new Font("Arial", Font.PLAIN, 14));
        onlineMembersArea.setEditable(false);
        JScrollPane membersScrollPane = new JScrollPane(onlineMembersArea);
        membersScrollPane.setPreferredSize(new Dimension(150, 0));
        chatPanel.add(membersScrollPane, BorderLayout.EAST);

        // Message section
        JPanel messagePanel = new JPanel(new BorderLayout());
        messageField = new JTextField();
        messageField.setFont(new Font("Arial", Font.PLAIN, 14));
        JButton sendMessageButton = new JButton("Send");
        JButton leaveButton = new JButton("Leave");
        JButton searchButton = new JButton("Search");
        messagePanel.add(messageField, BorderLayout.CENTER);
        messagePanel.add(sendMessageButton, BorderLayout.EAST);
        messagePanel.add(leaveButton, BorderLayout.WEST);
        messagePanel.add(searchButton, BorderLayout.SOUTH);

        panel.add(chatPanel, BorderLayout.CENTER);
        panel.add(messagePanel, BorderLayout.SOUTH);

        // Configuring the send button on screen 2
        sendMessageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = messageField.getText();
                if (!message.isEmpty()) {
                    try {
                        channel.send(null, userName + ": " + message);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                    messageField.setText("");
                }
            }
        });

        // Configuring the leave button on screen 2
        leaveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    channel.disconnect();
                    channel.close();
                    CardLayout cardLayout = (CardLayout) getContentPane().getLayout();
                    cardLayout.show(getContentPane(), "Screen 1"); // show screen 1 content
                    groupName = null;
                    userName = null;
                    groupChatArea.setText("");
                    onlineMembersArea.setText("");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });



        // Configuring the search button on screen 2
        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchQuery = JOptionPane.showInputDialog(MyNeighbour.this, "Enter search query:");
                if (searchQuery != null && !searchQuery.isEmpty()) {
                    performMessageSearch(searchQuery);
                }
            }
        });

        return panel;
    }

    // Creating the admin panel
    private JPanel createAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Title section
        JLabel titleLabel = new JLabel("Admin Page");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, BorderLayout.NORTH);

        // Group management section
        JPanel groupManagementPanel = new JPanel(new BorderLayout(10, 10));

//        DefaultListModel<String> groupListModel = new DefaultListModel<>();
        JList<String> groupList = new JList<>(groupListModel);
        JScrollPane groupListScrollPane = new JScrollPane(groupList);
        groupManagementPanel.add(groupListScrollPane, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new GridLayout(1, 2, 10, 0));

        JButton createGroupButton = new JButton("Create Group");
        createGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String groupName = JOptionPane.showInputDialog(MyNeighbour.this, "Enter group name:");
                if (groupName != null && !groupName.isEmpty()) {
                    try {
                        Database.createGroup(groupName); // calling createGroup method in Database Class
                    } catch (SQLException ex) {
                        throw new RuntimeException(ex);
                    }
                    groupListModel.addElement(groupName);
                    JOptionPane.showMessageDialog(MyNeighbour.this,
                            "Group created successfully.",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(MyNeighbour.this,
                            "Invalid group name. Please try again.",
                            "Input Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        buttonsPanel.add(createGroupButton);

        JButton deleteGroupButton = new JButton("Delete Group");
        deleteGroupButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = groupList.getSelectedIndex(); //capture index of selected group
                if (selectedIndex != -1) {
                    String groupName = groupListModel.getElementAt(selectedIndex);
                    int confirm = JOptionPane.showConfirmDialog(MyNeighbour.this,
                            "Are you sure you want to delete the group '" + groupName + "'?",
                            "Confirmation", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        try {
                            Database.deleteGroup(groupName); // calling deleteGroup method in Database class
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
                        }
                        groupListModel.remove(selectedIndex);
                        JOptionPane.showMessageDialog(MyNeighbour.this,
                                "Group deleted successfully.",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(MyNeighbour.this,
                            "Please select a group to delete.",
                            "Selection Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        buttonsPanel.add(deleteGroupButton);

        groupManagementPanel.add(buttonsPanel, BorderLayout.SOUTH);

        panel.add(groupManagementPanel, BorderLayout.CENTER);

        return panel;
    }


    private void showAdminPage() {
        CardLayout cardLayout = (CardLayout) getContentPane().getLayout();
        cardLayout.show(getContentPane(), "Admin");
    }
    
    
    //message search functionality

    private void performMessageSearch(String searchQuery) { 
        String chatHistory = groupChatArea.getText();       // get all chat in chat area
        String[] messages = chatHistory.split("\n");  //break the messages into array elements by a linebreak
        StringBuilder searchResult = new StringBuilder();   // for building strings
        for (String message : messages) {
            if (message.contains(searchQuery)) {            //for each message, if it contains <searchWord>
                searchResult.append(message).append("\n");  //message + new line
            }
        }
        if (searchResult.length() > 0) {
            JOptionPane.showMessageDialog(MyNeighbour.this, "Search Results:\n" + searchResult.toString(),
                    "Message Search", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(MyNeighbour.this, "No results found for the search query.",
                    "Message Search", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    @Override
    public void receive(Message msg) {
        String message = msg.getObject().toString();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                groupChatArea.append(message + "\n");
            }
        });
    }

    @Override
    public void getState(OutputStream output) throws Exception {
        synchronized (groupChatArea) {
            Util.objectToStream(groupChatArea.getText(), new DataOutputStream(output)); //method is used to serialize the chat to the outputstream
        }
    }

    @Override
    public void setState(InputStream input) throws Exception {
        String chatHistory = (String) Util.objectFromStream(new DataInputStream(input)); ////method is used to serialize the chat to the outputstream
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                groupChatArea.setText(chatHistory);
            }
        });
    }

    @Override
    public void viewAccepted(View v) {
        List<Address> members = v.getMembers();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                onlineMembersArea.setText("Members online (" + members.size() + "):\n"); //number of members online
                for (Address member : members) {
                    onlineMembersArea.append("   \u2022 " + member.toString() + "\n"); // Adding bullet point (dot)

                }
                JOptionPane.showMessageDialog(groupChatArea, "Member joined or left the group");
            }
        });
    }

    public static void main(String[] args) throws Exception {
        MyNeighbour app = new MyNeighbour();
        app.setVisible(true);
    }
}