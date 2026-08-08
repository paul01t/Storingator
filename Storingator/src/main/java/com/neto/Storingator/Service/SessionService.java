package com.neto.Storingator.Service;

import com.neto.Storingator.Entities.Activeusers;
import com.neto.Storingator.Exceptions.UnauthorizedException;
import com.neto.Storingator.Repository.ActiveUsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class SessionService {

    private static final Duration SESSION_TTL = Duration.ofMinutes(30);

    @Autowired
    private ActiveUsersRepository acUsersRepository;

    @Transactional
    public Activeusers authenticateFromBearer(String bearerToken) {
        String loginkey = parseToken(bearerToken);
        Activeusers active = acUsersRepository.findByLoginkey(loginkey);
        if (active == null) {
            throw new UnauthorizedException("Invalid or expired session");
        }
        if (active.getStartdate() == null
                || active.getStartdate().isBefore(LocalDateTime.now().minus(SESSION_TTL))) {
            acUsersRepository.delete(active);
            throw new UnauthorizedException("Session expired");
        }
        return active;
    }

    @Transactional
    public void logout(String bearerToken) {
        Activeusers active = authenticateFromBearer(bearerToken);
        acUsersRepository.deleteByUserseq(active.getUserseq());
    }

    private String parseToken(String bearerToken) {
        if (bearerToken == null || bearerToken.isBlank()) {
            throw new UnauthorizedException("Missing credentials");
        }
        String token = bearerToken;
        if (bearerToken.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = bearerToken.substring(7);
        }
        token = token.trim();
        if (token.isEmpty()) {
            throw new UnauthorizedException("Missing credentials");
        }
        return token;
    }
}