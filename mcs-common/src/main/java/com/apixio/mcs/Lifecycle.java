package com.apixio.mcs;

/**
 * Lifecycle states.
 *
 * (Treat EVALUATED and ACCEPTED identically for now)
 *
 * Once ACCEPTED, only JSON info can be changed
 * Once RELEASED, keep JSON mutable for now
 */
public enum Lifecycle
{
    DRAFT,            // initial state forced during creation
    EVALUATED,
    DISCARDED,
    ACCEPTED,
    RELEASED,
    ARCHIVED          // doesn't show up in searches unless explicitly asked for
}
