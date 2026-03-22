package com.habittracker.repository;

import com.habittracker.model.Habit;
import com.habittracker.model.HabitCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findByActiveTrue();

    List<Habit> findByActiveTrueAndCategory(HabitCategory category);

    @Query("SELECT h FROM Habit h WHERE h.active = true AND h.currentStreak >= :minStreak ORDER BY h.currentStreak DESC")
    List<Habit> findActiveHabitsWithMinStreak(@Param("minStreak") int minStreak);

    @Query("SELECT h FROM Habit h WHERE h.active = true AND h.lastCompletedDate = :date")
    List<Habit> findCompletedOnDate(@Param("date") LocalDate date);

    @Query("SELECT h FROM Habit h WHERE h.active = true AND (h.lastCompletedDate IS NULL OR h.lastCompletedDate < :today)")
    List<Habit> findIncompleteToday(@Param("today") LocalDate today);

    @Query("SELECT h FROM Habit h WHERE h.active = true ORDER BY h.currentStreak DESC")
    List<Habit> findAllActiveOrderedByStreak();

    @Query("SELECT COUNT(h) FROM Habit h WHERE h.active = true AND h.lastCompletedDate = :today")
    long countCompletedToday(@Param("today") LocalDate today);

    @Query("SELECT COUNT(h) FROM Habit h WHERE h.active = true")
    long countActiveHabits();
}
