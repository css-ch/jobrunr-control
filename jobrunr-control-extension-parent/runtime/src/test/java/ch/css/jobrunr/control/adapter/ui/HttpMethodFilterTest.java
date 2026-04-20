package ch.css.jobrunr.control.adapter.ui;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class HttpMethodFilterTest {

    @Test
    @DisplayName("POST attaches a BodyHandler with file-upload handling disabled")
    void postAttachesBodyHandlerWithoutFileUploads() throws Exception {
        Route route = mock(Route.class);

        HttpMethodFilter.POST.accept(route);

        verify(route).method(HttpMethod.POST);
        assertThat(handleFileUploadsOfAttachedBodyHandler(route)).isFalse();
    }

    @Test
    @DisplayName("PUT attaches a BodyHandler with file-upload handling disabled")
    void putAttachesBodyHandlerWithoutFileUploads() throws Exception {
        Route route = mock(Route.class);

        HttpMethodFilter.PUT.accept(route);

        verify(route).method(HttpMethod.PUT);
        assertThat(handleFileUploadsOfAttachedBodyHandler(route)).isFalse();
    }

    @Test
    @DisplayName("GET does not attach a BodyHandler")
    void getDoesNotAttachBodyHandler() {
        Route route = mock(Route.class);

        HttpMethodFilter.GET.accept(route);

        verify(route).method(HttpMethod.GET);
        verify(route, never()).handler(any());
    }

    @Test
    @DisplayName("DELETE does not attach a BodyHandler")
    void deleteDoesNotAttachBodyHandler() {
        Route route = mock(Route.class);

        HttpMethodFilter.DELETE.accept(route);

        verify(route).method(HttpMethod.DELETE);
        verify(route, never()).handler(any());
    }

    /**
     * Reads the private {@code handleFileUploads} field from the {@link BodyHandler} that was
     * attached to the route. We inspect the flag directly because Vert.x' {@link BodyHandler}
     * interface only exposes a setter (no getter), and a misconfigured {@code true} is what
     * causes the {@code file-uploads/} directory creation on read-only filesystems this test
     * guards against.
     */
    @SuppressWarnings("unchecked")
    private static boolean handleFileUploadsOfAttachedBodyHandler(Route route) throws Exception {
        ArgumentCaptor<Handler<RoutingContext>> captor = ArgumentCaptor.forClass(Handler.class);
        verify(route).handler(captor.capture());
        Handler<RoutingContext> attached = captor.getValue();
        assertThat(attached).isInstanceOf(BodyHandler.class);
        Field field = attached.getClass().getDeclaredField("handleFileUploads");
        field.setAccessible(true);
        return field.getBoolean(attached);
    }
}
