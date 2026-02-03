package com.devpaik.metting.application.room.config

import com.devpaik.metting.domain.room.service.RoomMatcher
import com.devpaik.metting.domain.room.service.RoomMatcherService
import com.devpaik.metting.domain.room.service.strategy.OptimalCapacityMatchingStrategy
import com.devpaik.metting.domain.room.service.strategy.RoomMatchingStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfig {

    @Bean
    fun roomMatchingStrategy(): RoomMatchingStrategy {
        return OptimalCapacityMatchingStrategy()
    }

    @Bean
    fun roomMatcher(
        roomMatchingStrategy: RoomMatchingStrategy
    ): RoomMatcher {
        return RoomMatcherService(roomMatchingStrategy)
    }
}