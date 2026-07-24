package com.unlim.incidentassistant.context;

import java.util.List;

public record RetrievedContext(
        String systemDescription,
        List<PastIncident> similarIncidents
) {
    public RetrievedContext {
        similarIncidents = List.copyOf(similarIncidents);
    }
}
