package com.habittracker.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "habits")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Habit name cannot be blank")
    @Column(nullable = false)
    private String name;

    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HabitFrequency frequency;

    @Enumerated(EnumType.STRING)
    private HabitCategory category;

    private String emoji;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // Streak tracking
    @Column(nullable = false)
    @Builder.Default
    private int currentStreak = 0;

    @Column(nullable = false)
    @Builder.Default
    private int longestStreak = 0;

    @Column(nullable = false)
    @Builder.Default
    private int totalCompletions = 0;

    private LocalDate lastCompletedDate;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "habit", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<HabitLog> logs = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (startDate == null) startDate = LocalDate.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Domain Methods ──────────────────────────────────────────────────────────

    /**
     * Mark the habit as completed today. Updates streak and total counts.
     */
    public void completeToday() {
        LocalDate today = LocalDate.now();

        if (lastCompletedDate != null && lastCompletedDate.equals(today)) {
            return; // Already completed today
        }

        totalCompletions++;

        if (lastCompletedDate == null || lastCompletedDate.equals(today.minusDays(1))) {
            currentStreak++;
        } else if (!lastCompletedDate.equals(today)) {
            currentStreak = 1; // Streak broken, restart
        }

        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
        }

        lastCompletedDate = today;
    }

    /**
     * Check if the streak has been broken (missed yesterday and not yet done today).
     */
    public void evaluateStreakBreak() {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        if (lastCompletedDate != null
                && !lastCompletedDate.equals(today)
                && !lastCompletedDate.equals(yesterday)) {
            currentStreak = 0;
        }
    }

    /**
     * Returns true if the habit was completed today.
     */
    public boolean isCompletedToday() {
        return LocalDate.now().equals(lastCompletedDate);
    }

    /**
     * Completion rate as a percentage over all days since start.
     */
    public double getCompletionRate() {
        long daysSinceStart = startDate.until(LocalDate.now()).getDays() + 1;
        if (daysSinceStart <= 0) return 0;
        return Math.min(100.0, (totalCompletions * 100.0) / daysSinceStart);
    }
}
