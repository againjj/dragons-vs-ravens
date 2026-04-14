package com.dragonsvsravens.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.support.StaticListableBeanFactory
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.user.DefaultOAuth2User

@SpringBootTest
@AutoConfigureMockMvc
class OAuthSupportTest {
    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var oauthLoginSuccessHandler: OAuthLoginSuccessHandler

    @BeforeEach
    fun resetUsers() {
        jdbcTemplate.update("delete from user_identities")
        jdbcTemplate.update("delete from users")
    }

    @Test
    fun `provider catalog lists configured oauth registrations`() {
        val repository = InMemoryClientRegistrationRepository(googleRegistration(), githubRegistration())
        val beanFactory = StaticListableBeanFactory().apply {
            addBean("clientRegistrationRepository", repository)
        }
        val catalog = OAuthProviderCatalog(beanFactory.getBeanProvider(ClientRegistrationRepository::class.java))

        assertEquals(listOf("github", "google"), catalog.availableProviders())
    }

    @Test
    fun `provider catalog returns empty when no oauth client registration repository exists`() {
        val catalog = OAuthProviderCatalog(StaticListableBeanFactory().getBeanProvider(ClientRegistrationRepository::class.java))

        assertEquals(emptyList<String>(), catalog.availableProviders())
    }

    @Test
    fun `authorization request resolver stores a safe next path in session`() {
        val resolver = NextAwareAuthorizationRequestResolver(InMemoryClientRegistrationRepository(googleRegistration()))
        val request = MockHttpServletRequest("GET", "/oauth2/authorization/google").apply {
            setParameter("next", "/g/CFGHJMP")
        }

        resolver.resolve(request, "google")

        assertEquals("/g/CFGHJMP", request.getSession(false)?.getAttribute(OAuthLoginSuccessHandler.oauthNextPathSessionAttribute))
    }

    @Test
    fun `authorization request resolver ignores an unsafe next path`() {
        val resolver = NextAwareAuthorizationRequestResolver(InMemoryClientRegistrationRepository(googleRegistration()))
        val request = MockHttpServletRequest("GET", "/oauth2/authorization/google").apply {
            setParameter("next", "https://evil.example")
        }

        resolver.resolve(request, "google")

        assertNull(request.getSession(false)?.getAttribute(OAuthLoginSuccessHandler.oauthNextPathSessionAttribute))
    }

    @Test
    fun `oauth login success handler redirects to the stored next path`() {
        val request = MockHttpServletRequest("GET", "/login/oauth2/code/google")
        val response = MockHttpServletResponse()
        request.getSession(true)!!.setAttribute(OAuthLoginSuccessHandler.oauthNextPathSessionAttribute, "/g/CFGHJMP")

        oauthLoginSuccessHandler.onAuthenticationSuccess(request, response, googleAuthentication())

        assertEquals("/g/CFGHJMP", response.redirectedUrl)
        assertNull(request.getSession(false)?.getAttribute(OAuthLoginSuccessHandler.oauthNextPathSessionAttribute))
    }

    @Test
    fun `oauth login success handler falls back to root when no next path was stored`() {
        val request = MockHttpServletRequest("GET", "/login/oauth2/code/google")
        val response = MockHttpServletResponse()

        oauthLoginSuccessHandler.onAuthenticationSuccess(request, response, googleAuthentication())

        assertEquals("/", response.redirectedUrl)
    }

    private fun googleAuthentication(): OAuth2AuthenticationToken {
        val principal = DefaultOAuth2User(
            listOf(SimpleGrantedAuthority("ROLE_USER")),
            mapOf(
                "sub" to "google-subject",
                "email" to "player@example.com",
                "name" to "Dragon Player"
            ),
            "sub"
        )
        return OAuth2AuthenticationToken(principal, principal.authorities, "google")
    }

    private fun googleRegistration(): ClientRegistration =
        registration("google")

    private fun githubRegistration(): ClientRegistration =
        registration("github")

    private fun registration(registrationId: String): ClientRegistration =
        ClientRegistration.withRegistrationId(registrationId)
            .clientId("client-id")
            .clientSecret("client-secret")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .authorizationUri("https://example.com/oauth2/authorize")
            .tokenUri("https://example.com/oauth2/token")
            .userInfoUri("https://example.com/userinfo")
            .userNameAttributeName("sub")
            .clientName(registrationId)
            .build()
}
