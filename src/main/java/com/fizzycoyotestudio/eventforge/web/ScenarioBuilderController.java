package com.fizzycoyotestudio.eventforge.web;

import com.fizzycoyotestudio.eventforge.engine.Event;
import com.fizzycoyotestudio.eventforge.engine.GameState;
import com.fizzycoyotestudio.eventforge.persistence.ScenarioPersistenceService;
import com.fizzycoyotestudio.eventforge.web.dto.ScenarioFormData;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.UUID;

@Controller
public class ScenarioBuilderController {

    private final ScenarioPersistenceService scenarioService;
    private final ScenarioFormMapper formMapper;
    private final ScenarioFormValidator validator;

    public ScenarioBuilderController(ScenarioPersistenceService scenarioService, ScenarioFormMapper formMapper,
                                     ScenarioFormValidator validator) {
        this.scenarioService = scenarioService;
        this.formMapper = formMapper;
        this.validator = validator;
    }

    @GetMapping("/scenarios/create")
    public String createForm(Model model) {
        model.addAttribute("scenarioForm", new ScenarioFormData());
        return "scenario-form";
    }

    @PostMapping("/scenarios")
    public String create(@ModelAttribute ScenarioFormData scenarioForm, Model model) {
        List<String> errors = validator.validate(scenarioForm);
        if (!errors.isEmpty()) {
            model.addAttribute("scenarioForm", scenarioForm);
            model.addAttribute("errors", errors);
            return "scenario-form";
        }

        Event startEvent = formMapper.toStartEvent(scenarioForm);
        GameState initialState = formMapper.toInitialState(scenarioForm.getInitialState());

        UUID id = scenarioService.save(
                scenarioForm.getName(),
                scenarioForm.getDescription(),
                scenarioForm.getStartEventId(),
                initialState,
                List.of(startEvent)
        );

        return "redirect:/scenarios/" + id;
    }

    @PostMapping("/scenarios/{scenarioId}/delete")
    public String delete(@PathVariable UUID scenarioId) {
        scenarioService.deleteScenario(scenarioId);
        return "redirect:/dashboard";
    }
}