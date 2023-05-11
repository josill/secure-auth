package com.josill.jwtlogin.token;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final TokenRepository tokenRepository;

    public List<Token> findAllValidTokensByUser(Long userId) {
        return tokenRepository.findAllValidTokensByUser(userId);
    }

    public Optional<Token> findByToken(String token) {
        return tokenRepository.findByToken(token);
    }

    public void save(Token token) {
        tokenRepository.save(token);
    }

    public void saveAll(List<Token> tokens) {
        tokenRepository.saveAll(tokens);
    }

}
