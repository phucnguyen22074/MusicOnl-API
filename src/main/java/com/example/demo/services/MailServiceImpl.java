package com.example.demo.services;

import java.io.File;
import java.util.Base64;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.google.zxing.WriterException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class MailServiceImpl implements MailService {
	@Autowired
	private JavaMailSender sender;
	

	private static final String DEFAULT_FROM = "phucnguyen220704@gmail.com";

    @Override
    public boolean send(String from, String to, String subject, String content) {
        try {
            MimeMessage mimeMessage = sender.createMimeMessage();
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom(from != null ? from : DEFAULT_FROM);
            messageHelper.setTo(to);
            messageHelper.setSubject(subject);
            messageHelper.setText(content, true);
            sender.send(mimeMessage);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean sendHtmlMail(String to, String subject, String username) {
        try {
            String htmlContent = "<!DOCTYPE html>" +
                    "<html lang='vi'>" +
                    "<head>" +
                    "<meta charset='UTF-8'>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "<style>" +
                    "  * { margin: 0; padding: 0; box-sizing: border-box; }" +
                    "  body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%); padding: 20px; }" +
                    "  .container { max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1); }" +
                    "  .header { background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%); padding: 30px 20px; text-align: center; color: white; }" +
                    "  .logo { font-size: 28px; font-weight: bold; margin-bottom: 10px; display: flex; align-items: center; justify-content: center; }" +
                    "  .logo-icon { margin-right: 10px; font-size: 32px; }" +
                    "  .header h1 { font-size: 24px; font-weight: 600; margin-top: 10px; }" +
                    "  .content { padding: 30px; color: #333333; }" +
                    "  .greeting { font-size: 20px; margin-bottom: 20px; color: #2c3e50; font-weight: 500; }" +
                    "  .message { line-height: 1.6; margin-bottom: 25px; font-size: 16px; color: #34495e; }" +
                    "  .highlight { color: #6a11cb; font-weight: 600; }" +
                    "  .button-container { text-align: center; margin: 30px 0; }" +
                    "  .login-button { display: inline-block; padding: 14px 35px; background: linear-gradient(135deg, #6a11cb 0%, #2575fc 100%); color: #ffffff !important; text-decoration: none; border-radius: 50px; font-weight: 600; font-size: 16px; box-shadow: 0 4px 15px rgba(106, 17, 203, 0.3); transition: all 0.3s ease; }" +
                    "  .login-button:hover { transform: translateY(-2px); box-shadow: 0 6px 20px rgba(106, 17, 203, 0.4); background: linear-gradient(135deg, #5a0db9 0%, #1c67e3 100%); }" +
                    "  .features { background: #f8f9fa; border-radius: 12px; padding: 20px; margin: 25px 0; }" +
                    "  .feature-title { font-size: 18px; font-weight: 600; margin-bottom: 15px; color: #2c3e50; }" +
                    "  .feature-list { list-style: none; }" +
                    "  .feature-list li { padding: 8px 0; padding-left: 30px; position: relative; }" +
                    "  .feature-list li:before { content: '✓'; position: absolute; left: 0; color: #6a11cb; font-weight: bold; }" +
                    "  .footer { background: #f1f3f6; padding: 20px; text-align: center; font-size: 14px; color: #7f8c8d; }" +
                    "  .footer-links { margin-top: 10px; }" +
                    "  .footer-links a { color: #6a11cb; text-decoration: none; margin: 0 10px; }" +
                    "  .warning { font-size: 12px; margin-top: 20px; color: #95a5a6; }" +
                    "  @media (max-width: 600px) { .container { border-radius: 0; } }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "  <div class='container'>" +
                    "    <div class='header'>" +
                    "      <div class='logo'><span class='logo-icon'>🎵</span> Music Online</div>" +
                    "      <h1>Chào mừng bạn đến với nền tảng âm nhạc</h1>" +
                    "    </div>" +
                    "    <div class='content'>" +
                    "      <p class='greeting'>Xin chào, <strong>" + username + "</strong>! 👋</p>" +
                    "      <p class='message'>Cảm ơn bạn đã đăng ký tài khoản tại <span class='highlight'>Music Online</span>. Tài khoản của bạn đã được kích hoạt thành công và bạn đã sẵn sàng khám phá thế giới âm nhạc đa dạng của chúng tôi.</p>" +
                    "      <div class='button-container'>" +
                    "        <a href='http://localhost:3000/login' class='login-button'>Bắt đầu trải nghiệm ngay</a>" +
                    "      </div>" +
                    "      <div class='features'>" +
                    "        <p class='feature-title'>Với Music Online, bạn có thể:</p>" +
                    "        <ul class='feature-list'>" +
                    "          <li>Nghe hàng triệu bài hát chất lượng cao</li>" +
                    "          <li>Tạo playlist theo sở thích cá nhân</li>" +
                    "          <li>Khám phá nghệ sĩ mới và xu hướng âm nhạc</li>" +
                    "          <li>Đồng bộ dữ liệu trên mọi thiết bị</li>" +
                    "        </ul>" +
                    "      </div>" +
                    "      <p class='message'>Nếu bạn có bất kỳ câu hỏi nào, đừng ngần ngại liên hệ với đội ngũ hỗ trợ của chúng tôi.</p>" +
                    "    </div>" +
                    "    <div class='footer'>" +
                    "      <p>&copy; 2023 Music Online. Tất cả quyền được bảo lưu.</p>" +
                    "      <div class='footer-links'>" +
                    "        <a href='#'>Trợ giúp</a> | <a href='#'>Điều khoản sử dụng</a> | <a href='#'>Chính sách bảo mật</a>" +
                    "      </div>" +
                    "      <p class='warning'>Nếu bạn không thực hiện hành động đăng ký này, vui lòng bỏ qua email này hoặc liên hệ với bộ phận hỗ trợ.</p>" +
                    "    </div>" +
                    "  </div>" +
                    "</body>" +
                    "</html>";

            return send(DEFAULT_FROM, to, subject, htmlContent);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
