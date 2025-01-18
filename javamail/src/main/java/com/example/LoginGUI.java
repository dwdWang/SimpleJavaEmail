package com.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginGUI extends JFrame {

    private JTextField smtpHostField, smtpPortField, imapHostField, imapPortField, fromField;
    private JPasswordField passwordField;
    private JButton loginButton;

    // 构造函数，初始化登录界面
    public LoginGUI() {
        setTitle("登录");
        setSize(400, 350);
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
        JPanel panel = new JPanel(new GridBagLayout());
        setContentPane(panel);

        // 设置组件之间的间距
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // 添加SMTP主机标签和输入框
        JLabel smtpHostLabel = new JLabel("SMTP 主机:");
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(smtpHostLabel, gbc);

        smtpHostField = new JTextField("smtp.163.com");
        smtpHostField.setColumns(20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        panel.add(smtpHostField, gbc);

        // 添加SMTP端口标签和输入框
        JLabel smtpPortLabel = new JLabel("SMTP 端口:");
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(smtpPortLabel, gbc);

        smtpPortField = new JTextField("25");
        smtpPortField.setColumns(20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        panel.add(smtpPortField, gbc);

        // 添加IMAP主机标签和输入框
        JLabel imapHostLabel = new JLabel("IMAP 主机:");
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(imapHostLabel, gbc);

        imapHostField = new JTextField("imap.163.com");
        imapHostField.setColumns(20);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(imapHostField, gbc);

        // 添加IMAP端口标签和输入框
        JLabel imapPortLabel = new JLabel("IMAP 端口:");
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(imapPortLabel, gbc);

        imapPortField = new JTextField("993");
        imapPortField.setColumns(20);
        gbc.gridx = 1;
        gbc.gridy = 3;
        panel.add(imapPortField, gbc);

        // 添加邮箱标签和输入框
        JLabel fromLabel = new JLabel("邮箱:");
        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(fromLabel, gbc);

        fromField = new JTextField("xxx@163.com");
        fromField.setColumns(20);
        gbc.gridx = 1;
        gbc.gridy = 4;
        panel.add(fromField, gbc);

        // 添加密码标签和输入框
        JLabel passwordLabel = new JLabel("密码:");
        gbc.gridx = 0;
        gbc.gridy = 5;
        panel.add(passwordLabel, gbc);

        passwordField = new JPasswordField("");//可以在此替换成你的密码作为默认密码，也可以在登录时自己输入。
        passwordField.setColumns(20);
        gbc.gridx = 1;
        gbc.gridy = 5;
        panel.add(passwordField, gbc);

        // 添加登录按钮
        loginButton = new JButton("登录");
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(loginButton, gbc);

        // 绑定登录按钮的点击事件
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 获取用户输入的SMTP、IMAP服务器信息和邮箱账号密码
                String smtpHost = smtpHostField.getText();
                String smtpPort = smtpPortField.getText();
                String imapHost = imapHostField.getText();
                String imapPort = imapPortField.getText();
                String from = fromField.getText();
                String password = new String(passwordField.getPassword());

                // 打开主界面并关闭登录界面
                openMainGUI(smtpHost, smtpPort, imapHost, imapPort, from, password);
                dispose();
            }
        });
    }

    // 打开主界面
    private void openMainGUI(String smtpHost, String smtpPort, String imapHost, String imapPort,
                             String from, String password) {
        MainGUI mainGui = new MainGUI(smtpHost, smtpPort, imapHost, imapPort, from, password);
        mainGui.setVisible(true);
    }

    // 主方法，启动登录界面
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginGUI loginGui = new LoginGUI();
            loginGui.setVisible(true);
        });
    }
}