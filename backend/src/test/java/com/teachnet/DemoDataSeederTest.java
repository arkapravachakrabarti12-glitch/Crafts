package com.teachnet;

import static org.assertj.core.api.Assertions.assertThat;

import com.teachnet.jobs.JobOpeningRepository;
import com.teachnet.user.Role;
import com.teachnet.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "app.seed.demo-data=true",
        "spring.datasource.url=jdbc:h2:mem:seedtest;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class DemoDataSeederTest {

    @Autowired
    UserRepository users;

    @Autowired
    JobOpeningRepository jobs;

    @Test
    void seedsDemoAccountsAndJobs() {
        assertThat(users.countByRole(Role.TEACHER)).isEqualTo(6);
        assertThat(users.countByRole(Role.INSTITUTION)).isEqualTo(3);
        assertThat(users.countByRole(Role.ADMIN)).isEqualTo(1);
        assertThat(jobs.count()).isEqualTo(7);
    }
}
