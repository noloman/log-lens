package me.manulorenzo.loglens.api.config

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

@ExtendWith(MockitoExtension::class)
class RateLimitFilterTest {
    private lateinit var rateLimitFilter: RateLimitFilter

    @Mock
    private lateinit var filterChain: FilterChain

    @BeforeEach
    fun setUp() {
        rateLimitFilter = RateLimitFilter()
    }

    @Test
    fun `should allow request when under rate limit`() {
        val request = MockHttpServletRequest()
        request.remoteAddr = "192.168.1.1"
        val response = MockHttpServletResponse()

        rateLimitFilter.doFilter(request, response, filterChain)

        verify(filterChain).doFilter(request, response)
        assertEquals(HttpStatus.OK.value(), response.status)
    }

    @Test
    fun `should return 429 when rate limit exceeded`() {
        val request = MockHttpServletRequest()
        request.remoteAddr = "10.0.0.1"
        val response = MockHttpServletResponse()

        // Exhaust the 100-request limit
        repeat(100) {
            val r = MockHttpServletResponse()
            rateLimitFilter.doFilter(request, r, filterChain)
        }

        // 101st request should be rejected
        rateLimitFilter.doFilter(request, response, filterChain)

        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response.status)
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.contentType)
        assertEquals("60", response.getHeader("Retry-After"))
        assertEquals("""{"error":"Rate limit exceeded"}""", response.contentAsString)
    }

    @Test
    fun `should not call filter chain when rate limit exceeded`() {
        val request = MockHttpServletRequest()
        request.remoteAddr = "10.0.0.2"
        val blockedResponse = MockHttpServletResponse()

        repeat(100) {
            rateLimitFilter.doFilter(request, MockHttpServletResponse(), filterChain)
        }

        val blockedChain = org.mockito.Mockito.mock(FilterChain::class.java)
        rateLimitFilter.doFilter(request, blockedResponse, blockedChain)

        verify(blockedChain, never()).doFilter(request, blockedResponse)
    }

    @Test
    fun `should track rate limits per IP address`() {
        val request1 = MockHttpServletRequest()
        request1.remoteAddr = "10.0.0.10"
        val request2 = MockHttpServletRequest()
        request2.remoteAddr = "10.0.0.11"

        // Exhaust limit for IP 1
        repeat(100) {
            rateLimitFilter.doFilter(request1, MockHttpServletResponse(), filterChain)
        }

        // IP 1 should be blocked
        val response1 = MockHttpServletResponse()
        rateLimitFilter.doFilter(request1, response1, filterChain)
        assertEquals(HttpStatus.TOO_MANY_REQUESTS.value(), response1.status)

        // IP 2 should still be allowed
        val response2 = MockHttpServletResponse()
        rateLimitFilter.doFilter(request2, response2, filterChain)
        assertEquals(HttpStatus.OK.value(), response2.status)
    }
}
