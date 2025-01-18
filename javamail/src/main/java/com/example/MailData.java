// MailData.java
package com.example;

import java.util.List;

public class MailData {
    String content; // 邮件内容
    List<AttachmentInfo> attachments; // 邮件附件列表
    String subject; // 邮件主题

    // 构造函数，初始化邮件内容、附件和主题
    public MailData(String content, List<AttachmentInfo> attachments, String subject) {
        this.content = content;
        this.attachments = attachments;
        this.subject = subject;
    }
}

// AttachmentInfo类，用于存储附件信息
class AttachmentInfo {
    public String fileName; // 附件文件名
    public byte[] data; // 附件数据

    // 构造函数，初始化附件文件名和数据
    public AttachmentInfo(String fileName, byte[] data) {
        this.fileName = fileName;
        this.data = data;
    }
}