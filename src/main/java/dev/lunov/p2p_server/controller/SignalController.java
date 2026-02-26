package dev.lunov.p2p_server.controller;

import dev.lunov.p2p_server.model.CallAnswer;
import dev.lunov.p2p_server.model.CallRequest;
import dev.lunov.p2p_server.model.Peer;
import dev.lunov.p2p_server.model.PublicChannel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Controller
public class SignalController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    private final Map<String, Peer> peers = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToPeer = new ConcurrentHashMap<>();
    private final Map<String, PublicChannel> publicChannels = new ConcurrentHashMap<>();
    private final Logger logger = Logger.getLogger(SignalController.class.getName());

    @MessageMapping("/register")
    @SendTo("/topic/peers")
    public Map<String, Peer> register(Peer peer, @Header("simpSessionId") String sessionId) {
        peers.put(peer.id(), peer);
        sessionToPeer.put(sessionId, peer.id());
        logger.info("Registered peer: " + peer.id() + " at " + peer.ip() + " (session: " + sessionId + ")");
        return peers;
    }

    @MessageMapping("/call/{targetId}")
    public void call(@DestinationVariable String targetId, CallRequest request) {
        Peer target = peers.get(targetId);
        if (target == null) {
            logger.warning("Call failed: target " + targetId + " not found");
            return;
        }
        logger.info("Forwarding call from " + request.callerId() + " to " + targetId);
        messagingTemplate.convertAndSend("/topic/call/" + targetId, request);
    }

    @MessageMapping("/answer")
    public void answer(CallAnswer answer) {
        messagingTemplate.convertAndSend("/topic/answer/" + answer.callerId(), answer);
    }

    @MessageMapping("/hangup/{targetId}")
    public void hangup(@DestinationVariable String targetId, String callerId) {
        logger.info("Hangup from " + callerId + " to " + targetId);
        messagingTemplate.convertAndSend("/topic/hangup/" + targetId, callerId);
    }

    // --- WebRTC signaling ---

    @MessageMapping("/webrtc/sdp/{targetId}")
    public void forwardSdp(@DestinationVariable String targetId, String sdpJson) {
        logger.info("Forwarding SDP to " + targetId);
        messagingTemplate.convertAndSend("/topic/webrtc/sdp/" + targetId, sdpJson);
    }

    @MessageMapping("/webrtc/ice/{targetId}")
    public void forwardIce(@DestinationVariable String targetId, String iceJson) {
        logger.info("Forwarding ICE candidate to " + targetId);
        messagingTemplate.convertAndSend("/topic/webrtc/ice/" + targetId, iceJson);
    }

    // --- Lifecycle ---

    @MessageMapping("/disconnect")
    @SendTo("/topic/peers")
    public Map<String, Peer> disconnect(String peerId) {
        peers.remove(peerId);
        logger.info("Disconnected peer: " + peerId);
        return peers;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String peerId = sessionToPeer.remove(sessionId);
        if (peerId != null) {
            peers.remove(peerId);
            logger.info("Auto-removed peer: " + peerId + " (session disconnected)");
            messagingTemplate.convertAndSend("/topic/peers", peers);
        }
    }

    @Scheduled(fixedRate = 5000)
    public void broadcastPeers() {
        if (!peers.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/peers", peers);
        }
        if (!publicChannels.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/channels", publicChannels);
        }
    }


    // --- Messaging relay ---

    @MessageMapping("/message/{targetId}")
    public void relayMessage(@DestinationVariable String targetId, String encryptedMessage) {
        logger.info("Relaying message to " + targetId);
        messagingTemplate.convertAndSend("/topic/message/" + targetId, encryptedMessage);
    }

    // --- Public Channels ---

    @MessageMapping("/channel/register")
    @SendTo("/topic/channels")
    public Map<String, PublicChannel> registerChannel(PublicChannel channel) {
        publicChannels.put(channel.id(), channel);
        logger.info("Registered public channel: " + channel.name() + " (id: " + channel.id() + ")");
        return publicChannels;
    }

    @MessageMapping("/channel/message/{channelId}")
    public void relayChannelMessage(@DestinationVariable String channelId, String messageJson) {
        logger.info("Relaying channel message to channel: " + channelId);
        messagingTemplate.convertAndSend("/topic/channel/" + channelId, messageJson);
    }

    @GetMapping("/channels")
    @ResponseBody
    public Map<String, PublicChannel> getChannels() {
        return publicChannels;
    }

    public void addPublicChannel(PublicChannel channel) {
        publicChannels.put(channel.id(), channel);
        logger.info("Admin registered public channel: " + channel.name() + " (id: " + channel.id() + ")");
        messagingTemplate.convertAndSend("/topic/channels", publicChannels);
    }

    public void removePublicChannel(String id) {
        if (publicChannels.remove(id) != null) {
            logger.info("Admin removed public channel: " + id);
            messagingTemplate.convertAndSend("/topic/channels", publicChannels);
        }
    }

    public void broadcastSystemNotification(String text) {
        logger.info("Broadcasting system notification: " + text);
        try {
            java.util.Map<String, Object> sysMsg = new java.util.HashMap<>();
            sysMsg.put("senderId", "system_admin");
            sysMsg.put("senderName", "Системне сповіщення");
            sysMsg.put("text", text);
            sysMsg.put("type", "text");
            sysMsg.put("timestamp", System.currentTimeMillis());
            
            String jsonPayload = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(sysMsg);
            for (String peerId : peers.keySet()) {
                messagingTemplate.convertAndSend("/topic/message/" + peerId, jsonPayload);
            }
        } catch (Exception e) {
            logger.warning("Failed to broadcast system notification: " + e.getMessage());
        }
    }

    // --- File transfer signaling ---

    @MessageMapping("/file/request/{targetId}")
    public void fileRequest(@DestinationVariable String targetId, String metadataJson) {
        logger.info("Relaying file request to " + targetId);
        messagingTemplate.convertAndSend("/topic/file/request/" + targetId, metadataJson);
    }

    @MessageMapping("/file/accept/{targetId}")
    public void fileAccept(@DestinationVariable String targetId, String responseJson) {
        logger.info("Relaying file accept to " + targetId);
        messagingTemplate.convertAndSend("/topic/file/accept/" + targetId, responseJson);
    }

    @MessageMapping("/file/sdp/{targetId}")
    public void fileSdp(@DestinationVariable String targetId, String sdpJson) {
        messagingTemplate.convertAndSend("/topic/file/sdp/" + targetId, sdpJson);
    }

    @MessageMapping("/file/ice/{targetId}")
    public void fileIce(@DestinationVariable String targetId, String iceJson) {
        messagingTemplate.convertAndSend("/topic/file/ice/" + targetId, iceJson);
    }

    @GetMapping("/peers")
    @ResponseBody
    public Map<String, Peer> getPeers() {
        return peers;
    }
}
