package com.devpaik.metting.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("회의실 예약 시스템 API")
                    .description("""
                        ## 회의실 예약 시스템 API
                        
                        ### 주요 기능
                        1. **예약 관리**: 회의실 예약 생성, 수정, 취소, 조회
                        2. **회의실 관리**: 회의실 등록, 수정, 삭제, 조회
                        3. **가용성 확인**: 시간대별 회의실 예약 가능 여부 확인
                        4. **참여자 관리**: 예약 참여자 추가 및 관리
        
                    """.trimIndent())
                    .version("1.0.0")
            )
    }
}