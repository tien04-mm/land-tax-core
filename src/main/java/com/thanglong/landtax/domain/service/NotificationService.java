package com.thanglong.landtax.domain.service;

import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AccountEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.NotificationEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.NotificationJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service tạo thông báo cho người dân.
 *
 * <p>Tìm account_id từ citizen_id (bảng accounts) rồi INSERT vào bảng notifications.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class NotificationService {

    private final NotificationJpaRepository notificationJpaRepository;
    private final AccountJpaRepository accountJpaRepository;
    private final CitizenLocalJpaRepository citizenLocalJpaRepository;

    /**
     * Gửi thông báo khi tờ khai được DUYỆT.
     */
    public void notifyDeclarationApproved(Integer citizenId, Integer recordId) {
        String title = "Tờ khai thuế đã được duyệt";
        String content = String.format(
                "Tờ khai mã #%d đã được duyệt. Vui lòng thanh toán số tiền thuế trước hạn.",
                recordId);

        createNotification(citizenId, null, title, content, "TAX_APPROVED");
    }

    /**
     * Gửi thông báo khi tờ khai bị TỪ CHỐI.
     */
    public void notifyDeclarationRejected(Integer citizenId, Integer recordId, String reason) {
        String title = "Tờ khai thuế bị từ chối";
        String content = String.format(
                "Tờ khai mã #%d bị từ chối do: %s. Vui lòng kiểm tra và nộp lại.",
                recordId, reason);

        createNotification(citizenId, null, title, content, "TAX_REJECTED");
    }

    /**
     * Gửi thông báo khi thanh toán THÀNH CÔNG.
     */
    public void notifyPaymentSuccess(Integer citizenId, Integer payId,
                                      java.math.BigDecimal amount, Integer taxYear) {
        String title = "Thanh toán thuế đất thành công";
        String content = String.format(
                "Cảm ơn bạn đã nộp thuế đất năm %d. Mã thanh toán #%d, " +
                        "số tiền: %,.0f VNĐ. Hệ thống đã ghi nhận thành công.",
                taxYear, payId, amount);

        createNotification(citizenId, null, title, content, "PAYMENT_SUCCESS");
    }

    /**
     * Tạo thông báo chung cho một citizen.
     */
    public void createNotification(Integer citizenId, String cccdNumber, String title, String content, String notiType) {
        // Tìm account_id từ citizen_id
        AccountEntity account = accountJpaRepository.findByCitizenId(citizenId)
                .orElseThrow(() -> {
                    log.error("Cannot send notification: no account found for citizenId={}", citizenId);
                    return new RuntimeException(
                            "Không tìm thấy tài khoản cho citizenId: " + citizenId);
                });

        // Nếu cccdNumber truyền vào là null, thử tìm từ CitizenLocalEntity
        String finalCccd = cccdNumber;
        if (finalCccd == null) {
            finalCccd = citizenLocalJpaRepository.findById(citizenId)
                    .map(CitizenLocalEntity::getCccdNumber)
                    .orElse(null);
        }

        NotificationEntity notification = NotificationEntity.builder()
                .accountId(account.getAccountId())
                .cccdNumber(finalCccd)
                .title(title)
                .content(content)
                .notiType(notiType)
                .isRead(false)
                .build();

        NotificationEntity saved = notificationJpaRepository.save(notification);

        log.info("Notification created: notiId={}, accountId={}, cccd={}, type={}, title='{}'",
                saved.getNotiId(), account.getAccountId(), finalCccd, notiType, title);
    }
}
