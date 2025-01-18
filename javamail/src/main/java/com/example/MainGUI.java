package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainGUI extends JFrame {

    private String smtpHost, smtpPort, pop3Host, pop3Port, from, password;

    // 构造函数，初始化SMTP和POP3服务器信息
    public MainGUI(String smtpHost, String smtpPort, String pop3Host, String pop3Port, String from, String password) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.pop3Host = pop3Host;
        this.pop3Port = pop3Port;
        this.from = from;
        this.password = password;

        // 设置窗口标题、大小、关闭操作和居中显示
        setTitle("邮件客户端");
        setSize(400, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 设置全局字体为微软雅黑，字号为14
        Font font = new Font("微软雅黑", Font.PLAIN, 14);
        UIManager.put("TextField.font", font);
        UIManager.put("PasswordField.font", font);
        UIManager.put("TextArea.font", font);
        UIManager.put("ComboBox.font", font);
        UIManager.put("List.font", font);
        UIManager.put("Button.font", font);
        UIManager.put("Label.font", font);
        UIManager.put("OptionPane.messageFont", font);
        UIManager.put("OptionPane.buttonFont", font);

        // 使用GridBagLayout布局管理器
        JPanel mainPanel = new JPanel(new GridBagLayout());
        setContentPane(mainPanel);

        // 设置组件之间的间距
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        // 创建“写信”按钮，并绑定点击事件
        JButton sendButton = new JButton("写信");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showSendGUI(); // 显示发送邮件界面
            }
        });

        // 创建“收件箱”按钮，并绑定点击事件
        JButton receiveButton = new JButton("收件箱");
        receiveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showReceiveGUI(); // 显示接收邮件界面
            }
        });

        // 将按钮添加到主面板
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(sendButton, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        mainPanel.add(receiveButton, gbc);
    }

    // 显示发送邮件界面
    private void showSendGUI() {
        SendGUI sendGui = new SendGUI(smtpHost, smtpPort, from, password);
        sendGui.setVisible(true);
        sendGui.toFront(); // 将窗口置于最前面
    }

    // 显示接收邮件界面
    private void showReceiveGUI() {
        ReceiveGUI receiveGui = new ReceiveGUI(pop3Host, pop3Port, from, password, smtpHost, smtpPort);
        receiveGui.setVisible(true);
        receiveGui.toFront(); // 将窗口置于最前面
        receiveGui.requestFocus(); // 请求焦点
    }

    // 主方法，启动登录界面
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginGUI loginGui = new LoginGUI();
            loginGui.setVisible(true);
        });
    }
}