package com.teachnet.seed;

import com.teachnet.common.Strings;
import com.teachnet.config.AppProperties;
import com.teachnet.user.Role;
import com.teachnet.user.User;
import com.teachnet.user.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates the admin account from ADMIN_EMAIL / ADMIN_PASSWORD on startup if it does not exist yet. */
@Component
@Order(1)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AppProperties props;
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrap(AppProperties props, UserRepository users, PasswordEncoder passwordEncoder) {
        this.props = props;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String email = props.admin() == null ? null : Strings.blankToNull(props.admin().email());
        String password = props.admin() == null ? null : Strings.blankToNull(props.admin().password());
        if (email == null || password == null) {
            return;
        }
        email = email.toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            return;
        }
        users.save(new User(email, passwordEncoder.encode(password), "TeachNet Admin", Role.ADMIN));
        log.info("Created admin account {}", email);
    }
}
