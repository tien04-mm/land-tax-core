package com.thanglong.landtax.domain.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AccountEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.NotificationEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.NotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service tao thong bao cho nguoi dan.
 *
 * <p>Tim account_id tu citizen_id (bang accounts) roi INSERT vao bang notifications.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class NotificationService {

    private final NotificationJpaRepository notificationJpaRepository;
    private final AccountJpaRepository accountJpaRepository;

    /**
     * Gui thong bao khi to khai duoc DUYET.
     */
    public void notifyDeclarationApproved(Integer citizenId, Integer recordId) {
        String title = "To khai thue da duoc duyet";
        String content = String.format(
                "To khai ma #%d da duoc duyet. Vui long thanh toan so tien thue truoc han.",
                recordId);

        createNotification(citizenId, title, content, "TAX_APPROVED");
    }

    /**
     * Gui thong bao khi to khai bi TU CHOI.
     */
    public void notifyDeclarationRejected(Integer citizenId, Integer recordId, String reason) {
        String title = "To khai thue bi tu choi";
        String content = String.format(
                "To khai ma #%d bi tu choi do: %s. Vui long kiem tra va nop lai.",
                recordId, reason);

        createNotification(citizenId, title, content, "TAX_REJECTED");
    }

    /**
     * Gui thong bao khi thanh toan THANH CONG.
     */
    public void notifyPaymentSuccess(Integer citizenId, Integer payId,
                                      java.math.BigDecimal amount, Integer taxYear) {
        String title = "Thanh toan thue dat thanh cong";
        String content = String.format(
                "Cam on ban da nop thue dat nam %d. Ma thanh toan #%d, " +
                        "so tien: %,.0f VND. He thong da ghi nhan thanh cong.",
                taxYear, payId, amount);

        createNotification(citizenId, title, content, "PAYMENT_SUCCESS");
    }

    /**
     * Tao thong bao chung cho mot citizen.
     * Tim account_id tu citizen_id qua bang accounts.
     */
    public void createNotification(Integer citizenId, String title, String content, String notiType) {
        // Tim account_id tu citizen_id
        AccountEntity account = accountJpaRepository.findByCitizenId(citizenId)
                .orElseThrow(() -> {
                    log.error("Cannot send notification: no account found for citizenId={}", citizenId);
                    return new RuntimeException(
                            "Khong tim thay tai khoan cho citizenId: " + citizenId);
                });

        NotificationEntity notification = NotificationEntity.builder()
                .accountId(account.getAccountId())
                .title(title)
                .content(content)
                .notiType(notiType)
                .isRead(false)
                .build();

        NotificationEntity saved = notificationJpaRepository.save(notification);

        log.info("Notification created: notiId={}, accountId={}, type={}, title='{}'",
                saved.getNotiId(), account.getAccountId(), notiType, title);
    }
}
