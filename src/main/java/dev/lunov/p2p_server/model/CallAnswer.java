package dev.lunov.p2p_server.model;

public record CallAnswer(
        String callerId,
        boolean accepted
) {
}
