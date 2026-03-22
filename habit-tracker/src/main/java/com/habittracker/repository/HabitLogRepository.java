package com.habittracker.repository;

import com.habittracker.model.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {

    List<HabitLog> findByHabitIdOrderByCompletedDateDesc(Long habitId);

    Optional<HabitLog> findByHabitIdAndCompletedDate(Long habitId, LocalDate date);

    boolean existsByHabitIdAndCompletedDate(Long habitId, LocalDate date);

    @Query("SELECT hl FROM HabitLog hl WHERE hl.habit.id = :habitId AND hl.completedDate BETWEEN :from AND :to ORDER BY hl.completedDate ASC")
    List<HabitLog> findByHabitIdAndDateRange(@Param("habitId") Long habitId,
                                             @Param("from") LocalDate from,
                                             @Param("to") LocalDate to);

    @Query("SELECT hl FROM HabitLog hl WHERE hl.completedDate = :date")
    List<HabitLog> findAllByDate(@Param("date") LocalDate date);

    @Query("SELECT COUNT(hl) FROM HabitLog hl WHERE hl.habit.id = :habitId AND hl.completedDate >= :since")
    long countCompletionsSince(@Param("habitId") Long habitId, @Param("since") LocalDate since);

    @Query("SELECT hl.completedDate FROM HabitLog hl WHERE hl.habit.id = :habitId ORDER BY hl.completedDate ASC")
    List<LocalDate> findCompletionDatesByHabitId(@Param("habitId") Long habitId);
}
