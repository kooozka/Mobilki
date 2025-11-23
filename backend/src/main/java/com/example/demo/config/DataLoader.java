package com.example.demo.config;

import com.example.demo.security.model.User;
import com.example.demo.security.model.UserRole;
import com.example.demo.security.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!prod")
public class DataLoader implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.users.admin.email:admin@example.com}")
    private String adminEmail;

    @Value("${app.users.admin.password:admin}")
    private String adminPassword;

    @Value("${app.users.client.email:client@example.com}")
    private  String clientEmail;

    @Value("${app.users.client.password:client}")
    private String clientPassword;

    @Value("${app.users.dispatchManager.email:manager@example.com}")
    private String dispatchManagerEmail;

    @Value("${app.users.dispatchManager.password:manager}")
    private String dispatchManagerPassword;

    @Value("${app.users.driver.email:driver@example.com}")
    private String driverEmail;

    @Value("${app.users.driver.password:driver}")
    private String driverPassword;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = User.builder()
                        .email(adminEmail)
                        .password(passwordEncoder.encode(adminPassword))
                        .role(UserRole.ADMIN)
                        .build();

                userRepository.save(admin);
                logger.info("Default admin user created: {}", adminEmail);
            } else {
                logger.debug("Admin user already exists: {}", adminEmail);
            }
            if (!userRepository.existsByEmail(clientEmail)) {
                User client = User.builder()
                        .email(clientEmail)
                        .password(passwordEncoder.encode(clientPassword))
                        .role(UserRole.CLIENT)
                        .build();

                userRepository.save(client);
                logger.info("Default client user created: {}", clientEmail);
            } else {
                logger.debug("Client user already exists: {}", clientEmail);
            }
            if (!userRepository.existsByEmail(dispatchManagerEmail)) {
                User dispatchManager = User.builder()
                        .email(dispatchManagerEmail)
                        .password(passwordEncoder.encode(dispatchManagerPassword))
                        .role(UserRole.DISPATCH_MANAGER)
                        .build();
                userRepository.save(dispatchManager);
                logger.info("Default dispatch manager user created: {}", dispatchManagerEmail);
            } else {
                logger.debug("Dispatch manager user already exists: {}", dispatchManagerEmail);
            }
            if (!userRepository.existsByEmail(driverEmail)) {
                User driver = User.builder()
                        .email(driverEmail)
                        .password(passwordEncoder.encode(driverPassword))
                        .role(UserRole.DRIVER)
                        .build();
                userRepository.save(driver);
                logger.info("Default driver user created: {}", driverEmail);
            } else {
                logger.debug("Driver user already exists: {}", driverEmail);
            }
        } catch (Exception e) {
            logger.error("Error creating default users", e);
        }
    }
}

