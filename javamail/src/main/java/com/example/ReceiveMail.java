package com.example;

import com.sun.mail.imap.IMAPStore;

import javax.mail.*;
import javax.mail.internet.MimeMultipart;
import javax.mail.BodyPart;
import java.io.*;
import java.util.*;

public class ReceiveMail {

    // 获取邮件存储连接
    public static Store getStore(String pop3Host, String pop3Port, String username, String password) throws MessagingException {
        // 配置IMAP协议属性
        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");
        props.put("mail.imaps.host", pop3Host);
        props.put("mail.imaps.port", pop3Port);
        props.put("mail.imaps.ssl.enable", "true");
        props.put("mail.imaps.ssl.trust", "*");

        // 创建邮件会话
        Session session = Session.getInstance(props);

        // 获取IMAPStore实例并连接
        IMAPStore store = (IMAPStore) session.getStore("imaps");
        store.connect(pop3Host, username, password);

        // 设置IMAP ID信息，用于标识客户端
        Map<String, String> id = new HashMap<>();
        id.put("name", "JavaMailClient");
        id.put("version", "1.0.0");
        id.put("vendor", "JavaMail");
        id.put("support-email", username);
        store.id(id);

        return store;
    }

    // 从邮件中提取文本内容
    public static String getTextFromMessage(Message message) throws MessagingException, IOException {
        // 根据邮件内容类型处理
        if (message.isMimeType("text/plain")) {
            return message.getContent().toString(); // 直接返回纯文本内容
        } else if (message.isMimeType("text/html")) {
            return ((String) message.getContent()).replaceAll("<[^>]*>", ""); // 去除HTML标签
        } else if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            return getTextFromMimeMultipart(mimeMultipart); // 处理多部分内容
        }
        return ""; // 默认返回空字符串
    }

    // 从MIME多部分内容中提取文本
    public static String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws MessagingException, IOException {
        StringBuilder result = new StringBuilder();
        int count = mimeMultipart.getCount();

        // 遍历每个部分
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent()); // 添加纯文本内容
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result.append(html.replaceAll("<[^>]*>", "")); // 去除HTML标签并添加
            } else if (bodyPart.getContent() instanceof MimeMultipart) {
                result.append(getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent())); // 递归处理嵌套多部分
            }
        }
        return result.toString();
    }

    // 获取邮件中的附件
    public static List<AttachmentInfo> getAttachments(Message message) throws MessagingException, IOException {
        List<AttachmentInfo> attachments = new ArrayList<>();

        // 检查邮件是否为多部分类型
        if (message.isMimeType("multipart/*")) {
            MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
            int count = mimeMultipart.getCount();

            // 遍历每个部分
            for (int i = 0; i < count; i++) {
                BodyPart bodyPart = mimeMultipart.getBodyPart(i);
                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    String fileName = bodyPart.getFileName();
                    if (fileName != null) {
                        // 读取附件数据
                        InputStream is = bodyPart.getInputStream();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            baos.write(buffer, 0, bytesRead);
                        }
                        attachments.add(new AttachmentInfo(fileName, baos.toByteArray())); // 添加到附件列表
                    }
                }
            }
        }
        return attachments;
    }

    // 删除邮件
    public static void deleteEmail(Store store, Folder folder, int messageNumber) throws MessagingException {
        try {
            Message message = folder.getMessage(messageNumber);

            // 确保文件夹以读写模式打开
            if (!folder.isOpen()) {
                folder.open(Folder.READ_WRITE);
            } else if (folder.getMode() != Folder.READ_WRITE) {
                folder.close(false);
                folder.open(Folder.READ_WRITE);
            }

            // 查找垃圾箱文件夹
            Folder trash = store.getFolder("Deleted Messages"); // 163邮箱的垃圾箱文件夹名
            if (!trash.exists()) {
                trash = store.getFolder("已删除邮件"); // 尝试中文名称
            }
            if (!trash.exists()) {
                trash = store.getFolder("Trash"); // 尝试其他可能的名称
            }

            if (trash != null && trash.exists()) {
                // 如果找到垃圾箱，将邮件移动到垃圾箱
                if (!trash.isOpen()) {
                    trash.open(Folder.READ_WRITE);
                }

                // 复制邮件到垃圾箱并标记为已删除
                folder.copyMessages(new Message[]{message}, trash);
                message.setFlag(Flags.Flag.DELETED, true);

                // 立即执行删除操作
                folder.expunge();

                // 关闭垃圾箱文件夹
                if (trash.isOpen()) {
                    trash.close(false);
                }
            } else {
                // 如果找不到垃圾箱，直接标记为删除并永久删除
                message.setFlag(Flags.Flag.DELETED, true);
                folder.expunge();
            }

            System.out.println("Email deleted successfully and moved to trash");

        } catch (MessagingException e) {
            System.err.println("Error during email deletion: " + e.getMessage());
            e.printStackTrace();
            throw new MessagingException("删除邮件失败: " + e.getMessage(), e);
        }
    }
}