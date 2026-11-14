package com.example

import com.example.api.ApiBaseUrlType
import com.example.api.GeminiApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiConfigUnitTest {

    @Test
    fun `resolveEndpointUrl handles WITH_V1 OpenAI style base urls`() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            GeminiApiClient.resolveEndpointUrl("https://api.openai.com/v1", ApiBaseUrlType.WITH_V1)
        )
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            GeminiApiClient.resolveEndpointUrl("https://api.openai.com", ApiBaseUrlType.WITH_V1)
        )
        assertEquals(
            "https://openrouter.ai/api/v1/chat/completions",
            GeminiApiClient.resolveEndpointUrl("https://openrouter.ai", ApiBaseUrlType.WITH_V1)
        )
        assertEquals(
            "https://openrouter.ai/api/v1/chat/completions",
            GeminiApiClient.resolveEndpointUrl("https://openrouter.ai/api/v1", ApiBaseUrlType.WITH_V1)
        )
    }

    @Test
    fun `resolveEndpointUrl handles WITHOUT_V1 Anthropic style base urls`() {
        assertEquals(
            "https://api.anthropic.com/v1/messages",
            GeminiApiClient.resolveEndpointUrl("https://api.anthropic.com", ApiBaseUrlType.WITHOUT_V1)
        )
        assertEquals(
            "https://api.anthropic.com/v1/messages",
            GeminiApiClient.resolveEndpointUrl("https://api.anthropic.com/v1/", ApiBaseUrlType.WITHOUT_V1)
        )
        assertEquals(
            "https://proxy.example.com/v1/messages",
            GeminiApiClient.resolveEndpointUrl("https://proxy.example.com/v1/messages", ApiBaseUrlType.WITHOUT_V1)
        )
    }

    @Test
    fun `resolveEndpointUrl and validation support https and http localhost and 127_0_0_1`() {
        assertEquals(
            "https://localhost:8080/v1/chat/completions",
            GeminiApiClient.resolveEndpointUrl("https://localhost:8080/v1", ApiBaseUrlType.WITH_V1)
        )
        assertEquals(
            "https://127.0.0.1:8443/v1/messages",
            GeminiApiClient.resolveEndpointUrl("https://127.0.0.1:8443", ApiBaseUrlType.WITHOUT_V1)
        )
        assertEquals(
            "http://127.0.0.1:11434/v1/chat/completions",
            GeminiApiClient.resolveEndpointUrl("127.0.0.1:11434", ApiBaseUrlType.WITH_V1)
        )
        assertEquals(
            "http://localhost:3000/v1/messages",
            GeminiApiClient.resolveEndpointUrl("localhost:3000", ApiBaseUrlType.WITHOUT_V1)
        )

        assertTrue(GeminiApiClient.isValidBaseUrlInput("https://localhost:8080"))
        assertTrue(GeminiApiClient.isValidBaseUrlInput("https://127.0.0.1:8080/v1"))
        assertTrue(GeminiApiClient.isValidBaseUrlInput("http://127.0.0.1:11434"))
        assertTrue(GeminiApiClient.isValidBaseUrlInput("localhost:3000"))
        assertTrue(GeminiApiClient.isLoopbackHost("localhost"))
        assertTrue(GeminiApiClient.isLoopbackHost("127.0.0.1"))
        assertFalse(GeminiApiClient.isLoopbackHost("api.openai.com"))
    }

    @Test
    fun `normalizeModel preserves provider models and defaults when blank`() {
        assertEquals(
            "claude-3-5-sonnet-latest",
            GeminiApiClient.normalizeModel("", "https://api.anthropic.com", ApiBaseUrlType.WITHOUT_V1)
        )
        assertEquals(
            "gpt-4o-mini",
            GeminiApiClient.normalizeModel("", "https://api.openai.com/v1", ApiBaseUrlType.WITH_V1)
        )
        assertEquals(
            "google/gemini-2.5-flash",
            GeminiApiClient.normalizeModel("gemini-2.5-flash", "https://openrouter.ai/api/v1", ApiBaseUrlType.WITH_V1)
        )
    }

    @Test
    fun `ApiBaseUrlType fromKey supports new and legacy keys`() {
        assertEquals(ApiBaseUrlType.WITH_V1, ApiBaseUrlType.fromKey("with_v1"))
        assertEquals(ApiBaseUrlType.WITHOUT_V1, ApiBaseUrlType.fromKey("without_v1"))
        assertEquals(ApiBaseUrlType.WITHOUT_V1, ApiBaseUrlType.fromKey("antropic"))
        assertEquals(ApiBaseUrlType.WITH_V1, ApiBaseUrlType.fromKey("chat_id"))
        assertEquals(ApiBaseUrlType.WITH_V1, ApiBaseUrlType.fromKey("response"))
    }
}
