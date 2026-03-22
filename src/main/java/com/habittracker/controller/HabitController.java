package com.habittracker.controller;

import com.habittracker.service.HabitService;
import com.habittracker.util.HabitDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class HabitController {

    private final HabitService habitService;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<HabitDto.ApiResponse<HabitDto.HabitResponse>> createHabit(
            @Valid @RequestBody HabitDto.CreateHabitRequest request) {
        HabitDto.HabitResponse habit = habitService.createHabit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(HabitDto.ApiResponse.ok("Habit created successfully", habit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HabitDto.ApiResponse<HabitDto.HabitResponse>> getHabit(@PathVariable Long id) {
        return ResponseEntity.ok(HabitDto.ApiResponse.ok(habitService.getHabit(id)));
    }

    @GetMapping
    public ResponseEntity<HabitDto.ApiResponse<List<HabitDto.HabitResponse>>> getAllHabits(
            @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(HabitDto.ApiResponse.ok(habitService.getAllHabits(activeOnly)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<HabitDto.ApiResponse<HabitDto.HabitResponse>> updateHabit(
            @PathVariable Long id,
            @RequestBody HabitDto.UpdateHabitRequest request) {
        return ResponseEntity.ok(HabitDto.ApiResponse.ok("Habit updated", habitService.updateHabit(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<HabitDto.ApiResponse<Void>> deleteHabit(@PathVariable Long id) {
        habitService.deleteHabit(id);
        return ResponseEntity.ok(HabitDto.ApiResponse.ok("Habit deleted", null));
    }

    // ── Completion ────────────────────────────────────────────────────────────

    @PostMapping("/{id}/complete")
    public ResponseEntity<HabitDto.ApiResponse<HabitDto.HabitResponse>> completeHabit(
            @PathVariable Long id,
            @RequestBody(required = false) HabitDto.CompleteHabitRequest request) {
        if (request == null) request = new HabitDto.CompleteHabitRequest();
        HabitDto.HabitResponse result = habitService.completeHabit(id, request);
        return ResponseEntity.ok(HabitDto.ApiResponse.ok("🎉 Habit completed! Keep the streak alive!", result));
    }

    @DeleteMapping("/{id}/complete")
    public ResponseEntity<HabitDto.ApiResponse<HabitDto.HabitResponse>> uncompleteHabit(
            @PathVariable Long id,
            @RequestParam(required = false) String date) {
        LocalDate targetDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        return ResponseEntity.ok(HabitDto.ApiResponse.ok("Completion removed", habitService.uncompleteHabit(id, targetDate)));
    }

    // ── Logs ──────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/logs")
    public ResponseEntity<HabitDto.ApiResponse<List<HabitDto.HabitLogResponse>>> getHabitLogs(@PathVariable Long id) {
        return ResponseEntity.ok(HabitDto.ApiResponse.ok(habitService.getHabitLogs(id)));
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    @GetMapping("/{id}/stats")
    public ResponseEntity<HabitDto.ApiResponse<HabitDto.HabitStatsResponse>> getHabitStats(@PathVariable Long id) {
        return ResponseEntity.ok(HabitDto.ApiResponse.ok(habitService.getHabitStats(id)));
    }
}
