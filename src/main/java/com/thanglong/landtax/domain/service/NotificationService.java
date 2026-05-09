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
 * Service tao thAng bao cho nguoi dan.
 *
 * <p>Tim account_id tu citizen_id (bang accounts) roi INSERT v o bang notifications.</p>
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
     * Goi thAng bao khi to khai duoc DUYoT.
     */
    public void notifyDeclarationApproved(Integer citizenId, Integer recordId) {
        String title = "To khai thua dA duoc duyet";
        String content = String.format(
                "To khai mA #%d dA duoc duyet. Vui long thanh toan so tion thua truoc han.",
                recordId);

        createNotification(citizenId, null, title, content, "TAX_APPROVED");
    }

    /**
     * Goi thAng bao khi to khai bi To CHoI.
     */
    public void notifyDeclarationRejected(Integer citizenId, Integer recordId, String reason) {
        String title = "To khai thua bi tu choi";
        String content = String.format(
                "To khai mA #%d bi tu choi do: %s. Vui long kiem tra v  nop lai.",
                recordId, reason);

        createNotification(citizenId, null, title, content, "TAX_REJECTED");
    }

    /**
     * Goi thAng bao khi thanh toan THANH CANG.
     */
    public void notifyPaymentSuccess(Integer citizenId, Integer payId,
                                      java.math.BigDecimal amount, Integer taxYear) {
        String title = "Thanh toan thua dat th nh cAng";
        String content = String.format(
                "Cam on ban dA nop thua dat nm %d. MA thanh toan #%d, " +
                        "so tion: %,.0f VN. He thong dA ghi nhan th nh cAng.",
                taxYear, payId, amount);

        createNotification(citizenId, null, title, content, "PAYMENT_SUCCESS");
    }

    /**
     * Tao thAng bao chung cho mot citizen.
     */
    public void createNotification(Integer citizenId, String cccdNumber, String title, String content, String notiType) {
        // Tim account_id tu citizen_id
        AccountEntity account = accountJpaRepository.findByCitizenId(citizenId)
                .orElseThrow(() -> {
                    log.error("Cannot send notification: no account found for citizenId={}", citizenId);
                    return new RuntimeException(
                            "KhAng tim thay t i khoan cho citizenId: " + citizenId);
                });

        // Nau cccdNumber truyon v o l  null, tho tim tu CitizenLocalEntity
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

