package dev.lunov.p2p_server.model;

public record Peer (
         String id,
         String ip,
         String platform,
         String displayName,
         boolean online,
         String status,
         String avatarBase64
) {
}