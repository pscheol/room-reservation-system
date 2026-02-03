package com.devpaik.metting.application.reservation.config

import com.devpaik.metting.domain.reservation.service.*
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class ReservationConfig {

    @Bean
    fun roomAvailabilityRule() = RoomAvailabilityRule()

    @Bean
    fun roomCapacityRule() = RoomCapacityRule()

    @Bean
    fun businessHoursRule() = BusinessHoursRule()

    @Bean
    fun reservationDateRule() = ReservationDateRule()


    @Bean
    fun reservationValidator(
        availabilityRules: List<AvailabilityValidationRule>
    ): ReservationValidator {
        return ReservationValidator(availabilityRules)
    }

    @Bean
    fun reservationAvailabilityValidator(
        reservationValidator: ReservationValidator
    ): ReservationAvailabilityValidator {
        return reservationValidator
    }

    @Bean
    fun reservationConflictValidator(
        reservationValidator: ReservationValidator
    ): ReservationConflictValidator {
        return reservationValidator
    }

    @Bean
    fun reservationFactory(
        reservationAvailabilityValidator: ReservationAvailabilityValidator,
        reservationConflictValidator: ReservationConflictValidator
    ): ReservationFactory {
        return ReservationFactory(reservationAvailabilityValidator, reservationConflictValidator)
    }

    @Bean
    fun reservationUpdater(
        reservationConflictValidator: ReservationConflictValidator
    ): ReservationUpdater {
        return ReservationUpdater(reservationConflictValidator)
    }
}