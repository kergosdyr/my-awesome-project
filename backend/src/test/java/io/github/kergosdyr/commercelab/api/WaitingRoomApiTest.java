package io.github.kergosdyr.commercelab.api;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kergosdyr.commercelab.api.waitingroom.WaitingRoomController;
import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomService;
import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomTestFixtures.FakeFlashSaleClient;
import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomTestFixtures.FakeWaitingRoomRepository;
import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomTestFixtures.MutableClock;
import io.github.kergosdyr.commercelab.domain.waitingroom.WaitingRoomTestFixtures.TestPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class WaitingRoomApiTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private FakeFlashSaleClient flashSaleClient;

    @BeforeEach
    void setUp() {
        flashSaleClient = new FakeFlashSaleClient();
        var service = new WaitingRoomService(
                new FakeWaitingRoomRepository(),
                flashSaleClient,
                TestPolicy.oneSlot(),
                new MutableClock(Instant.parse("2026-08-11T00:00:00Z"))
        );
        mockMvc = MockMvcBuilders.standaloneSetup(new WaitingRoomController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void runsTheQueuedTicketPollAndPurchaseFlowWithoutRedis() throws Exception {
        var ticketResponse = mockMvc.perform(post("/api/labs/waiting-room/tickets"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.error").value(nullValue()))
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.pollAfterMillis").value(1000))
                .andReturn();
        var ticketId = objectMapper.readTree(ticketResponse.getResponse().getContentAsString())
                .path("data").path("ticketId").asText();

        var pollResponse = mockMvc.perform(get("/api/labs/waiting-room/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ADMITTED"))
                .andExpect(jsonPath("$.data.admissionToken").isNotEmpty())
                .andExpect(jsonPath("$.data.pollAfterMillis").value(0))
                .andReturn();
        var admissionToken = objectMapper.readTree(pollResponse.getResponse().getContentAsString())
                .path("data").path("admissionToken").asText();

        var purchaseBody = """
                {"ticketId":"%s","admissionToken":"%s"}
                """.formatted(ticketId, admissionToken);
        mockMvc.perform(post("/api/labs/waiting-room/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mode").value("WAITING_ROOM"))
                .andExpect(jsonPath("$.data.status").value("ACCEPTED"));

        mockMvc.perform(post("/api/labs/waiting-room/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(purchaseBody))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error.code").value("WAITING_ROOM_TICKET_EXPIRED"));

        mockMvc.perform(get("/api/labs/waiting-room/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.waitingRoom.completed").value(1))
                .andExpect(jsonPath("$.data.waitingRoom.maxActive").value(1))
                .andExpect(jsonPath("$.data.waitingRoom.fifoViolations").value(0));
    }

    @Test
    void returnsOneSecondPollingHintWhileATicketIsQueued() throws Exception {
        var activeTicketResponse = mockMvc.perform(post("/api/labs/waiting-room/tickets"))
                .andExpect(status().isAccepted())
                .andReturn();
        var activeTicketId = objectMapper.readTree(
                        activeTicketResponse.getResponse().getContentAsString()
                )
                .path("data").path("ticketId").asText();
        mockMvc.perform(get("/api/labs/waiting-room/tickets/{ticketId}", activeTicketId))
                .andExpect(status().isOk());

        var queuedTicketResponse = mockMvc.perform(post("/api/labs/waiting-room/tickets"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.pollAfterMillis").value(1000))
                .andReturn();
        var queuedTicketId = objectMapper.readTree(
                        queuedTicketResponse.getResponse().getContentAsString()
                )
                .path("data").path("ticketId").asText();
        mockMvc.perform(get(
                        "/api/labs/waiting-room/tickets/{ticketId}",
                        queuedTicketId
                ))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("QUEUED"))
                .andExpect(jsonPath("$.data.pollAfterMillis").value(1000));
    }

    @Test
    void mapsAdmissionBypassAndDirectOverloadToExplicitHttpStatuses() throws Exception {
        var ticketResponse = mockMvc.perform(post("/api/labs/waiting-room/tickets"))
                .andExpect(status().isAccepted())
                .andReturn();
        var ticketId = objectMapper.readTree(ticketResponse.getResponse().getContentAsString())
                .path("data").path("ticketId").asText();
        mockMvc.perform(get("/api/labs/waiting-room/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/labs/waiting-room/purchase")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ticketId":"%s","admissionToken":"forged-token"}
                                """.formatted(ticketId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("WAITING_ROOM_ADMISSION_REQUIRED"));

        flashSaleClient.rejectRequests();
        mockMvc.perform(post("/api/labs/waiting-room/direct"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("WAITING_ROOM_CAPACITY_EXCEEDED"));
    }
}
