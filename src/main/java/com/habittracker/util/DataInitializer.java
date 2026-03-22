package com.habittracker.util;

import com.habittracker.model.*;
import com.habittracker.repository.HabitLogRepository;
import com.habittracker.repository.HabitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Seeds the database with sample habits on startup (dev profile only).
 * Remove @Profile("dev") to always seed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;

    @Override
    public void run(String... args) {
        if (habitRepository.count() > 0) return;

        log.info("Seeding demo habits...");

        Habit meditation = habitRepository.save(Habit.builder()
                .name("Morning Meditation")
                .description("10 minutes of mindfulness to start the day")
                .frequency(HabitFrequency.DAILY)
                .category(HabitCategory.MINDFULNESS)
                .emoji("🧘")
                .startDate(LocalDate.now().minusDays(20))
                .active(true)
                .build());

        Habit reading = habitRepository.save(Habit.builder()
                .name("Read 30 Minutes")
                .description("Read a book or educational content")
                .frequency(HabitFrequency.DAILY)
                .category(HabitCategory.LEARNING)
                .emoji("📚")
                .startDate(LocalDate.now().minusDays(15))
                .active(true)
                .build());

        Habit exercise = habitRepository.save(Habit.builder()
                .name("Exercise")
                .description("At least 30 mins of physical activity")
                .frequency(HabitFrequency.DAILY)
                .category(HabitCategory.FITNESS)
                .emoji("💪")
                .startDate(LocalDate.now().minusDays(30))
                .active(true)
                .build());

        Habit water = habitRepository.save(Habit.builder()
                .name("Drink 8 Glasses of Water")
                .description("Stay hydrated throughout the day")
                .frequency(HabitFrequency.DAILY)
                .category(HabitCategory.HEALTH)
                .emoji("💧")
                .startDate(LocalDate.now().minusDays(10))
                .active(true)
                .build());

        Habit journal = habitRepository.save(Habit.builder()
                .name("Gratitude Journal")
                .description("Write 3 things you're grateful for")
                .frequency(HabitFrequency.DAILY)
                .category(HabitCategory.MINDFULNESS)
                .emoji("📓")
                .startDate(LocalDate.now().minusDays(7))
                .active(true)
                .build());

        // Seed completion logs and streaks
        seedCompletions(meditation, 18);
        seedCompletions(reading, 12);
        seedCompletions(exercise, 25);
        seedCompletions(water, 8);
        seedCompletions(journal, 6);

        log.info("Demo data seeded successfully.");
    }

    private void seedCompletions(Habit habit, int daysBack) {
        LocalDate today = LocalDate.now();
        int streak = 0;

        for (int i = daysBack; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            // Skip some days randomly for realism
            if (i > 3 && i % 7 == 0) continue;

            if (!habitLogRepository.existsByHabitIdAndCompletedDate(habit.getId(), day)) {
                habitLogRepository.save(HabitLog.builder()
                        .habit(habit)
                        .completedDate(day)
                        .mood(MoodRating.values()[(int)(Math.random() * MoodRating.values().length)])
                        .build());

                habit.setTotalCompletions(habit.getTotalCompletions() + 1);
                streak++;
            }
        }

        // Set today as complete and calculate streak
        habit.setCurrentStreak(streak);
        habit.setLongestStreak(Math.max(habit.getLongestStreak(), streak));
        habit.setLastCompletedDate(today);
        habitRepository.save(habit);
    }
}
