package com.ttt.session.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ttt.session.model.GameSession;

@Repository
public interface GameSessionRepository extends JpaRepository<GameSession, String> {
}
