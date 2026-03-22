package com.habittracker.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "habit_logs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"habit_id", "completedDate"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HabitLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    private Habit habit;

    @Column(nullable = false)
    private LocalDate completedDate;

    private String note;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private MoodRating mood = MoodRating.NEUTRAL;

    @Column(nullable = false)
    private LocalDateTime loggedAt;

    @PrePersist
    protected void onCreate() {
        loggedAt = LocalDateTime.now();
        if (completedDate == null) completedDate = LocalDate.now();
    }
}
