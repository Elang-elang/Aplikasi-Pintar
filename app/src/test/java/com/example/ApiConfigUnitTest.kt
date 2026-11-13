package com.example

import com.example.api.ApiBaseUrlType
import com.example.api.GeminiApiClient
import org.junit.Assert.assertEquals
import org.junit.Test

class ApiConfigUnitTest {

    @Test
    fun `resolveEndpointUrl handles antropic base urls`() {
        assertEquals(
            "https://api.anthropic.com/v1/messages",
            GeminiApiClient.resolveEndpointUrl("https://api.anthropic.com", ApiBaseUrlType.ANTROPIC)
        )
        assertEquals(
            "https://api.anthropic.com/v1/messages",
            GeminiApiClient.resolveEndpointUrl("https://api.anthropic.com/v1/", ApiBaseUrlType.ANTROPIC)
        )
        assertEquals(
            "https://proxy.example.com/v1/messages",
            GeminiApiClient.resolveEndpointUrl("https://proxy.example.com/v1/messages", ApiBaseUrlType.ANTROPIC)
        )
    }

    @Test
    fun `resolveEndpointUrl handles chat_id base urls`() {
        assertEquals(
            "https://openrouter.ai/api/v1/chat/completions",
            GeminiApiClient.resolveEndpointUrl("https://openrouter.ai", ApiBaseUrlType.CHAT_ID)
        )
        assertEquals(
            "https://openrouter.ai/api/v1/chat/completions",
            GeminiApiClient.resolveEndpointUrl("https://openrouter.ai/api/v1", ApiBaseUrlType.CHAT_ID)
        )
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            GeminiApiClient.resolveEndpointUrl("https://api.openai.com", ApiBaseUrlType.CHAT_ID)
        )
    }

    @Test
    fun `resolveEndpointUrl handles response base urls`() {
        assertEquals(
            "https://api.openai.com/v1/responses",
            GeminiApiClient.resolveEndpointUrl("https://api.openai.com", ApiBaseUrlType.RESPONSE)
        )
        assertEquals(
            "https://api.openai.com/v1/responses",
            GeminiApiClient.resolveEndpointUrl("https://api.openai.com/v1/", ApiBaseUrlType.RESPONSE)
        )
        assertEquals(
            "https://custom.api.dev/v1/responses",
            GeminiApiClient.resolveEndpointUrl("https://custom.api.dev/v1/responses", ApiBaseUrlType.RESPONSE)
        )
    }

    @Test
    fun `normalizeModel preserves provider models and defaults when blank`() {
        assertEquals(
            "claude-3-5-sonnet-latest",
            GeminiApiClient.normalizeModel("", "https://api.anthropic.com", ApiBaseUrlType.ANTROPIC)
        )
        assertEquals(
            "gpt-4o-mini",
            GeminiApiClient.normalizeModel("gpt-4o-mini", "https://api.openai.com", ApiBaseUrlType.RESPONSE)
        )
        assertEquals(
            "google/gemini-2.5-flash",
            GeminiApiClient.normalizeModel("gemini-2.5-flash", "https://openrouter.ai/api/v1", ApiBaseUrlType.CHAT_ID)
        )
    }

    @Test
    fun `ApiBaseUrlType fromKey maps all supported types`() {
        assertEquals(ApiBaseUrlType.ANTROPIC, ApiBaseUrlType.fromKey("antropic"))
        assertEquals(ApiBaseUrlType.CHAT_ID, ApiBaseUrlType.fromKey("chat_id"))
        assertEquals(ApiBaseUrlType.RESPONSE, ApiBaseUrlType.fromKey("response"))
    }
}
