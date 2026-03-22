package com.habittracker;

import com.habittracker.model.*;
import com.habittracker.repository.HabitLogRepository;
import com.habittracker.repository.HabitRepository;
import com.habittracker.service.HabitService;
import com.habittracker.util.HabitDto;
import com.habittracker.util.HabitMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HabitServiceTest {

    @Mock HabitRepository habitRepository;
    @Mock HabitLogRepository habitLogRepository;
    @Mock HabitMapper habitMapper;

    @InjectMocks HabitService habitService;

    private Habit sampleHabit;

    @BeforeEach
    void setUp() {
        sampleHabit = Habit.builder()
                .id(1L)
                .name("Morning Run")
                .description("5km run every morning")
                .frequency(HabitFrequency.DAILY)
                .category(HabitCategory.FITNESS)
                .emoji("🏃")
                .startDate(LocalDate.now().minusDays(10))
                .active(true)
                .build();
        sampleHabit.setCreatedAt(LocalDateTime.now());
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create habit with default values")
    void createHabit_shouldSetDefaults() {
        HabitDto.CreateHabitRequest req = HabitDto.CreateHabitRequest.builder()
                .name("Read Daily")
                .frequency(HabitFrequency.DAILY)
                .build();

        when(habitRepository.save(any())).thenReturn(sampleHabit);
        when(habitMapper.toResponse(any())).thenReturn(new HabitDto.HabitResponse());

        habitService.createHabit(req);

        ArgumentCaptor<Habit> captor = ArgumentCaptor.forClass(Habit.class);
        verify(habitRepository).save(captor.capture());
        Habit saved = captor.getValue();

        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getCurrentStreak()).isZero();
        assertThat(saved.getTotalCompletions()).isZero();
        assertThat(saved.getCategory()).isEqualTo(HabitCategory.OTHER);
        assertThat(saved.getEmoji()).isEqualTo("✅");
    }

    // ── Completion ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should complete habit and increment streak")
    void completeHabit_shouldIncrementStreak() {
        when(habitRepository.findById(1L)).thenReturn(Optional.of(sampleHabit));
        when(habitLogRepository.existsByHabitIdAndCompletedDate(any(), any())).thenReturn(false);
        when(habitRepository.save(any())).thenReturn(sampleHabit);
        when(habitMapper.toResponse(any())).thenReturn(new HabitDto.HabitResponse());

        habitService.completeHabit(1L, new HabitDto.CompleteHabitRequest());

        verify(habitLogRepository).save(any(HabitLog.class));
        verify(habitRepository).save(any(Habit.class));
        assertThat(sampleHabit.getTotalCompletions()).isEqualTo(1);
        assertThat(sampleHabit.getCurrentStreak()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should throw when completing already-completed habit")
    void completeHabit_alreadyCompleted_shouldThrow() {
        when(habitRepository.findById(1L)).thenReturn(Optional.of(sampleHabit));
        when(habitLogRepository.existsByHabitIdAndCompletedDate(any(), any())).thenReturn(true);

        assertThatThrownBy(() -> habitService.completeHabit(1L, new HabitDto.CompleteHabitRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already completed");
    }

    @Test
    @DisplayName("Should throw EntityNotFoundException for unknown habit")
    void getHabit_notFound_shouldThrow() {
        when(habitRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> habitService.getHabit(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── Streak Logic ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Streak should reset if last completion was 2+ days ago")
    void evaluateStreakBreak_shouldResetStreak() {
        sampleHabit.setCurrentStreak(5);
        sampleHabit.setLastCompletedDate(LocalDate.now().minusDays(3));

        sampleHabit.evaluateStreakBreak();

        assertThat(sampleHabit.getCurrentStreak()).isZero();
    }

    @Test
    @DisplayName("Streak should not reset if completed yesterday")
    void evaluateStreakBreak_shouldKeepStreakIfYesterday() {
        sampleHabit.setCurrentStreak(5);
        sampleHabit.setLastCompletedDate(LocalDate.now().minusDays(1));

        sampleHabit.evaluateStreakBreak();

        assertThat(sampleHabit.getCurrentStreak()).isEqualTo(5);
    }

    @Test
    @DisplayName("completeToday should not double-count same day")
    void completeToday_sameDay_shouldNotCount() {
        sampleHabit.setLastCompletedDate(LocalDate.now());
        sampleHabit.setCurrentStreak(3);
        sampleHabit.setTotalCompletions(3);

        sampleHabit.completeToday();

        assertThat(sampleHabit.getCurrentStreak()).isEqualTo(3); // unchanged
        assertThat(sampleHabit.getTotalCompletions()).isEqualTo(3); // unchanged
    }

    @Test
    @DisplayName("completeToday should set longestStreak when currentStreak exceeds it")
    void completeToday_shouldUpdateLongestStreak() {
        sampleHabit.setLastCompletedDate(LocalDate.now().minusDays(1));
        sampleHabit.setCurrentStreak(9);
        sampleHabit.setLongestStreak(9);

        sampleHabit.completeToday();

        assertThat(sampleHabit.getCurrentStreak()).isEqualTo(10);
        assertThat(sampleHabit.getLongestStreak()).isEqualTo(10);
    }

    // ── Completion Rate ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Completion rate should calculate correctly")
    void completionRate_shouldCalculateCorrectly() {
        sampleHabit.setStartDate(LocalDate.now().minusDays(9)); // 10 days including today
        sampleHabit.setTotalCompletions(5);

        double rate = sampleHabit.getCompletionRate();

        assertThat(rate).isEqualTo(50.0);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Delete should call repository when habit exists")
    void deleteHabit_shouldDelete() {
        when(habitRepository.existsById(1L)).thenReturn(true);

        habitService.deleteHabit(1L);

        verify(habitRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Delete should throw when habit not found")
    void deleteHabit_notFound_shouldThrow() {
        when(habitRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> habitService.deleteHabit(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── Get All ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("getAllHabits with activeOnly=true should use active query")
    void getAllHabits_activeOnly() {
        when(habitRepository.findAllActiveOrderedByStreak()).thenReturn(List.of(sampleHabit));
        when(habitMapper.toResponse(any())).thenReturn(new HabitDto.HabitResponse());

        List<HabitDto.HabitResponse> result = habitService.getAllHabits(true);

        assertThat(result).hasSize(1);
        verify(habitRepository).findAllActiveOrderedByStreak();
        verify(habitRepository, never()).findAll();
    }
}
