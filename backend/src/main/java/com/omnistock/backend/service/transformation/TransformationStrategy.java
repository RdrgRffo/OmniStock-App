package com.omnistock.backend.service.transformation;

import com.fasterxml.jackson.databind.JsonNode;

public interface TransformationStrategy {
    JsonNode transform(JsonNode root, String path);
    String getStrategyName();
}

