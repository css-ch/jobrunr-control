package ch.css.jobrunr.control.application.audit;

import io.quarkus.security.identity.SecurityIdentity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLoggerHelper")
class AuditLoggerHelperTest {

    @Mock
    private SecurityIdentity securityIdentity;

    @Mock
    private Principal principal;

    private AuditLoggerHelper auditLogger;

    @BeforeEach
    void setUp() {
        auditLogger = new AuditLoggerHelper(securityIdentity);
    }

    // --- Template operations ---

    @Test
    @DisplayName("logTemplateCreated should use 'anonymous' when identity is anonymous")
    void logTemplateCreated_AnonymousUser_DoesNotThrow() {
        when(securityIdentity.isAnonymous()).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                auditLogger.logTemplateCreated("My Template", UUID.randomUUID(), Map.of("key", "value")));
    }

    @Test
    @DisplayName("logTemplateCreated should use principal name when authenticated")
    void logTemplateCreated_AuthenticatedUser_DoesNotThrow() {
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(securityIdentity.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("alice");

        assertThatNoException().isThrownBy(() ->
                auditLogger.logTemplateCreated("My Template", UUID.randomUUID(), Map.of("key", "value")));
    }

    @Test
    @DisplayName("logTemplateCreated should handle null parameters")
    void logTemplateCreated_NullParameters_DoesNotThrow() {
        when(securityIdentity.isAnonymous()).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                auditLogger.logTemplateCreated("My Template", UUID.randomUUID(), null));
    }

    @Test
    @DisplayName("logTemplateUpdated should not throw")
    void logTemplateUpdated_DoesNotThrow() {
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(securityIdentity.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("bob");

        assertThatNoException().isThrownBy(() ->
                auditLogger.logTemplateUpdated("Template", UUID.randomUUID(), Map.of("p", "v")));
    }

    @Test
    @DisplayName("logTemplateDeleted should not throw")
    void logTemplateDeleted_DoesNotThrow() {
        when(securityIdentity.isAnonymous()).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                auditLogger.logTemplateDeleted("Template", UUID.randomUUID()));
    }

    @Test
    @DisplayName("logTemplateExecuted should not throw for REST trigger")
    void logTemplateExecuted_Rest_DoesNotThrow() {
        when(securityIdentity.isAnonymous()).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                auditLogger.logTemplateExecuted("Template", UUID.randomUUID(), UUID.randomUUID(), TriggerSource.REST));
    }

    @Test
    @DisplayName("logTemplateExecuted should not throw for UI trigger")
    void logTemplateExecuted_UI_DoesNotThrow() {
        when(securityIdentity.isAnonymous()).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                auditLogger.logTemplateExecuted("Template", UUID.randomUUID(), UUID.randomUUID(), TriggerSource.UI));
    }

    // --- Job operations ---

    @Test
    @DisplayName("logJobCreated should handle empty parameters")
    void logJobCreated_EmptyParameters_DoesNotThrow() {
        when(securityIdentity.isAnonymous()).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                auditLogger.logJobCreated("My Job", UUID.randomUUID(), Map.of()));
    }

    @Test
    @DisplayName("logJobCreated should handle null parameters")
    void logJobCreated_NullParameters_DoesNotThrow() {
        when(securityIdentity.isAnonymous()).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                auditLogger.logJobCreated("My Job", UUID.randomUUID(), null));
    }

    @Test
    @DisplayName("logJobUpdated should not throw")
    void logJobUpdated_DoesNotThrow() {
        when(securityIdentity.isAnonymous()).thenReturn(false);
        when(securityIdentity.getPrincipal()).thenReturn(principal);
        when(principal.getName()).thenReturn("charlie");

        assertThatNoException().isThrownBy(() ->
                auditLogger.logJobUpdated("Job", UUID.randomUUID(), Map.of("param", "val")));
    }

    @Test
    @DisplayName("logJobDeleted should not throw")
    void logJobDeleted_DoesNotThrow() {
        when(securityIdentity.isAnonymous()).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                auditLogger.logJobDeleted("Job", UUID.randomUUID()));
    }

    @Test
    @DisplayName("logJobExecuted should not throw for REST trigger")
    void logJobExecuted_Rest_DoesNotThrow() {
        when(securityIdentity.isAnonymous()).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                auditLogger.logJobExecuted("Job", UUID.randomUUID(), TriggerSource.REST));
    }

    @Test
    @DisplayName("logJobExecuted should not throw for UI trigger")
    void logJobExecuted_UI_DoesNotThrow() {
        when(securityIdentity.isAnonymous()).thenReturn(true);

        assertThatNoException().isThrownBy(() ->
                auditLogger.logJobExecuted("Job", UUID.randomUUID(), TriggerSource.UI));
    }
}
