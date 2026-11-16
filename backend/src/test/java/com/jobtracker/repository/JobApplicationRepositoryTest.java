package com.jobtracker.repository;

import com.jobtracker.entity.JobApplication;
import com.jobtracker.entity.User;
import com.jobtracker.enums.ApplicationStatus;
import com.jobtracker.enums.JobType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class JobApplicationRepositoryTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JobApplicationRepository applicationRepository;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = saveUser("alice@example.com");
        bob = saveUser("bob@example.com");

        save(alice, "Acme Software", "Java Developer", JobType.FULL_TIME, ApplicationStatus.APPLIED, LocalDate.of(2026, 9, 1));
        save(alice, "Globex", "Frontend Intern", JobType.INTERNSHIP, ApplicationStatus.INTERVIEW, LocalDate.of(2026, 10, 5));
        save(alice, "Initech", "100% Remote Support", JobType.PART_TIME, ApplicationStatus.OFFER, LocalDate.of(2026, 10, 20));
        save(alice, "Umbrella", "1000 Remote Tester", JobType.FULL_TIME, ApplicationStatus.REJECTED, LocalDate.of(2026, 11, 2));
        save(bob, "Acme Bob Corp", "Secret Role", JobType.FULL_TIME, ApplicationStatus.OFFER, LocalDate.of(2026, 10, 1));
    }

    @Test
    void findByIdAndUserIdOnlyReturnsOwnedRows() {
        Long bobsApplicationId = applicationRepository.findAll().stream()
                .filter(a -> a.getCompanyName().equals("Acme Bob Corp"))
                .findFirst().orElseThrow().getId();

        assertThat(applicationRepository.findByIdAndUserId(bobsApplicationId, bob.getId())).isPresent();
        assertThat(applicationRepository.findByIdAndUserId(bobsApplicationId, alice.getId())).isEmpty();
        assertThat(applicationRepository.existsByIdAndUserId(bobsApplicationId, alice.getId())).isFalse();
    }

    @Test
    void ownedBySpecificationNeverLeaksOtherUsersRows() {
        List<JobApplication> result = applicationRepository.findAll(JobApplicationSpecifications.ownedBy(alice.getId()));

        assertThat(result).hasSize(4);
        assertThat(result).extracting(a -> a.getUser().getId()).containsOnly(alice.getId());
    }

    @Test
    void searchMatchesCompanyOrTitleCaseInsensitivelyWithinOwnRows() {
        Specification<JobApplication> byCompany = Specification
                .where(JobApplicationSpecifications.ownedBy(alice.getId()))
                .and(JobApplicationSpecifications.matchesSearch("ACME"));
        Specification<JobApplication> byTitle = Specification
                .where(JobApplicationSpecifications.ownedBy(alice.getId()))
                .and(JobApplicationSpecifications.matchesSearch("intern"));

        assertThat(applicationRepository.findAll(byCompany)).extracting(JobApplication::getCompanyName)
                .containsExactly("Acme Software");   // "Acme Bob Corp" belongs to Bob
        assertThat(applicationRepository.findAll(byTitle)).extracting(JobApplication::getCompanyName)
                .containsExactly("Globex");
    }

    @Test
    void likeWildcardsInSearchTermAreTreatedLiterally() {
        Specification<JobApplication> percent = Specification
                .where(JobApplicationSpecifications.ownedBy(alice.getId()))
                .and(JobApplicationSpecifications.matchesSearch("100%"));

        assertThat(applicationRepository.findAll(percent)).extracting(JobApplication::getCompanyName)
                .containsExactly("Initech");
    }

    @Test
    void filtersByStatusJobTypeAndDateRange() {
        Specification<JobApplication> status = Specification
                .where(JobApplicationSpecifications.ownedBy(alice.getId()))
                .and(JobApplicationSpecifications.hasStatus(ApplicationStatus.OFFER));
        Specification<JobApplication> type = Specification
                .where(JobApplicationSpecifications.ownedBy(alice.getId()))
                .and(JobApplicationSpecifications.hasJobType(JobType.FULL_TIME));
        Specification<JobApplication> range = Specification
                .where(JobApplicationSpecifications.ownedBy(alice.getId()))
                .and(JobApplicationSpecifications.appliedOnOrAfter(LocalDate.of(2026, 10, 1)))
                .and(JobApplicationSpecifications.appliedOnOrBefore(LocalDate.of(2026, 10, 31)));

        assertThat(applicationRepository.findAll(status)).extracting(JobApplication::getCompanyName)
                .containsExactly("Initech");
        assertThat(applicationRepository.findAll(type)).extracting(JobApplication::getCompanyName)
                .containsExactlyInAnyOrder("Acme Software", "Umbrella");
        assertThat(applicationRepository.findAll(range)).extracting(JobApplication::getCompanyName)
                .containsExactlyInAnyOrder("Globex", "Initech");
    }

    @Test
    void paginatesAndSortsNewestFirst() {
        Specification<JobApplication> mine = JobApplicationSpecifications.ownedBy(alice.getId());
        PageRequest firstPage = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "applicationDate", "id"));
        PageRequest secondPage = PageRequest.of(1, 3, Sort.by(Sort.Direction.DESC, "applicationDate", "id"));

        Page<JobApplication> page1 = applicationRepository.findAll(mine, firstPage);
        Page<JobApplication> page2 = applicationRepository.findAll(mine, secondPage);

        assertThat(page1.getTotalElements()).isEqualTo(4);
        assertThat(page1.getTotalPages()).isEqualTo(2);
        assertThat(page1.getContent()).extracting(JobApplication::getCompanyName)
                .containsExactly("Umbrella", "Initech", "Globex");
        assertThat(page2.getContent()).extracting(JobApplication::getCompanyName)
                .containsExactly("Acme Software");
    }

    @Test
    void countByStatusIsScopedToUser() {
        List<Object[]> rows = applicationRepository.countByStatus(alice.getId());

        long total = rows.stream().mapToLong(row -> ((Number) row[1]).longValue()).sum();
        assertThat(total).isEqualTo(4);
        assertThat(rows).extracting(row -> (ApplicationStatus) row[0])
                .containsExactlyInAnyOrder(ApplicationStatus.APPLIED, ApplicationStatus.INTERVIEW,
                        ApplicationStatus.OFFER, ApplicationStatus.REJECTED);
    }

    @Test
    void countByMonthGroupsApplicationsByYearAndMonth() {
        List<Object[]> rows = applicationRepository.countByMonthSince(alice.getId(), LocalDate.of(2026, 10, 1));

        assertThat(rows).hasSize(2);
        long october = rows.stream()
                .filter(row -> ((Number) row[1]).intValue() == 10)
                .mapToLong(row -> ((Number) row[2]).longValue()).sum();
        long november = rows.stream()
                .filter(row -> ((Number) row[1]).intValue() == 11)
                .mapToLong(row -> ((Number) row[2]).longValue()).sum();
        assertThat(october).isEqualTo(2);
        assertThat(november).isEqualTo(1);
    }

    // ---- helpers -------------------------------------------------------------------------

    private User saveUser(String email) {
        User user = new User();
        user.setFullName("Test User");
        user.setEmail(email);
        user.setPasswordHash("hash");
        return userRepository.saveAndFlush(user);
    }

    private void save(User owner, String company, String title, JobType type, ApplicationStatus status, LocalDate date) {
        JobApplication application = new JobApplication();
        application.setUser(owner);
        application.setCompanyName(company);
        application.setJobTitle(title);
        application.setJobType(type);
        application.setStatus(status);
        application.setApplicationDate(date);
        applicationRepository.saveAndFlush(application);
    }
}
