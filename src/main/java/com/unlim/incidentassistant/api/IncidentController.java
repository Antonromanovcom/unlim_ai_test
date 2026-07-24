package com.unlim.incidentassistant.api;

import com.unlim.incidentassistant.api.model.AnalyzeIncidentRequest;
import com.unlim.incidentassistant.api.model.IncidentAnalysis;
import com.unlim.incidentassistant.service.IncidentAnalyzer;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentAnalyzer incidentAnalyzer;

    public IncidentController(IncidentAnalyzer incidentAnalyzer) {
        this.incidentAnalyzer = incidentAnalyzer;
    }

    @PostMapping("/analyze")
    public IncidentAnalysis analyze(@Valid @RequestBody AnalyzeIncidentRequest request) {
        return incidentAnalyzer.analyze(request);
    }
}
