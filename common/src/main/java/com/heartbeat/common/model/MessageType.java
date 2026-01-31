package com.heartbeat.common.model;

public enum MessageType {

    // ===== SYSTEM =====
    SYSTEM,
    LOGIN,
    REGISTER,
    PAIR,
    PAIRED,
    UNPAIR,
    ERROR,

    // ===== CHAT =====
    CHAT,
    HISTORY,
    TYPING,
    MOOD,

    // ===== EMOTIONAL SIGNALS =====
    EMOJI,
    PULSE,
    ATTENTION
}
