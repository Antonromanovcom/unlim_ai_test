package com.unlim.incidentassistant.service;

import com.unlim.incidentassistant.api.model.AnalyzeIncidentRequest;
import com.unlim.incidentassistant.api.model.IncidentAnalysis;

public interface IncidentAnalyzer {

    IncidentAnalysis analyze(AnalyzeIncidentRequest request);
}
