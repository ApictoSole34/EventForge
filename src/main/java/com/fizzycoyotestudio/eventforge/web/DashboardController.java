package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.persistence.ScenarioPersistenceService;
import com.fizzycoyotestudio.eventforge.web.dto.EventDto;
import com.fizzycoyotestudio.eventforge.web.dto.ScenarioResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;


@Controller
public class DashboardController {

    private final ScenarioPersistenceService scenarioService;
    private final EventDtoMapper mapper;

    public DashboardController(ScenarioPersistenceService scenarioService, EventDtoMapper mapper) {
        this.scenarioService = scenarioService;
        this.mapper = mapper;
    }

    @GetMapping("/scenarios")
    public String redirectScenarios() {
        return "redirect:/dashboard";
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Model model) {
        List<ScenarioResponse> scenarios = scenarioService.findAll().stream()
                .map(loaded -> {
                    List<EventDto> eventDtos = loaded.registry().getAll().stream()
                            .map(mapper::toDto)
                            .toList();
                    return new ScenarioResponse(
                            loaded.id(),
                            loaded.name(),
                            loaded.description(),
                            loaded.startEventId(),
                            loaded.initialState().asMap(),
                            eventDtos
                    );
                })
                .toList();

        model.addAttribute("scenarios", scenarios);
        return "dashboard";
    }

    @GetMapping("/scenarios/{id}")
    public String scenarioDetail(@PathVariable UUID id, Model model) {
        var loaded = scenarioService.load(id);
        List<EventDto> eventDtos = loaded.registry().getAll().stream()
                .map(mapper::toDto)
                .toList();
        var response = new ScenarioResponse(
                loaded.id(),
                loaded.name(),
                loaded.description(),
                loaded.startEventId(),
                loaded.initialState().asMap(),
                eventDtos
        );
        model.addAttribute("scenario", response);
        return "scenario-detail";
    }

    @GetMapping("/scenarios/{id}/graph")
    public String scenarioGraph(@PathVariable UUID id, Model model) {
        var loaded = scenarioService.load(id);
        List<EventDto> eventDtos = loaded.registry().getAll().stream()
                .map(mapper::toDto)
                .toList();
        var response = new ScenarioResponse(
                loaded.id(), loaded.name(), loaded.description(),
                loaded.startEventId(), loaded.initialState().asMap(), eventDtos
        );
        model.addAttribute("scenario", response);
        return "scenario-graph";
    }
}
