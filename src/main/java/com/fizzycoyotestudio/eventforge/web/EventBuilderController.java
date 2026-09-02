package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.engine.Event;
import com.fizzycoyotestudio.eventforge.engine.Operator;
import com.fizzycoyotestudio.eventforge.persistence.ScenarioPersistenceService;
import com.fizzycoyotestudio.eventforge.web.dto.EventFormData;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/scenarios/{scenarioId}/events")
public class EventBuilderController {

    private final ScenarioPersistenceService scenarioService;
    private final EventFormMapper formMapper;
    private final EventFormValidator validator;

    public EventBuilderController(ScenarioPersistenceService scenarioService, EventFormMapper formMapper,
                                  EventFormValidator validator) {
        this.scenarioService = scenarioService;
        this.formMapper = formMapper;
        this.validator = validator;
    }

    @GetMapping("/create")
    public String createForm(@PathVariable UUID scenarioId, Model model) {
        var scenario = scenarioService.load(scenarioId);
        populateFormModel(scenario, new EventFormData(), false, model);
        return "event-form";
    }

    @PostMapping
    public String create(@PathVariable UUID scenarioId, @ModelAttribute EventFormData eventForm, Model model) {
        var scenario = scenarioService.load(scenarioId);
        List<String> existingIds = existingEventIds(scenario);

        List<String> errors = validator.validate(eventForm, false, existingIds);
        if (!errors.isEmpty()) {
            populateFormModel(scenario, eventForm, false, model);
            model.addAttribute("errors", errors);
            return "event-form";
        }

        scenarioService.saveEvent(scenarioId, formMapper.toDomain(eventForm));
        return "redirect:/scenarios/" + scenarioId;
    }

    @GetMapping("/{eventId}/edit")
    public String editForm(@PathVariable UUID scenarioId, @PathVariable String eventId, Model model) {
        var scenario = scenarioService.load(scenarioId);
        Event event = scenario.registry().getOrThrow(eventId);
        populateFormModel(scenario, formMapper.toFormData(event), true, model);
        return "event-form";
    }

    @PostMapping("/{eventId}")
    public String update(@PathVariable UUID scenarioId, @PathVariable String eventId,
                         @ModelAttribute EventFormData eventForm, Model model) {
        var scenario = scenarioService.load(scenarioId);
        List<String> existingIds = existingEventIds(scenario);

        List<String> errors = validator.validate(eventForm, true, existingIds);
        if (!errors.isEmpty()) {
            populateFormModel(scenario, eventForm, true, model);
            model.addAttribute("errors", errors);
            return "event-form";
        }

        scenarioService.saveEvent(scenarioId, formMapper.toDomain(eventForm));
        return "redirect:/scenarios/" + scenarioId;
    }

    @PostMapping("/{eventId}/delete")
    public String delete(@PathVariable UUID scenarioId, @PathVariable String eventId) {
        scenarioService.deleteEvent(scenarioId, eventId);
        return "redirect:/scenarios/" + scenarioId;
    }

    private List<String> existingEventIds(ScenarioPersistenceService.LoadedScenario scenario) {
        return scenario.registry().getAll().stream().map(Event::getId).toList();
    }

    private void populateFormModel(ScenarioPersistenceService.LoadedScenario scenario, EventFormData form,
                                   boolean isEdit, Model model) {
        model.addAttribute("scenarioId", scenario.id());
        model.addAttribute("scenarioName", scenario.name());
        model.addAttribute("eventForm", form);
        model.addAttribute("operators", Operator.values());
        model.addAttribute("existingEventIds", existingEventIds(scenario));
        model.addAttribute("isEdit", isEdit);
    }
}
