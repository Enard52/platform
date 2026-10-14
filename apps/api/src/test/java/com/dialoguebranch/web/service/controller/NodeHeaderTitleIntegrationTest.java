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
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests that a draft node's title only changes by renaming it: {@code /create-node} and {@code
 * /update-node} refuse a header whose {@code title} isn't the node's title (#343).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NodeHeaderTitleIntegrationTest {

	private static final String ISSUER = "https://keycloak.test/realms/dialoguebranch";

	@Autowired
	MockMvc mockMvc;

	private static MockHttpServletRequestBuilder asAdmin(MockHttpServletRequestBuilder builder) {
		Consumer<Jwt.Builder> claims = jwt -> jwt
				.subject("header-title-admin")
				.claim("iss", ISSUER)
				.claim("preferred_username", "header-title-admin")
				.claim("resource_access",
						Map.of("dlb-web-service", Map.of("roles", List.of("admin"))));
		return builder.with(jwt().jwt(claims));
	}

	private String newProjectWithDialogue() throws Exception {
		String slug = "header-title-" + UUID.randomUUID().toString().substring(0, 8);
		mockMvc.perform(asAdmin(post("/v1/project/create-project")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"slug\":\"" + slug + "\",\"displayName\":\"T\",\"description\":\"\","
								+ "\"sourceLanguageCode\":\"en\",\"sourceLanguageName\":\"English\"}")))
				.andExpect(status().isCreated());
		mockMvc.perform(asAdmin(post("/v1/authoring/create-dialogue").param("projectSlug", slug)
						.contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"main\"}")))
				.andExpect(status().isCreated());
		return slug;
	}

	private static String node(String title, String header) {
		return "{\"title\":\"" + title + "\",\"header\":\"" + header + "\",\"body\":\"Hi.\"}";
	}

	private MockHttpServletRequestBuilder createNode(String slug, String title, String header) {
		return asAdmin(post("/v1/authoring/create-node").param("projectSlug", slug)
				.param("dialogueName", "main").contentType(MediaType.APPLICATION_JSON)
				.content(node(title, header)));
	}

	private MockHttpServletRequestBuilder updateNode(String slug, String title, String header) {
		return asAdmin(post("/v1/authoring/update-node").param("projectSlug", slug)
				.param("dialogueName", "main").param("nodeTitle", title)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"header\":\"" + header + "\",\"body\":\"Hi.\"}"));
	}

	@Test
	void createNodeAcceptsAMatchingTitleOnly() throws Exception {
		String slug = newProjectWithDialogue();
		mockMvc.perform(createNode(slug, "Start", "title: Start\\nposition: 0,0"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Start"));
		mockMvc.perform(createNode(slug, "Other", "title: Different"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message", containsString("/rename-node")));
		mockMvc.perform(createNode(slug, "Other", "speaker: Robin"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updateNodeRefusesAChangedOrMissingTitle() throws Exception {
		String slug = newProjectWithDialogue();
		mockMvc.perform(createNode(slug, "Start", "title: Start")).andExpect(status().isCreated());

		mockMvc.perform(updateNode(slug, "Start", "title: Start\\nposition: 10,20"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.header").value("title: Start\nposition: 10,20"));
		mockMvc.perform(updateNode(slug, "Start", "title: Renamed"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(updateNode(slug, "Start", "position: 10,20"))
				.andExpect(status().isBadRequest());
	}
}
