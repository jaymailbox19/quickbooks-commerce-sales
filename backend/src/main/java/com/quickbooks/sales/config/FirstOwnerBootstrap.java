package com.quickbooks.sales.config;

import com.quickbooks.sales.model.AppUser;
import com.quickbooks.sales.model.Tenant;
import com.quickbooks.sales.model.TenantMembership;
import com.quickbooks.sales.model.TenantRole;
import com.quickbooks.sales.repository.AppUserRepository;
import com.quickbooks.sales.repository.TenantMembershipRepository;
import com.quickbooks.sales.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        prefix = "app.bootstrap",
        name = "enabled",
        havingValue = "true"
)
public class FirstOwnerBootstrap implements CommandLineRunner {
    private static final Logger log =
            LoggerFactory.getLogger(FirstOwnerBootstrap.class);

    private final AppUserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final String ownerEmail;
    private final String ownerPassword;
    private final String tenantName;

    public FirstOwnerBootstrap(
            AppUserRepository userRepository,
            TenantRepository tenantRepository,
            TenantMembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.owner-email}") String ownerEmail,
            @Value("${app.bootstrap.owner-password}") String ownerPassword,
            @Value("${app.bootstrap.tenant-name}") String tenantName) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.ownerEmail = ownerEmail;
        this.ownerPassword = ownerPassword;
        this.tenantName = tenantName;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Owner bootstrap skipped because application users already exist.");
            return;
        }

        if (ownerEmail == null || ownerEmail.isBlank()
                || ownerPassword == null || ownerPassword.length() < 12) {
            throw new IllegalStateException(
                    "First-owner bootstrap requires APP_BOOTSTRAP_OWNER_EMAIL "
                            + "and an APP_BOOTSTRAP_OWNER_PASSWORD of at least 12 characters."
            );
        }

        Tenant tenant = tenantRepository.findByNameIgnoreCase(tenantName)
                .orElseThrow(() -> new IllegalStateException(
                        "Bootstrap tenant not found: " + tenantName
                                + ". Apply the tenant schema migration first."
                ));

        AppUser owner = userRepository.save(new AppUser(
                ownerEmail.trim().toLowerCase(java.util.Locale.ROOT),
                passwordEncoder.encode(ownerPassword)
        ));

        membershipRepository.save(new TenantMembership(
                owner,
                tenant,
                TenantRole.OWNER
        ));

        log.info("Created the initial OWNER account for the configured tenant.");
    }
}
