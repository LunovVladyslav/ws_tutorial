package dev.lunov.p2p_server.controller;

import dev.lunov.p2p_server.model.Peer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private SignalController signalController;

    @GetMapping("/peers")
    public ResponseEntity<Map<String, Peer>> getActivePeers() {
        // This endpoint requires ROLE_ADMIN. Because we disabled Web Security for the SignalController 
        // /peers HTTP mapping previously, we inject SignalController here to safely read its state under the admin lock.
        return ResponseEntity.ok(signalController.getPeers());
    }
}
