package com.ttt.engine.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record MoveRequestDto(@NotNull String symbol, @Min(0) @Max(8) int position) {
}
