package com.ttt.engine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ttt.engine.model.Game;

@Repository
public interface GameRepository extends JpaRepository<Game, String> {
}
