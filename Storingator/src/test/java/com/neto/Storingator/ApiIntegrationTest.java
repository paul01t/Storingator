package com.neto.Storingator;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "app.rate-limit.per-minute=100000",
        "app.security.require-https=false"
})
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    private String uniq(String prefix) {
        return prefix + ThreadLocalRandom.current().nextInt(1_000_000);
    }

    private String json(Map<String, Object> object) {
        StringBuilder sb = new StringBuilder("{");
        int i = 0;
        for (Map.Entry<String, Object> entry : object.entrySet()) {
            if (i++ > 0) sb.append(',');
            sb.append('"').append(entry.getKey()).append("\":");
            sb.append(render(entry.getValue()));
        }
        return sb.append('}').toString();
    }

    private String render(Object value) {
        if (value instanceof Map) {
            return json((Map<String, Object>) value);
        }
        if (value instanceof Boolean || value instanceof Number) {
            return String.valueOf(value);
        }
        String s = value == null ? "" : value.toString();
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private void register(String username, String email, String password) throws Exception {
        mvc.perform(post("/api/v1/User/New")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "email", email,
                                "clearPassword", password))))
                .andExpect(status().isCreated());
    }

    private String signin(String username, String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/v1/User/Signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "clearPassword", password))))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.loginkey");
    }

private String createVault(String token, String platform, String password, String passphrase,
                               boolean encryptOnDevice) throws Exception {
        mvc.perform(post("/api/v1/Data/New")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", passphrase,
                                "tmpLoginData", Map.of(
                                        "platform", platform,
                                        "username", "acc1",
                                        "email", "acc1@example.org",
                                        "password", password,
                                        "encryptOnDevice", encryptOnDevice)))))
                .andExpect(status().isOk());

        MvcResult list = mvc.perform(post("/api/v1/Data/GetLoginData")
                        .header("Authorization", "Bearer " + token))
                .andReturn();
        String content = list.getResponse().getContentAsString();
        List<Map<String, Object>> entries = JsonPath.read(content, "$");
        for (Map<String, Object> entry : entries) {
            if (platform.equals(entry.get("platform"))) {
                return (String) entry.get("seq");
            }
        }
        return null;
    }

    @Test
    void healthIsPublic() throws Exception {
        mvc.perform(get("/health")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointsRequireAuth() throws Exception {
        mvc.perform(get("/api/v1/User/Me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/Data/New")
                        .header("Authorization", "Bearer invalid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void localOnlyEndpointsDeniedForNonLocalhost() throws Exception {
        mvc.perform(post("/api/v1/User/New")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.5");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", uniq("remote"),
                                "email", "remote@x.com",
                                "clearPassword", "pass123"))))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/User/Delete")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.5");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("password", "pass123"))))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/User/ChangePassword")
                        .with(request -> {
                            request.setRemoteAddr("203.0.113.5");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("currentPassword", "a", "newPassword", "b"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateUsernameRejected() throws Exception {
        String username = uniq("dup");
        register(username, uniq("e") + "@x.com", "pass123");
        mvc.perform(post("/api/v1/User/New")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "email", uniq("f") + "@x.com",
                                "clearPassword", "pass123"))))
                .andExpect(status().isConflict());
    }

    @Test
    void wrongPasswordFails() throws Exception {
        String username = uniq("wp");
        register(username, uniq("g") + "@x.com", "correct");
        mvc.perform(post("/api/v1/User/Signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "clearPassword", "wrong"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void meReturnsCurrentUser() throws Exception {
        String username = uniq("me");
        register(username, username + "@x.com", "pass123");
        String token = signin(username, "pass123");
        mvc.perform(get("/api/v1/User/Me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username));
    }

    @Test
    void serverEncryptedVaultIsMaskedAndDecrypts() throws Exception {
        String username = uniq("sec");
        register(username, username + "@x.com", "pass123");
        String token = signin(username, "pass123");

        String seq = createVault(token, "github-secret", "mySecretPw", "master", false);

        mvc.perform(post("/api/v1/Data/GetLoginData").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").value("*************"));

        mvc.perform(post("/api/v1/Data/GetClearPassword")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", "master",
                                "tmpLoginData", Map.of("seq", seq)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").value("mySecretPw"));
    }

    @Test
    void deviceEncryptedVaultReturnsRawPassword() throws Exception {
        String username = uniq("dev");
        register(username, username + "@x.com", "pass123");
        String token = signin(username, "pass123");

        String seq = createVault(token, "github-dev", "alreadyEncryptedClient", "unused", true);

        mvc.perform(post("/api/v1/Data/GetLoginData").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].password").value("alreadyEncryptedClient"))
                .andExpect(jsonPath("$[0].encryptOnDevice").value(true));

        mvc.perform(post("/api/v1/Data/GetClearPassword")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", "unused",
                                "tmpLoginData", Map.of("seq", seq)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").value("alreadyEncryptedClient"));
    }

    @Test
    void updateChangesVaultEntry() throws Exception {
        String username = uniq("upd");
        register(username, username + "@x.com", "pass123");
        String token = signin(username, "pass123");

        String seq = createVault(token, "github", "oldPass", "master", false);

        mvc.perform(post("/api/v1/Data/Update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", "master",
                                "tmpLoginData", Map.of(
                                        "seq", seq,
                                        "platform", "gitlab",
                                        "username", "newacc",
                                        "password", "newPass")))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/Data/GetLoginData").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].platform").value("gitlab"))
                .andExpect(jsonPath("$[0].password").value("*************"));

        mvc.perform(post("/api/v1/Data/GetClearPassword")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", "master",
                                "tmpLoginData", Map.of("seq", seq)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password").value("newPass"));
    }

    @Test
    void deleteRemovesVaultEntry() throws Exception {
        String username = uniq("del");
        register(username, username + "@x.com", "pass123");
        String token = signin(username, "pass123");

        String seq = createVault(token, username, "pw", "master", false);

        mvc.perform(delete("/api/v1/Data/Delete/" + seq)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/Data/GetLoginData").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    void crossUserAccessDenied() throws Exception {
        String owner = uniq("owner");
        register(owner, owner + "@x.com", "pass123");
        String ownerToken = signin(owner, "pass123");
        String seq = createVault(ownerToken, owner, "secret", "master", false);

        String intruder = uniq("intr");
        register(intruder, intruder + "@x.com", "pass123");
        String intruderToken = signin(intruder, "pass123");

        mvc.perform(post("/api/v1/Data/GetClearPassword")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", "master",
                                "tmpLoginData", Map.of("seq", seq)))))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/Data/Update")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", "master",
                                "tmpLoginData", Map.of("seq", seq, "password", "x")))))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete("/api/v1/Data/Delete/" + seq)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutInvalidatesSession() throws Exception {
        String username = uniq("logout");
        register(username, username + "@x.com", "pass123");
        String token = signin(username, "pass123");

        mvc.perform(post("/api/v1/User/Logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/User/Me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePasswordInvalidatesSessions() throws Exception {
        String username = uniq("cpw");
        register(username, username + "@x.com", "oldPass");
        String token = signin(username, "oldPass");

        mvc.perform(post("/api/v1/User/ChangePassword")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "currentPassword", "oldPass",
                                "newPassword", "newPass"))))
                .andExpect(status().isOk());

        mvc.perform(get("/api/v1/User/Me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/User/Signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "clearPassword", "oldPass"))))
                .andExpect(status().isUnauthorized());

        signin(username, "newPass");
    }

    @Test
    void deleteUserRemovesAccountAndData() throws Exception {
        String username = uniq("dacc");
        register(username, username + "@x.com", "pass123");
        String token = signin(username, "pass123");
        createVault(token, username, "pw", "master", false);

        mvc.perform(post("/api/v1/User/Delete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("password", "pass123"))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/User/Signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "clearPassword", "pass123"))))
                .andExpect(status().isUnauthorized());
    }

    private String createNote(String token, String title, String content, String passphrase,
                            boolean encryptOnDevice) throws Exception {
        mvc.perform(post("/api/v1/Note/New")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", passphrase,
                                "tmpNote", Map.of(
                                        "title", title,
                                        "content", content,
                                        "encryptOnDevice", encryptOnDevice)))))
                .andExpect(status().isOk());

        MvcResult list = mvc.perform(post("/api/v1/Note/GetNotes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("passphrase", passphrase))))
                .andReturn();
        String listContent = list.getResponse().getContentAsString();
        List<Map<String, Object>> entries = JsonPath.read(listContent, "$");
        for (Map<String, Object> entry : entries) {
            if (title.equals(entry.get("title"))) {
                return (String) entry.get("seq");
            }
        }
        return null;
    }

    @Test
    void invalidNoteWithoutContentRejected() throws Exception {
        String username = uniq("notereq");
        register(username, username + "@x.com", "pass123");
        String token = signin(username, "pass123");

        mvc.perform(post("/api/v1/Note/New")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", "master",
                                "tmpNote", Map.of("title", "empty")))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void serverEncryptedNoteIsDecryptedOnList() throws Exception {
        String username = uniq("notemaster");
        register(username, username + "@x.com", "pass123");
        String token = signin(username, "pass123");

        String seq = createNote(token, "todo", "Buy milk and groceries", "master", false);

        mvc.perform(post("/api/v1/Note/GetNotes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("passphrase", "master"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("Buy milk and groceries"));

        mvc.perform(post("/api/v1/Note/GetNote")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", "master",
                                "tmpNote", Map.of("seq", seq)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Buy milk and groceries"));
    }

    @Test
    void serverEncryptedNoteListingRequiresPassphrase() throws Exception {
        String username = uniq("note-nopass");
        register(username, username + "@x.com", "pass123");
        String token = signin(username, "pass123");

        createNote(token, "todo", "secret text", "master", false);

        mvc.perform(post("/api/v1/Note/GetNotes")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deviceEncryptedNoteReturnsRawContent() throws Exception {
        String username = uniq("note-dev");
        register(username, username + "@x.com", "pass123");
        String token = signin(username, "pass123");

        String seq = createNote(token, "idea", "client encrypted idea", "unused", true);

        mvc.perform(post("/api/v1/Note/GetNotes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].content").value("client encrypted idea"))
                .andExpect(jsonPath("$[0].encryptOnDevice").value(true));

        mvc.perform(post("/api/v1/Note/GetNote")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", "unused",
                                "tmpNote", Map.of("seq", seq)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("client encrypted idea"));
    }

    @Test
    void updateChangesNoteContent() throws Exception {
        String username = uniq("note-upd");
        register(username, username + "@x.com", "pass123");
        String token = signin(username, "pass123");

        String seq = createNote(token, "todo", "old content", "master", false);

        mvc.perform(post("/api/v1/Note/Update")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", "master",
                                "tmpNote", Map.of(
                                        "seq", seq,
                                        "title", "updated",
                                        "content", "new content")))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/Note/GetNote")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", "master",
                                "tmpNote", Map.of("seq", seq)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("new content"))
                .andExpect(jsonPath("$.title").value("updated"));
    }

    @Test
    void deleteRemovesNote() throws Exception {
        String username = uniq("note-del");
        register(username, username + "@x.com", "pass123");
        String token = signin(username, "pass123");

        String seq = createNote(token, "todo", "delete me", "master", false);

        mvc.perform(delete("/api/v1/Note/Delete/" + seq)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/Note/GetNotes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size()").value(0));
    }

    @Test
    void noteCrossUserAccessDenied() throws Exception {
        String owner = uniq("note-owner");
        register(owner, owner + "@x.com", "pass123");
        String ownerToken = signin(owner, "pass123");
        String seq = createNote(ownerToken, "private", "owner's secret", "master", false);

        String intruder = uniq("note-intr");
        register(intruder, intruder + "@x.com", "pass123");
        String intruderToken = signin(intruder, "pass123");

        mvc.perform(post("/api/v1/Note/GetNote")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", "master",
                                "tmpNote", Map.of("seq", seq)))))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/v1/Note/Update")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "passphrase", "master",
                                "tmpNote", Map.of("seq", seq, "content", "x")))))
                .andExpect(status().isUnauthorized());

        mvc.perform(delete("/api/v1/Note/Delete/" + seq)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteUserRemovesNotes() throws Exception {
        String username = uniq("dnotes");
        register(username, username + "@x.com", "pass123");
        String token = signin(username, "pass123");
        createNote(token, "todo", "delete with user", "master", false);

        mvc.perform(post("/api/v1/User/Delete")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("password", "pass123"))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/User/Signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "clearPassword", "pass123"))))
                .andExpect(status().isUnauthorized());
    }
}
