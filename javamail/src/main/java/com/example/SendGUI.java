package com.example;

import javax.mail.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import com.alibaba.dashscope.aigc.generation.GenerationResult;

public class SendGUI extends JFrame {

    private JTextField toField, ccField, bccField, subjectField;
    private JTextArea messageTextArea;
    private JButton sendButton, attachButton, generateContentButton;
    private JLabel statusLabel;
    private JFileChooser fileChooser;
    private File selectedFile;

    private String smtpHost, smtpPort, from, password;
    private AIModel aiModel;

    // 构造函数，初始化SMTP服务器信息和AI模型
    public SendGUI(String smtpHost, String smtpPort, String from, String password) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.from = from;
        this.password = password;
        this.aiModel = new AIModel();

        // 设置窗口标题、大小、关闭操作和居中显示
        setTitle("发送邮件");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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

        // 使用BorderLayout布局管理器
        JPanel panel = new JPanel(new BorderLayout());
        setContentPane(panel);

        // 收件人等信息输入区
        JPanel recipientPanel = new JPanel(new GridLayout(4, 2));
        recipientPanel.add(new JLabel("收件人:"));
        toField = new JTextField("xxx@qq.com");
        recipientPanel.add(toField);

        recipientPanel.add(new JLabel("抄送:"));
        ccField = new JTextField("xxx@qq.com");
        recipientPanel.add(ccField);

        recipientPanel.add(new JLabel("密送:"));
        bccField = new JTextField("");
        recipientPanel.add(bccField);

        recipientPanel.add(new JLabel("主题:"));
        subjectField = new JTextField("JavaMail测试");
        recipientPanel.add(subjectField);

        panel.add(recipientPanel, BorderLayout.NORTH);

        // 邮件正文
        messageTextArea = new JTextArea();
        messageTextArea.setLineWrap(true);
        messageTextArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(messageTextArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // 底部：附件、发送按钮和状态标签
        JPanel bottomPanel = new JPanel(new BorderLayout());
        JPanel buttonPanel = new JPanel();
        attachButton = new JButton("附加文件");
        generateContentButton = new JButton("大模型生成");
        sendButton = new JButton("发送");
        buttonPanel.add(attachButton);
        buttonPanel.add(generateContentButton);
        buttonPanel.add(sendButton);
        bottomPanel.add(buttonPanel, BorderLayout.NORTH);

        statusLabel = new JLabel("状态：空闲");
        bottomPanel.add(statusLabel, BorderLayout.SOUTH);

        panel.add(bottomPanel, BorderLayout.SOUTH);

        // 添加事件监听器
        attachButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileChooser = new JFileChooser();
                int result = fileChooser.showOpenDialog(SendGUI.this);
                if (result == JFileChooser.APPROVE_OPTION) {
                    selectedFile = fileChooser.getSelectedFile();
                    statusLabel.setText("已选择文件：" + selectedFile.getName());
                }
            }
        });

        generateContentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showPromptInputDialog(); // 显示提示词输入对话框
            }
        });

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String to = toField.getText();
                    String cc = ccField.getText();
                    String bcc = bccField.getText();
                    String subject = subjectField.getText();
                    String text = messageTextArea.getText();

                    // 调用SendMail类发送邮件
                    SendMail.sendTextEmail(smtpHost, smtpPort, from, password, to, cc, bcc, subject, text, selectedFile);
                    statusLabel.setText("邮件发送成功！");
                } catch (MessagingException ex) {
                    statusLabel.setText("邮件发送失败！错误：" + ex.getMessage());
                }
            }
        });

        // 设置默认邮件内容
        messageTextArea.setText("请在这里输入您的邮件内容。\n\n此致,\n敬礼");
    }

    // 显示提示词输入对话框
    private void showPromptInputDialog() {
        JTextArea promptArea = new JTextArea("请帮我写一封邀请信，邀请小郭去食堂吃热干面，时间是明天上午八点。");
        promptArea.setLineWrap(true);
        promptArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(promptArea);
        scrollPane.setPreferredSize(new Dimension(300, 150));

        Object[] message = {
                "请输入提示词:", scrollPane
        };

        // 显示对话框并获取用户输入
        int option = JOptionPane.showConfirmDialog(this, message, "输入提示词", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String prompt = promptArea.getText();
            try {
                // 调用AI模型生成内容
                GenerationResult result = aiModel.generateContent(prompt);
                messageTextArea.setText(result.getOutput().getChoices().get(0).getMessage().getContent());
                statusLabel.setText("内容生成成功！");
            } catch (Exception ex) {
                statusLabel.setText(ex.getMessage());
            }
        }
    }

}