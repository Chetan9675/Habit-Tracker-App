package com.habittracker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.habittracker.model.HabitCategory;
import com.habittracker.model.HabitFrequency;
import com.habittracker.util.HabitDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class HabitControllerIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    private HabitDto.CreateHabitRequest buildRequest(String name) {
        return HabitDto.CreateHabitRequest.builder()
                .name(name)
                .frequency(HabitFrequency.DAILY)
                .category(HabitCategory.FITNESS)
                .emoji("🏃")
                .build();
    }

    @Test
    @DisplayName("POST /api/habits - creates a new habit")
    void createHabit_returnsCreated() throws Exception {
        mockMvc.perform(post("/api/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Run 5km"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Run 5km"))
                .andExpect(jsonPath("$.data.currentStreak").value(0))
                .andExpect(jsonPath("$.data.completedToday").value(false));
    }

    @Test
    @DisplayName("POST /api/habits - rejects missing name")
    void createHabit_missingName_returnsBadRequest() throws Exception {
        HabitDto.CreateHabitRequest bad = HabitDto.CreateHabitRequest.builder()
                .frequency(HabitFrequency.DAILY)
                .build();

        mockMvc.perform(post("/api/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /api/habits - returns list of habits")
    void getAllHabits_returnsList() throws Exception {
        // Create a habit first
        mockMvc.perform(post("/api/habits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest("Meditate"))));

        mockMvc.perform(get("/api/habits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("GET /api/habits/{id} - returns 404 for unknown habit")
    void getHabit_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/habits/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/habits/{id}/complete - completes a habit")
    void completeHabit_returnsUpdatedHabit() throws Exception {
        // Create
        String resp = mockMvc.perform(post("/api/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Cold Shower"))))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).at("/data/id").asLong();

        mockMvc.perform(post("/api/habits/" + id + "/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.completedToday").value(true))
                .andExpect(jsonPath("$.data.currentStreak").value(1))
                .andExpect(jsonPath("$.data.totalCompletions").value(1));
    }

    @Test
    @DisplayName("POST /api/habits/{id}/complete - conflict on double complete")
    void completeHabit_twice_returnsConflict() throws Exception {
        String resp = mockMvc.perform(post("/api/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Journaling"))))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).at("/data/id").asLong();

        mockMvc.perform(post("/api/habits/" + id + "/complete")
                .contentType(MediaType.APPLICATION_JSON).content("{}"));

        mockMvc.perform(post("/api/habits/" + id + "/complete")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/habits/{id}/stats - returns stats")
    void getHabitStats_returnsStats() throws Exception {
        String resp = mockMvc.perform(post("/api/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Read"))))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).at("/data/id").asLong();

        mockMvc.perform(get("/api/habits/" + id + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.habitId").value(id))
                .andExpect(jsonPath("$.data.milestone7Days").value(false));
    }

    @Test
    @DisplayName("DELETE /api/habits/{id} - deletes a habit")
    void deleteHabit_returnsOk() throws Exception {
        String resp = mockMvc.perform(post("/api/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Stretch"))))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).at("/data/id").asLong();

        mockMvc.perform(delete("/api/habits/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/habits/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/habits/{id} - updates a habit")
    void updateHabit_returnsUpdated() throws Exception {
        String resp = mockMvc.perform(post("/api/habits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Walk"))))
                .andReturn().getResponse().getContentAsString();

        Long id = objectMapper.readTree(resp).at("/data/id").asLong();

        HabitDto.UpdateHabitRequest update = new HabitDto.UpdateHabitRequest();
        update.setName("Power Walk");

        mockMvc.perform(put("/api/habits/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Power Walk"));
    }

    @Test
    @DisplayName("GET /api/dashboard - returns dashboard data")
    void getDashboard_returnsData() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalActiveHabits").isNumber())
                .andExpect(jsonPath("$.data.todayHabits").isArray())
                .andExpect(jsonPath("$.data.last30Days").isArray());
    }
}
