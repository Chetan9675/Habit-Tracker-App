package com.habittracker.service;

import com.habittracker.model.*;
import com.habittracker.repository.HabitLogRepository;
import com.habittracker.repository.HabitRepository;
import com.habittracker.util.HabitDto;
import com.habittracker.util.HabitMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitLogRepository habitLogRepository;
    private final HabitMapper habitMapper;

    // ── CRUD ──────────────────────────────────────────────────────────────────

    public HabitDto.HabitResponse createHabit(HabitDto.CreateHabitRequest request) {
        Habit habit = Habit.builder()
                .name(request.getName())
                .description(request.getDescription())
                .frequency(request.getFrequency())
                .category(request.getCategory() != null ? request.getCategory() : HabitCategory.OTHER)
                .emoji(request.getEmoji() != null ? request.getEmoji() : "✅")
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
                .active(true)
                .build();

        Habit saved = habitRepository.save(habit);
        log.info("Created habit: {} (id={})", saved.getName(), saved.getId());
        return habitMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public HabitDto.HabitResponse getHabit(Long id) {
        return habitMapper.toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public List<HabitDto.HabitResponse> getAllHabits(boolean activeOnly) {
        List<Habit> habits = activeOnly
                ? habitRepository.findAllActiveOrderedByStreak()
                : habitRepository.findAll();

        habits.forEach(Habit::evaluateStreakBreak);
        return habits.stream().map(habitMapper::toResponse).collect(Collectors.toList());
    }

    public HabitDto.HabitResponse updateHabit(Long id, HabitDto.UpdateHabitRequest request) {
        Habit habit = findOrThrow(id);

        if (request.getName() != null)        habit.setName(request.getName());
        if (request.getDescription() != null) habit.setDescription(request.getDescription());
        if (request.getFrequency() != null)   habit.setFrequency(request.getFrequency());
        if (request.getCategory() != null)    habit.setCategory(request.getCategory());
        if (request.getEmoji() != null)        habit.setEmoji(request.getEmoji());
        if (request.getActive() != null)       habit.setActive(request.getActive());

        return habitMapper.toResponse(habitRepository.save(habit));
    }

    public void deleteHabit(Long id) {
        if (!habitRepository.existsById(id)) {
            throw new EntityNotFoundException("Habit not found: " + id);
        }
        habitRepository.deleteById(id);
        log.info("Deleted habit id={}", id);
    }

    // ── Completion ────────────────────────────────────────────────────────────

    public HabitDto.HabitResponse completeHabit(Long id, HabitDto.CompleteHabitRequest request) {
        Habit habit = findOrThrow(id);
        LocalDate targetDate = request.getDate() != null ? request.getDate() : LocalDate.now();

        if (habitLogRepository.existsByHabitIdAndCompletedDate(id, targetDate)) {
            throw new IllegalStateException("Habit already completed for " + targetDate);
        }

        // Create log entry
        HabitLog log = HabitLog.builder()
                .habit(habit)
                .completedDate(targetDate)
                .note(request.getNote())
                .mood(request.getMood() != null ? request.getMood() : MoodRating.NEUTRAL)
                .build();
        habitLogRepository.save(log);

        // Update streak on the habit
        if (targetDate.equals(LocalDate.now())) {
            habit.completeToday();
        } else {
            // Backfill: just count it
            habit.setTotalCompletions(habit.getTotalCompletions() + 1);
        }

        return habitMapper.toResponse(habitRepository.save(habit));
    }

    public HabitDto.HabitResponse uncompleteHabit(Long id, LocalDate date) {
        Habit habit = findOrThrow(id);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        HabitLog logEntry = habitLogRepository.findByHabitIdAndCompletedDate(id, targetDate)
                .orElseThrow(() -> new EntityNotFoundException("No log found for " + targetDate));

        habitLogRepository.delete(logEntry);

        // Recompute streak from remaining logs
        recomputeStreak(habit);

        return habitMapper.toResponse(habitRepository.save(habit));
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public HabitDto.HabitStatsResponse getHabitStats(Long id) {
        Habit habit = findOrThrow(id);

        LocalDate today = LocalDate.now();
        LocalDate sevenDaysAgo = today.minusDays(7);
        LocalDate thirtyDaysAgo = today.minusDays(30);

        long last7 = habitLogRepository.countCompletionsSince(id, sevenDaysAgo);
        long last30 = habitLogRepository.countCompletionsSince(id, thirtyDaysAgo);

        List<LocalDate> completionDates = habitLogRepository.findCompletionDatesByHabitId(id);

        // Mood distribution
        List<HabitLog> allLogs = habitLogRepository.findByHabitIdOrderByCompletedDateDesc(id);
        Map<String, Long> moodDist = allLogs.stream()
                .collect(Collectors.groupingBy(l -> l.getMood().name(), Collectors.counting()));

        int streak = habit.getCurrentStreak();

        return HabitDto.HabitStatsResponse.builder()
                .habitId(habit.getId())
                .habitName(habit.getName())
                .currentStreak(streak)
                .longestStreak(habit.getLongestStreak())
                .totalCompletions(habit.getTotalCompletions())
                .completionRate(habit.getCompletionRate())
                .completionsLast7Days((int) last7)
                .completionsLast30Days((int) last30)
                .completionDates(completionDates)
                .moodDistribution(moodDist)
                .milestone7Days(streak >= 7)
                .milestone30Days(streak >= 30)
                .milestone100Days(streak >= 100)
                .milestone365Days(streak >= 365)
                .build();
    }

    @Transactional(readOnly = true)
    public HabitDto.DashboardResponse getDashboard() {
        LocalDate today = LocalDate.now();
        List<Habit> activeHabits = habitRepository.findByActiveTrue();
        activeHabits.forEach(Habit::evaluateStreakBreak);

        long total = activeHabits.size();
        long completedToday = activeHabits.stream().filter(Habit::isCompletedToday).count();
        long pending = total - completedToday;
        double rate = total > 0 ? (completedToday * 100.0 / total) : 0;

        int globalCurrentStreak = activeHabits.stream()
                .mapToInt(Habit::getCurrentStreak).max().orElse(0);
        int globalLongestStreak = activeHabits.stream()
                .mapToInt(Habit::getLongestStreak).max().orElse(0);

        List<HabitDto.HabitResponse> topStreaks = activeHabits.stream()
                .sorted(Comparator.comparingInt(Habit::getCurrentStreak).reversed())
                .limit(5)
                .map(habitMapper::toResponse)
                .collect(Collectors.toList());

        List<HabitDto.HabitResponse> todayList = activeHabits.stream()
                .map(habitMapper::toResponse)
                .collect(Collectors.toList());

        List<HabitDto.HabitResponse> recentlyCompleted = activeHabits.stream()
                .filter(Habit::isCompletedToday)
                .map(habitMapper::toResponse)
                .collect(Collectors.toList());

        // Completions by category
        Map<String, Long> byCategory = activeHabits.stream()
                .filter(h -> h.getCategory() != null)
                .collect(Collectors.groupingBy(
                        h -> h.getCategory().name(),
                        Collectors.summingLong(Habit::getTotalCompletions)
                ));

        // Last 30 days daily stats
        List<HabitDto.DailyCompletionStat> last30 = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            List<HabitLog> dayLogs = habitLogRepository.findAllByDate(day);
            long dayCompleted = dayLogs.size();
            HabitDto.DailyCompletionStat stat = HabitDto.DailyCompletionStat.builder()
                    .date(day)
                    .completed(dayCompleted)
                    .total(total)
                    .rate(total > 0 ? dayCompleted * 100.0 / total : 0)
                    .build();
            last30.add(stat);
        }

        return HabitDto.DashboardResponse.builder()
                .totalActiveHabits(total)
                .completedToday(completedToday)
                .pendingToday(pending)
                .todayCompletionRate(Math.round(rate * 10.0) / 10.0)
                .globalCurrentStreak(globalCurrentStreak)
                .globalLongestStreak(globalLongestStreak)
                .todayHabits(todayList)
                .topStreaks(topStreaks)
                .recentlyCompleted(recentlyCompleted)
                .completionsByCategory(byCategory)
                .last30Days(last30)
                .build();
    }

    @Transactional(readOnly = true)
    public List<HabitDto.HabitLogResponse> getHabitLogs(Long id) {
        findOrThrow(id);
        return habitLogRepository.findByHabitIdOrderByCompletedDateDesc(id)
                .stream()
                .map(habitMapper::toLogResponse)
                .collect(Collectors.toList());
    }

    // ── Streak Milestones ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<HabitDto.StreakMilestone> getActiveMilestones() {
        List<Habit> habits = habitRepository.findByActiveTrue();
        List<HabitDto.StreakMilestone> milestones = new ArrayList<>();

        for (Habit h : habits) {
            int streak = h.getCurrentStreak();
            getMilestoneForStreak(streak).ifPresent(m ->
                milestones.add(HabitDto.StreakMilestone.builder()
                        .habitId(h.getId())
                        .habitName(h.getName())
                        .emoji(h.getEmoji())
                        .streakDays(streak)
                        .milestoneTitle(m[0])
                        .milestoneMessage(m[1])
                        .build())
            );
        }
        return milestones;
    }

    // ── Scheduler: Daily streak check at midnight ─────────────────────────────

    @Scheduled(cron = "0 1 0 * * *")  // 00:01 every day
    public void dailyStreakEvaluation() {
        log.info("Running daily streak evaluation...");
        habitRepository.findByActiveTrue().forEach(habit -> {
            habit.evaluateStreakBreak();
            habitRepository.save(habit);
        });
        log.info("Streak evaluation complete.");
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private Habit findOrThrow(Long id) {
        return habitRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Habit not found: " + id));
    }

    private void recomputeStreak(Habit habit) {
        List<LocalDate> dates = habitLogRepository.findCompletionDatesByHabitId(habit.getId());
        habit.setTotalCompletions(dates.size());

        if (dates.isEmpty()) {
            habit.setCurrentStreak(0);
            habit.setLastCompletedDate(null);
            return;
        }

        LocalDate latest = dates.get(dates.size() - 1);
        habit.setLastCompletedDate(latest);

        int currentStreak = 1;
        int longestStreak = 1;
        int runningStreak = 1;

        for (int i = dates.size() - 1; i > 0; i--) {
            if (dates.get(i).minusDays(1).equals(dates.get(i - 1))) {
                runningStreak++;
                longestStreak = Math.max(longestStreak, runningStreak);
            } else {
                runningStreak = 1;
            }
        }

        // Check if streak is current (last log was today or yesterday)
        LocalDate today = LocalDate.now();
        boolean isCurrentStreak = latest.equals(today) || latest.equals(today.minusDays(1));
        habit.setCurrentStreak(isCurrentStreak ? currentStreak : 0);
        habit.setLongestStreak(Math.max(habit.getLongestStreak(), longestStreak));
    }

    private Optional<String[]> getMilestoneForStreak(int streak) {
        if (streak == 7)   return Optional.of(new String[]{"🔥 One Week Warrior!", "7-day streak achieved!"});
        if (streak == 14)  return Optional.of(new String[]{"⚡ Two Week Champion!", "14 days strong!"});
        if (streak == 21)  return Optional.of(new String[]{"💎 Habit Formed!", "21 days — science says it's a habit now!"});
        if (streak == 30)  return Optional.of(new String[]{"🏆 Monthly Master!", "30-day streak — incredible!"});
        if (streak == 60)  return Optional.of(new String[]{"🚀 60 Day Legend!", "Two months of consistency!"});
        if (streak == 100) return Optional.of(new String[]{"💯 Century Club!", "100 days — you're unstoppable!"});
        if (streak == 365) return Optional.of(new String[]{"🌟 Year of Excellence!", "365 days — you're a legend!"});
        return Optional.empty();
    }
}
