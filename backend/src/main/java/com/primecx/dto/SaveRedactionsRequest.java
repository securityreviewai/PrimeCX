package com.primecx.dto;

import java.util.List;

public record SaveRedactionsRequest(
        List<RedactionRegionDto> regions
) {}
