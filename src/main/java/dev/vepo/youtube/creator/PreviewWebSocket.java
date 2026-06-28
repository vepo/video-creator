package dev.vepo.youtube.creator;

import dev.vepo.youtube.creator.service.PreviewSessionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/ws/preview/{sessionId}")
@ApplicationScoped
public class PreviewWebSocket {

    @Inject
    PreviewSessionService previewSessionService;

    @OnOpen
    public void onOpen(Session session, @PathParam("sessionId") String sessionId) {
        session.getUserProperties().put("sessionId", sessionId);
    }

    @OnMessage
    public void onMessage(String message, Session session, @PathParam("sessionId") String sessionId) {
        try {
            if ("refresh".equalsIgnoreCase(message.trim())) {
                previewSessionService.refreshSession(sessionId);
                session.getAsyncRemote().sendText("{\"event\":\"refreshed\"}");
            } else if ("ping".equalsIgnoreCase(message.trim())) {
                session.getAsyncRemote().sendText("{\"event\":\"pong\"}");
            } else if (message.startsWith("seek:")) {
                session.getAsyncRemote().sendText("{\"event\":\"seek\",\"ms\":" + message.substring(5) + "}");
            }
        } catch (Exception e) {
            session.getAsyncRemote().sendText("{\"event\":\"error\",\"message\":\"" + e.getMessage() + "\"}");
        }
    }

    @OnClose
    public void onClose(@PathParam("sessionId") String sessionId) {
        previewSessionService.stopSession(sessionId);
    }
}
