package com.softer.riderassignment;

import com.softer.riderassignment.model.AssignmentStatus;
import com.softer.riderassignment.model.RiderAssignment;
import com.softer.riderassignment.repository.TestH2Repository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ApplicationTest {

    private static final String INSERT_ASSIGNMENT =
            "INSERT INTO rider_assignments (id, delivery_id, rider_id, status, assigned_at) "
            + "VALUES (1, 1001, 501, 'ASSIGNED', TIMESTAMP '2026-11-18 10:00:00')";
    private static final String DELETE_ASSIGNMENTS = "DELETE FROM rider_assignments";

    @LocalServerPort
    private int port;

    private String baseUrl = "http://localhost";
    private static RestTemplate restTemplate;

    @Autowired
    private TestH2Repository testH2Repository;

    @BeforeAll
    public static void init() {
        restTemplate = new RestTemplate();
    }

    @BeforeEach
    public void setUp() {
        baseUrl = "http://localhost:" + port + "/api/assignments";
    }

    @Test
    @DisplayName("should get 0 assignments when table is empty")
    public void test1_getAssignmentsWhenTableIsEmpty() {
        List<?> response = restTemplate.getForObject(baseUrl, List.class);

        assertNotNull(response);
        assertEquals(0, response.size());
        assertEquals(0, testH2Repository.findAll().size());
    }

    @Test
    @DisplayName("should get all assignments when table is not empty")
    @Sql(statements = INSERT_ASSIGNMENT, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = DELETE_ASSIGNMENTS, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void test2_getAssignmentsWhenTableIsNotEmpty() {
        List<?> response = restTemplate.getForObject(baseUrl, List.class);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(1, testH2Repository.findAll().size());
    }

    @Test
    @DisplayName("should register a new assignment")
    @Sql(statements = DELETE_ASSIGNMENTS, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void test3_registerAssignment() {
        RiderAssignment assignment = new RiderAssignment(
                null, 1002L, 502L, AssignmentStatus.ASSIGNED, null);

        RiderAssignment response = restTemplate.postForObject(baseUrl, assignment, RiderAssignment.class);

        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(1002L, response.getDeliveryId());
        assertEquals(502L, response.getRiderId());
        assertEquals(AssignmentStatus.ASSIGNED, response.getStatus());
        assertNotNull(response.getAssignedAt());

        List<RiderAssignment> storedAssignments = testH2Repository.findAll();
        assertEquals(1, storedAssignments.size());
        RiderAssignment storedAssignment = storedAssignments.getFirst();
        assertEquals(response.getId(), storedAssignment.getId());
        assertEquals(1002L, storedAssignment.getDeliveryId());
        assertEquals(502L, storedAssignment.getRiderId());
        assertEquals(AssignmentStatus.ASSIGNED, storedAssignment.getStatus());
    }

    @Test
    @DisplayName("should get assignment with id=1")
    @Sql(statements = INSERT_ASSIGNMENT, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = DELETE_ASSIGNMENTS, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void test4_getAssignmentById() {
        RiderAssignment response = restTemplate.getForObject(baseUrl + "/{id}", RiderAssignment.class, 1L);

        assertAll(
                () -> assertNotNull(response),
                () -> assertEquals(1L, response.getId()),
                () -> assertEquals(1001L, response.getDeliveryId()),
                () -> assertEquals(501L, response.getRiderId()),
                () -> assertEquals(AssignmentStatus.ASSIGNED, response.getStatus()),
                () -> assertEquals(LocalDateTime.of(2026, 11, 18, 10, 0), response.getAssignedAt())
        );
    }

    @Test
    @DisplayName("should update assignment with id=1")
    @Sql(statements = INSERT_ASSIGNMENT, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
    @Sql(statements = DELETE_ASSIGNMENTS, executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
    public void test5_updateAssignment() {
        RiderAssignment updatedAssignment = new RiderAssignment(
                null, 1003L, 503L, AssignmentStatus.ACCEPTED, null);

        restTemplate.put(baseUrl + "/{id}", updatedAssignment, 1L);

        RiderAssignment storedAssignment = testH2Repository.findById(1L).orElse(null);
        assertAll(
                () -> assertNotNull(storedAssignment),
                () -> assertEquals(1L, storedAssignment.getId()),
                () -> assertEquals(1003L, storedAssignment.getDeliveryId()),
                () -> assertEquals(503L, storedAssignment.getRiderId()),
                () -> assertEquals(AssignmentStatus.ACCEPTED, storedAssignment.getStatus()),
                () -> assertEquals(LocalDateTime.of(2026, 11, 18, 10, 0), storedAssignment.getAssignedAt())
        );
    }
}
