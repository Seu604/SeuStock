package com.seu.seustock.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

/**
 * 개발용 기본 메일 발송기. 실제로 메일을 보내지 않고 재설정 링크를 로그로만 출력한다.
 * 운영용 SMTP 구현이 추가되면 {@code seustock.mail.type} 속성으로 교체할 수 있다.
 */
@Service
@Primary
public class LoggingPasswordResetMailSender implements PasswordResetMailSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingPasswordResetMailSender.class);

    @Override
    public void send(String toEmail, String resetUrl) {
        log.info("[비밀번호 재설정] 수신자={} 재설정 링크={}", toEmail, resetUrl);
    }
}
