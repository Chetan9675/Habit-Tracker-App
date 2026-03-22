package com.habittracker.util;

import com.habittracker.model.Habit;
import com.habittracker.model.HabitLog;
import org.springframework.stereotype.Component;

@Component
public class HabitMapper {

    public HabitDto.HabitResponse toResponse(Habit habit) {
        return HabitDto.HabitResponse.builder()
                .id(habit.getId())
                .name(habit.getName())
                .description(habit.getDescription())
                .frequency(habit.getFrequency())
                .category(habit.getCategory())
                .emoji(habit.getEmoji())
                .startDate(habit.getStartDate())
                .active(habit.isActive())
                .currentStreak(habit.getCurrentStreak())
                .longestStreak(habit.getLongestStreak())
                .totalCompletions(habit.getTotalCompletions())
                .lastCompletedDate(habit.getLastCompletedDate())
                .completedToday(habit.isCompletedToday())
                .completionRate(Math.round(habit.getCompletionRate() * 10.0) / 10.0)
                .createdAt(habit.getCreatedAt())
                .updatedAt(habit.getUpdatedAt())
                .build();
    }

    public HabitDto.HabitLogResponse toLogResponse(HabitLog log) {
        return HabitDto.HabitLogResponse.builder()
                .id(log.getId())
                .habitId(log.getHabit().getId())
                .habitName(log.getHabit().getName())
                .completedDate(log.getCompletedDate())
                .note(log.getNote())
                .mood(log.getMood())
                .loggedAt(log.getLoggedAt())
                .build();
    }
}
