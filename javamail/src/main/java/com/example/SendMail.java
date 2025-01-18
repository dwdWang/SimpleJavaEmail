package com.example;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.util.Properties;
import java.io.File;

public class SendMail {

	// 发送文本邮件的方法
	public static void sendTextEmail(String smtpHost, String smtpPort, String from, String password,
									 String to, String cc, String bcc, String subject, String text, File attachment)
			throws MessagingException {
		// 设置邮件服务器属性
		Properties properties = new Properties();
		properties.put("mail.smtp.host", smtpHost); // SMTP服务器地址
		properties.put("mail.smtp.port", smtpPort); // SMTP服务器端口
		properties.put("mail.smtp.auth", "true"); // 启用身份验证
		properties.put("mail.smtp.starttls.enable", "true"); // 启用TLS加密

		// 创建会话，使用Authenticator进行身份验证
		Session session = Session.getInstance(properties, new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(from, password); // 提供发件人邮箱和密码
			}
		});

		// 创建邮件消息
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from)); // 设置发件人
		message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to)); // 设置收件人

		// 设置抄送人（如果cc不为空）
		if (!cc.isEmpty()) {
			message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(cc));
		}

		// 设置密送人（如果bcc不为空）
		if (!bcc.isEmpty()) {
			message.setRecipients(Message.RecipientType.BCC, InternetAddress.parse(bcc));
		}

		message.setSubject(subject); // 设置邮件主题

		// 创建多部分消息，用于包含文本和附件
		Multipart multipart = new MimeMultipart();

		// 添加文本部分
		BodyPart textPart = new MimeBodyPart();
		textPart.setText(text); // 设置邮件正文
		multipart.addBodyPart(textPart);

		// 添加附件部分（如果附件不为空）
		if (attachment != null) {
			MimeBodyPart attachmentPart = new MimeBodyPart();
			DataSource source = new FileDataSource(attachment); // 创建附件数据源
			attachmentPart.setDataHandler(new DataHandler(source)); // 设置附件数据处理器
			attachmentPart.setFileName(attachment.getName()); // 设置附件文件名
			multipart.addBodyPart(attachmentPart); // 将附件添加到多部分消息中
		}

		// 设置邮件内容为多部分消息
		message.setContent(multipart);

		// 发送邮件
		Transport.send(message);
	}
}