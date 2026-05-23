package com.thanglong.landtax;

import com.thanglong.landtax.infrastructure.adapter.client.VneidServiceClient;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.AccountEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.entity.CitizenLocalEntity;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.AccountJpaRepository;
import com.thanglong.landtax.infrastructure.adapter.persistence.jpa.CitizenLocalJpaRepository;
import com.thanglong.landtax.usecase.dto.ApiResponse;
import com.thanglong.landtax.usecase.dto.ProfileResponse;
import com.thanglong.landtax.usecase.dto.VneidAuthResponse;
import com.thanglong.landtax.usecase.service.AuthService;
import com.thanglong.landtax.usecase.service.ProfileService;
import com.thanglong.landtax.infrastructure.adapter.controller.exception.AccountInactiveException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class AuthAndProfileIntegrationTest {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private AuthService authService;

    @Autowired
    private CitizenLocalJpaRepository citizenLocalJpaRepository;

    @Autowired
    private AccountJpaRepository accountJpaRepository;

    @MockBean
    private VneidServiceClient vneidServiceClient;

    @Test
    public void testGetProfileMeAndGetProfile() {
        List<CitizenLocalEntity> citizens = citizenLocalJpaRepository.findAll();
        if (citizens.isEmpty()) {
            return;
        }
        CitizenLocalEntity citizen = citizens.get(0);
        String cccd = citizen.getCccdNumber();

        ProfileResponse response = profileService.getProfileMe(cccd);
        assertNotNull(response);
        assertEquals(citizen.getFullName(), response.getFullName());
        assertEquals(citizen.getEmail(), response.getEmail());
        assertNotNull(response.getRoles());
        assertFalse(response.getRoles().isEmpty());
        assertNotNull(response.getActiveRole());

        ProfileResponse response2 = profileService.getProfile(cccd);
        assertNotNull(response2);
        assertEquals(response.getFullName(), response2.getFullName());
        assertEquals(response.getActiveRole(), response2.getActiveRole());
    }

    @Test
    public void testQrLoginWithInactiveAccount() {
        List<CitizenLocalEntity> citizens = citizenLocalJpaRepository.findAll();
        if (citizens.isEmpty()) {
            return;
        }
        CitizenLocalEntity citizen = citizens.get(0);
        AccountEntity account = accountJpaRepository.findByCitizenId(citizen.getCitizenId()).orElse(null);
        if (account == null) {
            return;
        }

        // Set account status to INACTIVE
        account.setAccountStatus("INACTIVE");
        accountJpaRepository.saveAndFlush(account);

        // Mock VNeID Client response
        VneidAuthResponse mockData = VneidAuthResponse.builder()
                .userId(citizen.getCccdNumber())
                .build();
        ApiResponse<VneidAuthResponse> mockResponse = ApiResponse.<VneidAuthResponse>builder()
                .success(true)
                .data(mockData)
                .build();

        Mockito.when(vneidServiceClient.loginByQr(Mockito.any())).thenReturn(mockResponse);

        // Mock VNeID Client response for getCitizenByCccd
        com.thanglong.landtax.usecase.dto.CitizenIdentityDTO citizenData = com.thanglong.landtax.usecase.dto.CitizenIdentityDTO.builder()
                .cccdNumber(citizen.getCccdNumber())
                .fullName(citizen.getFullName())
                .email(citizen.getEmail())
                .phoneNumber(citizen.getPhoneNumber())
                .build();
        ApiResponse<com.thanglong.landtax.usecase.dto.CitizenIdentityDTO> citizenResponse = ApiResponse.<com.thanglong.landtax.usecase.dto.CitizenIdentityDTO>builder()
                .success(true)
                .data(citizenData)
                .build();
        Mockito.when(vneidServiceClient.getCitizenByCccd(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(citizenResponse);

        // Expect AccountInactiveException to be thrown
        assertThrows(AccountInactiveException.class, () -> {
            authService.qrLogin("test-qr-token");
        });
    }

    @Test
    public void testProfileMeInconsistentRoleFallback() {
        // Set up a mock Authentication with JwtUserDetails having activeRole = "ROLE_CITIZEN"
        com.thanglong.landtax.infrastructure.security.JwtFilter.JwtUserDetails mockDetails = 
                new com.thanglong.landtax.infrastructure.security.JwtFilter.JwtUserDetails(
                        "test-cccd", "test-email", "ROLE_CITIZEN", java.util.List.of("ROLE_TAX_OFFICER"), 1L
                );
        org.springframework.security.core.Authentication mockAuth = Mockito.mock(org.springframework.security.core.Authentication.class);
        Mockito.when(mockAuth.getDetails()).thenReturn(mockDetails);
        
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(mockAuth);
        
        try {
            List<CitizenLocalEntity> citizens = citizenLocalJpaRepository.findAll();
            if (citizens.isEmpty()) {
                return;
            }
            CitizenLocalEntity citizen = citizens.get(0);
            String cccd = citizen.getCccdNumber();

            ProfileResponse response = profileService.getProfileMe(cccd);
            
            // activeRole must always be set to the most privileged role from roles (Tax/Land Officer > Citizen)
            if (response.getRoles().contains("ROLE_TAX_OFFICER")) {
                assertEquals("ROLE_TAX_OFFICER", response.getActiveRole());
            } else if (response.getRoles().contains("ROLE_LAND_OFFICER")) {
                assertEquals("ROLE_LAND_OFFICER", response.getActiveRole());
            } else {
                assertEquals("ROLE_CITIZEN", response.getActiveRole());
            }
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
