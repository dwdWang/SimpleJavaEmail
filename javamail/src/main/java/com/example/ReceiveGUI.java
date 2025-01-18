package com.example;

import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeUtility;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import javax.mail.*;
import java.util.*;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import com.alibaba.dashscope.aigc.generation.GenerationResult;

public class ReceiveGUI extends JFrame {

    private String pop3Host, pop3Port, from, password;
    private String smtpHost, smtpPort; // SMTP服务器地址和端口
    private JTable emailTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JTextField searchField;
    private JComboBox<String> searchTypeCombo;
    private TableRowSorter<DefaultTableModel> rowSorter;
    private Map<Integer, MailData> emailContents; // 存储邮件内容的映射

    public ReceiveGUI(String pop3Host, String pop3Port, String from, String password, String smtpHost, String smtpPort) {
        this.pop3Host = pop3Host;
        this.pop3Port = pop3Port;
        this.from = from;
        this.password = password;
        this.smtpHost = smtpHost; // 初始化SMTP服务器地址
        this.smtpPort = smtpPort; // 初始化SMTP服务器端口
        this.emailContents = new HashMap<>();

        setTitle("收件箱");
        setSize(800, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);

        Font font = new Font("微软雅黑", Font.PLAIN, 14);
        setUIFont(font);

        JPanel panel = new JPanel(new BorderLayout());
        setContentPane(panel);

        initializeTable(font);
        initializeSearchPanel(panel, font);

        refreshButton = new JButton("刷新");
        refreshButton.setFont(font);
        refreshButton.addActionListener(e -> refreshEmails());
        panel.add(refreshButton, BorderLayout.SOUTH);

        refreshEmails();
    }

    // 初始化邮件表格
    private void initializeTable(Font font) {
        String[] columnNames = {"发件人", "主题", "日期"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            public boolean isCellEditable(int row, int column) {
                return false; // 表格不可编辑
            }
        };

        emailTable = new JTable(tableModel);
        rowSorter = new TableRowSorter<>(tableModel);
        emailTable.setRowSorter(rowSorter);
        emailTable.setFont(font);
        emailTable.getTableHeader().setFont(font);
        emailTable.setRowHeight(25);

        emailTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        emailTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        emailTable.getColumnModel().getColumn(2).setPreferredWidth(200);

        // 双击邮件行时显示邮件详情
        emailTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showEmailDetail(emailTable.convertRowIndexToModel(emailTable.getSelectedRow()));
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(emailTable);
        getContentPane().add(scrollPane, BorderLayout.CENTER);
    }

    // 初始化搜索面板
    private void initializeSearchPanel(JPanel panel, Font font) {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchTypeCombo = new JComboBox<>(new String[]{"发件人", "主题", "内容"});
        searchTypeCombo.setFont(font);

        searchField = new JTextField(20);
        searchField.setFont(font);

        JButton searchButton = new JButton("搜索");
        searchButton.setFont(font);
        JButton resetButton = new JButton("重置");
        resetButton.setFont(font);

        searchPanel.add(new JLabel("搜索条件："));
        searchPanel.add(searchTypeCombo);
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(resetButton);

        searchButton.addActionListener(e -> performSearch());
        resetButton.addActionListener(e -> resetSearch());

        // 实时搜索
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                performSearch();
            }
        });

        panel.add(searchPanel, BorderLayout.NORTH);
    }

    // 执行搜索操作
    private void performSearch() {
        String searchText = searchField.getText();
        if (searchText.trim().length() == 0) {
            rowSorter.setRowFilter(null); // 清空搜索条件
        } else {
            int columnIndex;
            switch (searchTypeCombo.getSelectedItem().toString()) {
                case "发件人":
                    columnIndex = 0;
                    break;
                case "主题":
                    columnIndex = 1;
                    break;
                case "内容":
                    columnIndex = -1;
                    break;
                default:
                    columnIndex = -1;
            }

            if (columnIndex >= 0) {
                rowSorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText, columnIndex)); // 正则过滤
            } else {
                rowSorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                    @Override
                    public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                        int modelRow = entry.getIdentifier();
                        MailData emailData = emailContents.get(modelRow);
                        return emailData.content.toLowerCase().contains(searchText.toLowerCase()); // 搜索邮件内容
                    }
                });
            }
        }
    }

    // 重置搜索
    private void resetSearch() {
        searchField.setText("");
        rowSorter.setRowFilter(null);
    }

    // 刷新邮件列表
    private void refreshEmails() {
        JDialog progressDialog = new JDialog(this, "请稍候...", true);
        progressDialog.setSize(200, 100);
        progressDialog.setLocationRelativeTo(this);

        JLabel progressLabel = new JLabel("正在启动...");
        progressLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        progressLabel.setHorizontalAlignment(SwingConstants.CENTER);
        progressDialog.add(progressLabel);

        // 定时器更新进度文本
        javax.swing.Timer timer = new javax.swing.Timer(500, new ActionListener() {
            private boolean toggle = true;
            @Override
            public void actionPerformed(ActionEvent e) {
                if (toggle) {
                    progressLabel.setText("邮件接收中...");
                } else {
                    progressLabel.setText("邮件接收中.....");
                }
                toggle = !toggle;
            }
        });

        // 在新线程中执行刷新逻辑
        new Thread(() -> {
            timer.start();
            Store store = null;
            Folder inbox = null;
            try {
                tableModel.setRowCount(0);
                emailContents.clear();

                store = ReceiveMail.getStore(pop3Host, pop3Port, from, password);
                inbox = store.getFolder("INBOX");
                inbox.open(Folder.READ_WRITE);  // 使用读写模式打开
                Message[] messages = inbox.getMessages();

                int rowIndex = 0;
                for (Message message : messages) {
                    if (!message.isSet(Flags.Flag.DELETED)) {
                        String subject = message.getSubject();
                        String from = ((InternetAddress) message.getFrom()[0]).getAddress();
                        String sentDate = formatDate(message.getSentDate());
                        String content = ReceiveMail.getTextFromMessage(message);
                        List<AttachmentInfo> attachments = ReceiveMail.getAttachments(message);

                        MailData mailData = new MailData(content, attachments, subject);
                        final int currentRow = rowIndex;
                        SwingUtilities.invokeLater(() -> {
                            tableModel.addRow(new Object[]{from, subject, sentDate});
                            emailContents.put(currentRow, mailData);
                        });
                        rowIndex++;
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "刷新邮件失败: " + e.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                try {
                    if (inbox != null && inbox.isOpen()) {
                        inbox.close(false);
                    }
                    if (store != null) {
                        store.close();
                    }
                } catch (MessagingException ex) {
                    ex.printStackTrace();
                }
                timer.stop();
                SwingUtilities.invokeLater(() -> progressDialog.dispose());
            }
        }).start();

        progressDialog.setVisible(true);
    }

    // 格式化日期
    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        return sdf.format(date);
    }

    // 显示邮件详情
    private void showEmailDetail(int rowIndex) {
        MailData mailData = emailContents.get(rowIndex);
        if (mailData != null) {
            JFrame detailFrame = new JFrame("邮件详情");
            detailFrame.setSize(600, 400);
            detailFrame.setLocationRelativeTo(this);
            detailFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

            JPanel panel = new JPanel(new BorderLayout());
            detailFrame.setContentPane(panel);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

            // 删除邮件按钮
            JButton deleteButton = new JButton("删除邮件");
            deleteButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            deleteButton.addActionListener(e -> {
                try {
                    Store store = ReceiveMail.getStore(pop3Host, pop3Port, from, password);
                    Folder folder = store.getFolder("INBOX");
                    folder.open(Folder.READ_WRITE);

                    Message[] messages = folder.getMessages();
                    int messageNumber = messages.length - rowIndex;

                    try {
                        ReceiveMail.deleteEmail(store, folder, messageNumber);

                        tableModel.removeRow(rowIndex);
                        updateEmailContents(rowIndex);

                        detailFrame.dispose();
                        JOptionPane.showMessageDialog(this, "邮件已删除！",
                                "成功", JOptionPane.INFORMATION_MESSAGE);

                    } catch (MessagingException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(this,
                                "删除邮件失败: " + ex.getMessage(),
                                "错误", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        if (folder != null && folder.isOpen()) {
                            folder.close(false);
                        }
                        if (store != null) {
                            store.close();
                        }
                    }
                } catch (MessagingException ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                            "连接邮箱失败: " + ex.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            });

            // AI总结按钮
            JButton aiSummaryButton = new JButton("AI总结");
            aiSummaryButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            aiSummaryButton.addActionListener(e -> {
                try {
                    aiSummaryButton.setEnabled(false);
                    aiSummaryButton.setText("正在生成...");

                    new Thread(() -> {
                        try {
                            AIModel aiModel = new AIModel();
                            String prompt = "请总结以下邮件内容，用简洁的语言概括主要信息：\n\n" + mailData.content;
                            GenerationResult result = aiModel.generateContent(prompt);

                            SwingUtilities.invokeLater(() -> {
                                try {
                                    String summary = result.getOutput().getChoices().get(0).getMessage().getContent();

                                    JDialog summaryDialog = new JDialog(detailFrame, "AI总结结果", true);
                                    summaryDialog.setSize(400, 300);
                                    summaryDialog.setLocationRelativeTo(detailFrame);

                                    JTextArea summaryArea = new JTextArea(summary);
                                    summaryArea.setEditable(false);
                                    summaryArea.setLineWrap(true);
                                    summaryArea.setWrapStyleWord(true);
                                    summaryArea.setFont(new Font("微软雅黑", Font.PLAIN, 14));

                                    JScrollPane scrollPane = new JScrollPane(summaryArea);
                                    summaryDialog.add(scrollPane);

                                    summaryDialog.setVisible(true);
                                } finally {
                                    aiSummaryButton.setEnabled(true);
                                    aiSummaryButton.setText("AI总结");
                                }
                            });
                        } catch (Exception ex) {
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(detailFrame,
                                        "生成AI总结失败：" + ex.getMessage(),
                                        "错误",
                                        JOptionPane.ERROR_MESSAGE);
                                aiSummaryButton.setEnabled(true);
                                aiSummaryButton.setText("AI总结");
                            });
                        }
                    }).start();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(detailFrame,
                            "启动AI总结失败：" + ex.getMessage(),
                            "错误",
                            JOptionPane.ERROR_MESSAGE);
                    aiSummaryButton.setEnabled(true);
                    aiSummaryButton.setText("AI总结");
                }
            });

            // 转发邮件按钮
            JButton forwardButton = new JButton("转发");
            forwardButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            forwardButton.addActionListener(e -> {
                JDialog forwardDialog = new JDialog(detailFrame, "转发邮件", true);
                forwardDialog.setSize(400, 300);
                forwardDialog.setLocationRelativeTo(detailFrame);

                JPanel forwardPanel = new JPanel(new BorderLayout());
                forwardDialog.setContentPane(forwardPanel);

                JPanel toPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                JLabel toLabel = new JLabel("收件人:");
                JTextField toField = new JTextField(20);
                toPanel.add(toLabel);
                toPanel.add(toField);

                JTextArea forwardContentArea = new JTextArea("转发内容:\n\n" + mailData.content);
                forwardContentArea.setEditable(true);
                forwardContentArea.setLineWrap(true);
                forwardContentArea.setWrapStyleWord(true);
                JScrollPane contentScrollPane = new JScrollPane(forwardContentArea);

                JButton sendButton = new JButton("发送");
                sendButton.addActionListener(ev -> {
                    String to = toField.getText();
                    if (to.isEmpty()) {
                        JOptionPane.showMessageDialog(forwardDialog, "请输入收件人地址", "错误", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    try {
                        SendMail.sendTextEmail(smtpHost, smtpPort, from, password, to, "", "",
                                "转发: " + mailData.subject, forwardContentArea.getText(), null);
                        JOptionPane.showMessageDialog(forwardDialog, "邮件已成功转发", "成功", JOptionPane.INFORMATION_MESSAGE);
                        forwardDialog.dispose();
                    } catch (MessagingException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(forwardDialog, "转发邮件失败: " + ex.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                });

                forwardPanel.add(toPanel, BorderLayout.NORTH);
                forwardPanel.add(contentScrollPane, BorderLayout.CENTER);
                forwardPanel.add(sendButton, BorderLayout.SOUTH);

                forwardDialog.setVisible(true);
            });

            buttonPanel.add(deleteButton);
            buttonPanel.add(aiSummaryButton);
            buttonPanel.add(forwardButton);

            panel.add(buttonPanel, BorderLayout.NORTH);

            JTextArea contentTextArea = new JTextArea(mailData.content);
            contentTextArea.setEditable(false);
            contentTextArea.setLineWrap(true);
            contentTextArea.setWrapStyleWord(true);
            JScrollPane scrollPane = new JScrollPane(contentTextArea);
            panel.add(scrollPane, BorderLayout.CENTER);

            if (!mailData.attachments.isEmpty()) {
                JPanel attachmentPanel = new JPanel();
                attachmentPanel.setLayout(new BoxLayout(attachmentPanel, BoxLayout.Y_AXIS));
                for (AttachmentInfo attachment : mailData.attachments) {
                    JLabel label = new JLabel(decodeFileName(attachment.fileName));
                    JButton downloadButton = new JButton("下载");
                    downloadButton.addActionListener(e -> downloadAttachment(attachment));

                    JPanel buttonPanel2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    buttonPanel2.add(label);
                    buttonPanel2.add(downloadButton);
                    attachmentPanel.add(buttonPanel2);
                }
                panel.add(attachmentPanel, BorderLayout.SOUTH);
            }

            detailFrame.setVisible(true);
        }
    }

    // 更新邮件内容映射
    private void updateEmailContents(int deletedIndex) {
        Map<Integer, MailData> newEmailContents = new HashMap<>();
        for (Map.Entry<Integer, MailData> entry : emailContents.entrySet()) {
            if (entry.getKey() < deletedIndex) {
                newEmailContents.put(entry.getKey(), entry.getValue());
            } else if (entry.getKey() > deletedIndex) {
                newEmailContents.put(entry.getKey() - 1, entry.getValue());
            }
        }
        emailContents = newEmailContents;
    }

    // 下载附件
    private void downloadAttachment(AttachmentInfo attachment) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(decodeFileName(attachment.fileName)));
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File saveFile = fileChooser.getSelectedFile();
            try {
                Files.write(saveFile.toPath(), attachment.data);
                JOptionPane.showMessageDialog(this, "附件保存成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "附件保存失败！错误：" + e.getMessage(),
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    // 解码文件名
    private String decodeFileName(String encodedFileName) {
        try {
            return MimeUtility.decodeText(encodedFileName);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return encodedFileName;
        }
    }

    // 设置UI字体
    private void setUIFont(Font font) {
        UIManager.put("Label.font", font);
        UIManager.put("Button.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("ComboBox.font", font);
    }
}