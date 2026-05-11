package com.primecx.model;

/**
 * Commercial tier for end customers; influences smart-routing weights (higher tiers favor
 * agents with stronger historical resolution metrics).
 */
public enum CustomerTier {
    STANDARD,
    PREMIUM,
    ENTERPRISE
}
