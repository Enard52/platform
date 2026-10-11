/*
 *
 *                 Copyright (c) 2023-2026 Fruit Tree Labs (www.fruittreelabs.com)
 *
 *
 *     This material is part of the Dialogue Branch Platform, and is covered by the MIT License
 *                                        as outlined below.
 *
 *                                            ----------
 *
 * Copyright (c) 2023-2026 Fruit Tree Labs (www.fruittreelabs.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.dialoguebranch.web.service.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests for {@code GET /variables/history} (#259): changes made through the regular
 * variable end-points are recorded by the history listener every {@code UserService} registers,
 * and read back newest first, per user and per project.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VariableHistoryIntegrationTest {

	private static final String PROJECT = "default-test";
	private static final String ISSUER = "https://keycloak.test/realms/dialoguebranch";

	@Autowired
	MockMvc mockMvc;

	/** A fresh user per test, so tests sharing the Spring context never see each other's rows. */
	private static String newSubject() {
		return "history-" + UUID.randomUUID();
	}

	private static MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder builder,
			String subject, String... roles) {
		Consumer<Jwt.Builder> claims = jwt -> jwt
				.subject(subject)
				.claim("iss", ISSUER)
				.claim("preferred_username", subject)
				.claim("resource_access",
						Map.of("dlb-web-service", Map.of("roles", List.of(roles))));
		return builder.with(jwt().jwt(claims));
	}

	private void setSingle(String subject, String name, String value) throws Exception {
		MockHttpServletRequestBuilder request = post("/v1/variables/set-single")
				.param("projectSlug", PROJECT).param("name", name)
				.param("timeZone", "Europe/Lisbon");
		if (value != null) request.param("value", value);
		mockMvc.perform(as(request, subject, "participant")).andExpect(status().isOk());
	}

	private MockHttpServletRequestBuilder history(String subject) {
		return as(get("/v1/variables/history").param("projectSlug", PROJECT), subject,
				"participant");
	}

	@Test
	void recordsEveryChangeNewestFirstWithoutDeduplicating() throws Exception {
		String subject = newSubject();
		setSingle(subject, "mood", "fine");
		setSingle(subject, "mood", "fine");
		setSingle(subject, "mood", "bad");

		mockMvc.perform(history(subject))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andExpect(jsonPath("$[0].name").value("mood"))
				.andExpect(jsonPath("$[0].value").value("bad"))
				.andExpect(jsonPath("$[1].value").value("fine"))
				.andExpect(jsonPath("$[2].value").value("fine"))
				.andExpect(jsonPath("$[0].source").value("WEB_SERVICE"))
				.andExpect(jsonPath("$[0].recordedTimeZone").value("Europe/Lisbon"))
				.andExpect(jsonPath("$[0].recordedTime").isNumber());
	}

	@Test
	void recordsARemovalAsANullValue() throws Exception {
		String subject = newSubject();
		setSingle(subject, "mood", "fine");
		setSingle(subject, "mood", null);

		mockMvc.perform(history(subject))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].name").value("mood"))
				.andExpect(jsonPath("$[0].value").value(nullValue()))
				.andExpect(jsonPath("$[1].value").value("fine"));
	}

	@Test
	void filtersByVariableNameAndPaginates() throws Exception {
		String subject = newSubject();
		setSingle(subject, "mood", "fine");
		setSingle(subject, "energy", "low");
		setSingle(subject, "mood", "bad");

		mockMvc.perform(history(subject).param("variableName", "mood"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].value").value("bad"))
				.andExpect(jsonPath("$[1].value").value("fine"));

		mockMvc.perform(history(subject).param("page", "1").param("pageSize", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].name").value("energy"));
	}

	@Test
	void onlyReturnsTheCallersOwnHistory() throws Exception {
		String alice = newSubject();
		String bob = newSubject();
		setSingle(alice, "mood", "fine");

		mockMvc.perform(history(bob))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	void unknownProjectIsNotFound() throws Exception {
		mockMvc.perform(as(get("/v1/variables/history").param("projectSlug", "no-such-project"),
						newSubject(), "participant"))
				.andExpect(status().isNotFound());
	}

	@Test
	void invalidVariableNameIsABadRequest() throws Exception {
		mockMvc.perform(history(newSubject()).param("variableName", "1nvalid"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void readingAnotherUsersHistoryNeedsDelegation() throws Exception {
		mockMvc.perform(as(get("/v1/variables/history").param("projectSlug", PROJECT)
						.param("delegateUser", "someone-else"), newSubject(), "editor"))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("INSUFFICIENT_PRIVILEGES"));
	}

	@Test
	void withoutATokenIsUnauthorized() throws Exception {
		mockMvc.perform(get("/v1/variables/history").param("projectSlug", PROJECT))
				.andExpect(status().isUnauthorized());
	}

}
