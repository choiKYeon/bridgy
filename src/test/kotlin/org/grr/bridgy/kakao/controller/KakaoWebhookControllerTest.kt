package org.grr.bridgy.kakao.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.grr.bridgy.kakao.dto.*
import org.grr.bridgy.kakao.service.KakaoChannelService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.bean.MockBean
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(KakaoWebhookController::class)
@DisplayName("KakaoWebhookController API 테스트")
class KakaoWebhookControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @MockBean
    lateinit var kakaoChannelService: KakaoChannelService

    private val objectMapper = ObjectMapper()

    @Nested
    @DisplayName("POST /api/kakao/webhook/{storeId}")
    inner class HandleWebhook {

        @Test
        @WithMockUser
        @DisplayName("카카오톡 웹훅 메시지를 처리하면 200 OK와 스킬 응답을 반환한다")
        fun `should return 200 with kakao skill response`() {
            // given
            val request = KakaoWebhookRequest(
                userRequest = KakaoUserRequest(
                    user = KakaoUser(id = "kakao_user_001"),
                    utterance = "영업시간 알려주세요"
                )
            )
            whenever(kakaoChannelService.handleMessage(eq(1L), any()))
                .thenReturn(kakaoTextResponse("안녕하세요, 맛있는 카페입니다! 영업시간은 09:00~22:00입니다."))

            // when & then
            mockMvc.perform(
                post("/api/kakao/webhook/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.version").value("2.0"))
                .andExpect(jsonPath("$.template.outputs[0].simpleText.text")
                    .value("안녕하세요, 맛있는 카페입니다! 영업시간은 09:00~22:00입니다."))
        }

        @Test
        @WithMockUser
        @DisplayName("예약 문의 메시지를 처리한다")
        fun `should handle reservation inquiry`() {
            // given
            val request = KakaoWebhookRequest(
                userRequest = KakaoUserRequest(
                    user = KakaoUser(id = "kakao_user_002"),
                    utterance = "예약하고 싶어요"
                )
            )
            whenever(kakaoChannelService.handleMessage(eq(1L), any()))
                .thenReturn(kakaoTextResponse("예약을 도와드리겠습니다. 날짜, 시간, 인원수를 알려주세요!"))

            // when & then
            mockMvc.perform(
                post("/api/kakao/webhook/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.template.outputs[0].simpleText.text")
                    .value("예약을 도와드리겠습니다. 날짜, 시간, 인원수를 알려주세요!"))
        }

        @Test
        @WithMockUser
        @DisplayName("매장이 없을 때 에러 메시지를 반환한다")
        fun `should return error message when store not found`() {
            // given
            val request = KakaoWebhookRequest(
                userRequest = KakaoUserRequest(
                    user = KakaoUser(id = "kakao_user_003"),
                    utterance = "안녕하세요"
                )
            )
            whenever(kakaoChannelService.handleMessage(eq(999L), any()))
                .thenReturn(kakaoTextResponse("죄송합니다. 매장 정보를 찾을 수 없습니다."))

            // when & then
            mockMvc.perform(
                post("/api/kakao/webhook/999")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.template.outputs[0].simpleText.text")
                    .value("죄송합니다. 매장 정보를 찾을 수 없습니다."))
        }

        @Test
        @WithMockUser
        @DisplayName("응답이 카카오 스킬 v2.0 포맷을 준수한다")
        fun `should comply with kakao skill v2 format`() {
            // given
            val request = KakaoWebhookRequest(
                userRequest = KakaoUserRequest(
                    user = KakaoUser(id = "test_user"),
                    utterance = "테스트"
                )
            )
            whenever(kakaoChannelService.handleMessage(eq(1L), any()))
                .thenReturn(kakaoTextResponse("테스트 응답"))

            // when & then
            mockMvc.perform(
                post("/api/kakao/webhook/1")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.version").value("2.0"))
                .andExpect(jsonPath("$.template").exists())
                .andExpect(jsonPath("$.template.outputs").isArray)
                .andExpect(jsonPath("$.template.outputs.length()").value(1))
                .andExpect(jsonPath("$.template.outputs[0].simpleText").exists())
                .andExpect(jsonPath("$.template.outputs[0].simpleText.text").isString)
        }
    }

    @Nested
    @DisplayName("GET /api/kakao/webhook/health")
    inner class HealthCheck {

        @Test
        @WithMockUser
        @DisplayName("헬스체크 요청에 200 OK와 status ok를 반환한다")
        fun `should return 200 with status ok`() {
            // when & then
            mockMvc.perform(get("/api/kakao/webhook/health"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.status").value("ok"))
        }
    }
}
