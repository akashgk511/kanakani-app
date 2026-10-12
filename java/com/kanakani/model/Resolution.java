package com.kanakani.model;

/** Where a rumour stands. UNCLEAR means the vote never reached a clear answer, and changes no score. */
public enum Resolution {
    PENDING,
    CAME_TRUE,
    BUSTED,
    UNCLEAR
}
