package com.habittracker.controller;

import com.habittracker.service.HabitService;
import com.habittracker.util.HabitDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final HabitService habitService;

    @GetMapping
    public ResponseEntity<HabitDto.ApiResponse<HabitDto.DashboardResponse>> getDashboard() {
        return ResponseEntity.ok(HabitDto.ApiResponse.ok(habitService.getDashboard()));
    }

    @GetMapping("/milestones")
    public ResponseEntity<HabitDto.ApiResponse<List<HabitDto.StreakMilestone>>> getMilestones() {
        return ResponseEntity.ok(HabitDto.ApiResponse.ok(habitService.getActiveMilestones()));
    }
}
