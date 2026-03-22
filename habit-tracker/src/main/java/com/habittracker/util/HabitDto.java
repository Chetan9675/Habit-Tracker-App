package com.habittracker.util;

import com.habittracker.model.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class HabitDto {

    // ── Request DTOs ──────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateHabitRequest {
        @NotBlank(message = "Name is required")
        private String name;

        private String description;

        @NotNull(message = "Frequency is required")
        private HabitFrequency frequency;

        private HabitCategory category;
        private String emoji;
        private LocalDate startDate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateHabitRequest {
        private String name;
        private String description;
        private HabitFrequency frequency;
        private HabitCategory category;
        private String emoji;
        private Boolean active;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CompleteHabitRequest {
        private LocalDate date;         // defaults to today if null
        private String note;
        private MoodRating mood;
    }

    // ── Response DTOs ─────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HabitResponse {
        private Long id;
        private String name;
        private String description;
        private HabitFrequency frequency;
        private HabitCategory category;
        private String emoji;
        private LocalDate startDate;
        private boolean active;

        // Streak info
        private int currentStreak;
        private int longestStreak;
        private int totalCompletions;
        private LocalDate lastCompletedDate;
        private boolean completedToday;
        private double completionRate;

        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HabitLogResponse {
        private Long id;
        private Long habitId;
        private String habitName;
        private LocalDate completedDate;
        private String note;
        private MoodRating mood;
        private LocalDateTime loggedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardResponse {
        private long totalActiveHabits;
        private long completedToday;
        private long pendingToday;
        private double todayCompletionRate;

        private int globalLongestStreak;
        private int globalCurrentStreak;

        private List<HabitResponse> todayHabits;
        private List<HabitResponse> topStreaks;
        private List<HabitResponse> recentlyCompleted;

        private Map<String, Long> completionsByCategory;
        private List<DailyCompletionStat> last30Days;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DailyCompletionStat {
        private LocalDate date;
        private long completed;
        private long total;
        private double rate;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HabitStatsResponse {
        private Long habitId;
        private String habitName;
        private int currentStreak;
        private int longestStreak;
        private int totalCompletions;
        private double completionRate;
        private int completionsLast7Days;
        private int completionsLast30Days;
        private LocalDate bestStreakStartDate;
        private List<LocalDate> completionDates;
        private Map<String, Long> moodDistribution;

        // Milestone flags
        private boolean milestone7Days;
        private boolean milestone30Days;
        private boolean milestone100Days;
        private boolean milestone365Days;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StreakMilestone {
        private Long habitId;
        private String habitName;
        private String emoji;
        private int streakDays;
        private String milestoneTitle;
        private String milestoneMessage;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> ok(T data) {
            return ApiResponse.<T>builder().success(true).data(data).build();
        }

        public static <T> ApiResponse<T> ok(String message, T data) {
            return ApiResponse.<T>builder().success(true).message(message).data(data).build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder().success(false).message(message).build();
        }
    }
}
