package com.teachnet.seed;

import com.teachnet.config.AppProperties;
import com.teachnet.feed.Comment;
import com.teachnet.feed.CommentRepository;
import com.teachnet.feed.Post;
import com.teachnet.feed.PostRepository;
import com.teachnet.institution.Institution;
import com.teachnet.institution.InstitutionRepository;
import com.teachnet.institution.InstitutionType;
import com.teachnet.jobs.EmploymentType;
import com.teachnet.jobs.JobOpening;
import com.teachnet.jobs.JobOpeningRepository;
import com.teachnet.network.Connection;
import com.teachnet.network.ConnectionRepository;
import com.teachnet.network.ConnectionStatus;
import com.teachnet.network.Follow;
import com.teachnet.network.FollowRepository;
import com.teachnet.profile.Board;
import com.teachnet.profile.Experience;
import com.teachnet.profile.PortfolioItem;
import com.teachnet.profile.PortfolioType;
import com.teachnet.profile.Qualification;
import com.teachnet.profile.TeacherProfile;
import com.teachnet.profile.TeacherProfileRepository;
import com.teachnet.profile.TeacherSubject;
import com.teachnet.user.Role;
import com.teachnet.user.User;
import com.teachnet.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty database with sample teachers, schools, jobs and posts so the app has something to show.
 * Enabled with SEED_DEMO_DATA=true. All demo accounts use the password {@value #DEMO_PASSWORD}.
 */
@Component
@Order(2)
public class DemoDataSeeder implements ApplicationRunner {

    public static final String DEMO_PASSWORD = "password123";

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final AppProperties props;
    private final UserRepository users;
    private final TeacherProfileRepository profiles;
    private final InstitutionRepository institutions;
    private final JobOpeningRepository jobs;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final ConnectionRepository connections;
    private final FollowRepository follows;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(AppProperties props, UserRepository users, TeacherProfileRepository profiles,
                          InstitutionRepository institutions, JobOpeningRepository jobs, PostRepository posts,
                          CommentRepository comments, ConnectionRepository connections, FollowRepository follows,
                          PasswordEncoder passwordEncoder) {
        this.props = props;
        this.users = users;
        this.profiles = profiles;
        this.institutions = institutions;
        this.jobs = jobs;
        this.posts = posts;
        this.comments = comments;
        this.connections = connections;
        this.follows = follows;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (props.seed() == null || !props.seed().demoData()) {
            return;
        }
        if (users.countByRole(Role.TEACHER) > 0 || users.countByRole(Role.INSTITUTION) > 0) {
            log.info("Demo data skipped: database already has accounts");
            return;
        }
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        TeacherProfile priya = teacher(hash, "priya@demo.teachnet.in", "Priya Sharma",
                "Physics teacher · JEE & NEET mentor", "Pune", "Maharashtra", 9, true,
                "I help Class 11–12 students fall in love with physics through experiments and problem-solving. "
                        + "Mentored 200+ JEE/NEET aspirants.");
        subject(priya, "Physics", Board.CBSE, 11, 12);
        subject(priya, "Science", Board.CBSE, 9, 10);
        qualification(priya, "M.Sc. Physics", "University of Pune", 2013, true);
        qualification(priya, "B.Ed.", "Tilak College of Education", 2014, true);
        experience(priya, "Senior Physics Teacher", "Greenfield International School", 2018, null,
                "Head of the physics department; designed the lab curriculum for Class 11–12.");
        experience(priya, "Physics Faculty", "Aakash Coaching", 2015, 2018, null);
        portfolio(priya, PortfolioType.DEMO_VIDEO, "Demo: Projectile motion in 10 minutes",
                "https://www.youtube.com/results?search_query=projectile+motion+class+11");
        portfolio(priya, PortfolioType.STUDENT_RESULTS, "2024 board results: 96% class average",
                "https://example.com/results-2024");

        TeacherProfile rahul = teacher(hash, "rahul@demo.teachnet.in", "Rahul Verma",
                "Mathematics educator · Olympiad coach", "Pune", "Maharashtra", 6, false,
                "Maths should be fun. I coach Olympiad teams and teach Class 6–10.");
        subject(rahul, "Mathematics", Board.ICSE, 6, 10);
        qualification(rahul, "B.Sc. Mathematics", "Fergusson College", 2016, false);
        qualification(rahul, "B.Ed.", "SNDT University", 2018, false);
        experience(rahul, "Maths Teacher", "St. Mary's High School", 2019, null, null);

        TeacherProfile ananya = teacher(hash, "ananya@demo.teachnet.in", "Ananya Iyer",
                "English & Literature · IB DP teacher", "Bengaluru", "Karnataka", 11, true,
                "IB English A: Language & Literature examiner. I love building reading cultures in schools.");
        subject(ananya, "English", Board.IB, 9, 12);
        subject(ananya, "English", Board.IGCSE, 9, 10);
        qualification(ananya, "M.A. English Literature", "Christ University", 2012, true);
        experience(ananya, "IB English Teacher", "Oakridge International", 2016, null, null);
        portfolio(ananya, PortfolioType.LESSON_PLAN, "Unit plan: Poetry across cultures",
                "https://example.com/poetry-unit");

        TeacherProfile arjun = teacher(hash, "arjun@demo.teachnet.in", "Arjun Mehta",
                "Computer Science teacher · Python & AI for kids", "Mumbai", "Maharashtra", 4, false,
                "Teaching coding from Class 3 onwards. Robotics club coordinator.");
        subject(arjun, "Computer Science", Board.CBSE, 3, 12);
        qualification(arjun, "B.Tech Computer Engineering", "VJTI Mumbai", 2019, false);
        experience(arjun, "Computer Teacher", "Podar International School", 2021, null, null);

        TeacherProfile fatima = teacher(hash, "fatima@demo.teachnet.in", "Fatima Khan",
                "Chemistry teacher · NEET faculty", "Delhi", "Delhi", 12, true,
                "12 years teaching organic chemistry. Author of two NEET chemistry workbooks.");
        subject(fatima, "Chemistry", Board.CBSE, 11, 12);
        qualification(fatima, "M.Sc. Chemistry", "University of Delhi", 2011, true);
        qualification(fatima, "CTET", "CBSE", 2012, true);
        experience(fatima, "Chemistry Faculty", "Allen Career Institute", 2017, null, null);

        TeacherProfile meera = teacher(hash, "meera@demo.teachnet.in", "Meera Nair",
                "Primary teacher · Montessori trained", "Bengaluru", "Karnataka", 7, false,
                "Early-years educator focused on phonics and play-based learning.");
        subject(meera, "All subjects (Primary)", Board.STATE_BOARD, 1, 5);
        qualification(meera, "Diploma in Montessori Education", "AMI India", 2017, false);
        experience(meera, "Primary Teacher", "Little Oaks Montessori", 2017, null, null);

        Institution greenfield = institution(hash, "hr@greenfield.demo.teachnet.in", "Greenfield International School",
                InstitutionType.SCHOOL, Board.CBSE, "Pune", "Maharashtra", true,
                "A CBSE K-12 school with 2,000 students and a strong science programme.",
                "https://example.com/greenfield");
        Institution brightpath = institution(hash, "jobs@brightpath.demo.teachnet.in", "BrightPath Coaching",
                InstitutionType.COACHING_CENTRE, Board.CBSE, "Delhi", "Delhi", false,
                "JEE and NEET coaching with centres across North India.", null);
        Institution oakwood = institution(hash, "careers@oakwood.demo.teachnet.in", "Oakwood IB World School",
                InstitutionType.SCHOOL, Board.IB, "Bengaluru", "Karnataka", true,
                "An IB continuum school (PYP, MYP, DP) focused on inquiry-based learning.", null);

        job(greenfield, "PGT Physics", "Physics", Board.CBSE, 11, 12, EmploymentType.FULL_TIME, 55000, 80000,
                "We are hiring an experienced PGT Physics teacher for Class 11–12. Lab experience and JEE exposure "
                        + "preferred. M.Sc. + B.Ed. required.", 2);
        job(greenfield, "TGT Mathematics", "Mathematics", Board.CBSE, 6, 10, EmploymentType.FULL_TIME, 40000, 60000,
                "Looking for a TGT Maths teacher who can make numbers fun for middle school students.", 5);
        job(greenfield, "Substitute English Teacher (3 months)", "English", Board.CBSE, 6, 8,
                EmploymentType.SUBSTITUTE, 25000, 30000, "Maternity-leave cover from next month.", 1);
        job(brightpath, "NEET Chemistry Faculty", "Chemistry", Board.CBSE, 11, 12, EmploymentType.FULL_TIME,
                90000, 150000, "Organic and physical chemistry faculty for our NEET programme. "
                        + "Minimum 5 years coaching experience.", 3);
        job(brightpath, "Weekend Physics Tutor", "Physics", Board.CBSE, 11, 12, EmploymentType.PART_TIME,
                20000, 35000, "Weekend doubt-clearing sessions for JEE students.", 7);
        job(oakwood, "IB DP English A Teacher", "English", Board.IB, 11, 12, EmploymentType.FULL_TIME, 70000, 110000,
                "IB-trained English A teacher for the Diploma Programme. IB workshop certification a plus.", 4);
        job(oakwood, "PYP Homeroom Teacher", "All subjects (Primary)", Board.IB, 1, 5, EmploymentType.FULL_TIME,
                45000, 65000, "Homeroom teacher for Grade 2. Montessori or PYP experience preferred.", 6);

        connect(priya, rahul, ConnectionStatus.ACCEPTED);
        connect(priya, fatima, ConnectionStatus.ACCEPTED);
        connect(ananya, meera, ConnectionStatus.ACCEPTED);
        connect(arjun, priya, ConnectionStatus.PENDING);
        connect(rahul, arjun, ConnectionStatus.ACCEPTED);

        follow(priya.getUser(), greenfield);
        follow(fatima.getUser(), brightpath);
        follow(ananya.getUser(), oakwood);
        follow(meera.getUser(), oakwood);
        follow(rahul.getUser(), greenfield);

        List<Post> created = new ArrayList<>();
        created.add(post(priya.getUser(), "Tried a new activity today: students built paper rockets to understand "
                + "projectile motion. The questions they asked afterwards were better than any worksheet! 🚀", 30));
        created.add(post(rahul.getUser(), "Olympiad tip for fellow maths teachers: give students one \"impossible\" "
                + "problem every Friday. No marks, just curiosity. Engagement has doubled.", 180));
        created.add(post(greenfield.getOwner(), "We're hiring! Greenfield International School is looking for a "
                + "PGT Physics and a TGT Mathematics teacher. Check our open positions on TeachNet.", 60));
        created.add(post(ananya.getUser(), "Reading recommendation for IB English teachers: \"The Reading "
                + "Environment\" by Aidan Chambers. It changed how I set up my classroom library.", 600));
        created.add(post(fatima.getUser(), "Sharing my free mnemonic sheet for named reactions in organic chemistry. "
                + "Message me if you'd like a copy for your students!", 1440));
        created.add(post(oakwood.getOwner(), "Proud of our Grade 5 students who presented their PYP Exhibition "
                + "projects on sustainable cities this week. 🌱", 2000));

        comment(created.get(0), rahul.getUser(), "Love this! Going to try it with my Class 9 students.");
        comment(created.get(0), fatima.getUser(), "Brilliant idea, Priya.");
        comment(created.get(3), meera.getUser(), "Adding it to my list, thanks!");

        log.info("Demo data created. Log in with e.g. priya@demo.teachnet.in / {}", DEMO_PASSWORD);
    }

    private TeacherProfile teacher(String hash, String email, String name, String headline, String city, String state,
                                   int years, boolean verified, String bio) {
        User user = users.save(new User(email, hash, name, Role.TEACHER));
        TeacherProfile p = new TeacherProfile(user);
        p.setHeadline(headline);
        p.setCity(city);
        p.setState(state);
        p.setYearsExperience(years);
        p.setVerified(verified);
        p.setBio(bio);
        return profiles.save(p);
    }

    private void subject(TeacherProfile p, String subject, Board board, int from, int to) {
        TeacherSubject s = new TeacherSubject();
        s.setProfile(p);
        s.setSubject(subject);
        s.setBoard(board);
        s.setGradeFrom(from);
        s.setGradeTo(to);
        p.getSubjects().add(s);
    }

    private void qualification(TeacherProfile p, String degree, String institute, int year, boolean verified) {
        Qualification q = new Qualification();
        q.setProfile(p);
        q.setDegree(degree);
        q.setInstitute(institute);
        q.setCompletionYear(year);
        q.setVerified(verified);
        p.getQualifications().add(q);
    }

    private void experience(TeacherProfile p, String title, String org, int start, Integer end, String description) {
        Experience e = new Experience();
        e.setProfile(p);
        e.setTitle(title);
        e.setOrganization(org);
        e.setStartYear(start);
        e.setEndYear(end);
        e.setDescription(description);
        p.getExperiences().add(e);
    }

    private void portfolio(TeacherProfile p, PortfolioType type, String title, String url) {
        PortfolioItem i = new PortfolioItem();
        i.setProfile(p);
        i.setItemType(type);
        i.setTitle(title);
        i.setUrl(url);
        p.getPortfolio().add(i);
    }

    private Institution institution(String hash, String email, String name, InstitutionType type, Board board,
                                    String city, String state, boolean verified, String about, String website) {
        User owner = users.save(new User(email, hash, name, Role.INSTITUTION));
        Institution i = new Institution(owner, name, type);
        i.setBoard(board);
        i.setCity(city);
        i.setState(state);
        i.setVerified(verified);
        i.setAbout(about);
        i.setWebsite(website);
        return institutions.save(i);
    }

    private void job(Institution inst, String title, String subject, Board board, int from, int to,
                     EmploymentType type, int salaryMin, int salaryMax, String description, int daysAgo) {
        JobOpening j = new JobOpening();
        j.setInstitution(inst);
        j.setTitle(title);
        j.setSubject(subject);
        j.setBoard(board);
        j.setGradeFrom(from);
        j.setGradeTo(to);
        j.setEmploymentType(type);
        j.setSalaryMin(salaryMin);
        j.setSalaryMax(salaryMax);
        j.setCity(inst.getCity());
        j.setDescription(description);
        j.setCreatedAt(Instant.now().minus(daysAgo, ChronoUnit.DAYS));
        jobs.save(j);
    }

    private void connect(TeacherProfile a, TeacherProfile b, ConnectionStatus status) {
        Connection c = new Connection(a.getUser(), b.getUser());
        c.setStatus(status);
        connections.save(c);
    }

    private void follow(User user, Institution inst) {
        follows.save(new Follow(user, inst));
    }

    private Post post(User author, String content, int minutesAgo) {
        Post p = new Post(author, content, null);
        p.setCreatedAt(Instant.now().minus(minutesAgo, ChronoUnit.MINUTES));
        return posts.save(p);
    }

    private void comment(Post post, User author, String content) {
        comments.save(new Comment(post, author, content));
        post.setCommentCount(post.getCommentCount() + 1);
    }
}
