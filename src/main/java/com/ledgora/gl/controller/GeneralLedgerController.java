package com.ledgora.gl.controller;

import com.ledgora.gl.dto.GeneralLedgerDTO;
import com.ledgora.gl.entity.GeneralLedger;
import com.ledgora.gl.service.GeneralLedgerService;
import com.ledgora.common.enums.GLAccountType;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/gl")
public class GeneralLedgerController {

    private final GeneralLedgerService glService;

    public GeneralLedgerController(GeneralLedgerService glService) {
        this.glService = glService;
    }

    /** AJAX endpoint for GL account search - used by lookup modals */
    @GetMapping("/api/search")
    @ResponseBody
    public List<Map<String, Object>> searchGlApi(@RequestParam("q") String query,
                                                  @RequestParam(value = "parentOnly", required = false) Boolean parentOnly) {
        List<GeneralLedger> allGl = glService.getAllGLAccounts();
        String q = query.toLowerCase();
        return allGl.stream()
                .filter(gl -> {
                    boolean matches = gl.getGlCode().toLowerCase().contains(q)
                            || gl.getGlName().toLowerCase().contains(q);
                    if (Boolean.TRUE.equals(parentOnly)) {
                        matches = matches && gl.getIsActive();
                    }
                    return matches;
                })
                .map(gl -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("accountCode", gl.getGlCode());
                    m.put("accountName", gl.getGlName());
                    m.put("accountType", gl.getAccountType() != null ? gl.getAccountType().name() : "");
                    m.put("category", gl.getNormalBalance());
                    return m;
                })
                .collect(Collectors.toList());
    }

    @GetMapping
    public String glHierarchy(Model model) {
        model.addAttribute("rootAccounts", glService.getRootAccounts());
        model.addAttribute("allAccounts", glService.getAllGLAccounts());
        return "gl/gl-hierarchy";
    }

    @GetMapping("/create")
    public String createGLForm(Model model) {
        model.addAttribute("glDTO", new GeneralLedgerDTO());
        model.addAttribute("accountTypes", GLAccountType.values());
        model.addAttribute("parentAccounts", glService.getAllGLAccounts());
        return "gl/gl-create";
    }

    @PostMapping("/create")
    public String createGL(@Valid @ModelAttribute("glDTO") GeneralLedgerDTO dto,
                           BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("accountTypes", GLAccountType.values());
            model.addAttribute("parentAccounts", glService.getAllGLAccounts());
            return "gl/gl-create";
        }
        try {
            GeneralLedger gl = glService.createGLAccount(dto);
            redirectAttributes.addFlashAttribute("message",
                    "GL Account created: " + gl.getGlCode() + " - " + gl.getGlName());
            return "redirect:/gl";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accountTypes", GLAccountType.values());
            model.addAttribute("parentAccounts", glService.getAllGLAccounts());
            return "gl/gl-create";
        }
    }

    @GetMapping("/{id}")
    public String viewGL(@PathVariable Long id, Model model) {
        GeneralLedger gl = glService.getGLById(id)
                .orElseThrow(() -> new RuntimeException("GL Account not found"));
        model.addAttribute("gl", gl);
        model.addAttribute("children", glService.getChildren(id));
        return "gl/gl-view";
    }

    @GetMapping("/{id}/edit")
    public String editGLForm(@PathVariable Long id, Model model) {
        GeneralLedger gl = glService.getGLById(id)
                .orElseThrow(() -> new RuntimeException("GL Account not found"));
        GeneralLedgerDTO dto = new GeneralLedgerDTO();
        dto.setId(gl.getId());
        dto.setGlCode(gl.getGlCode());
        dto.setGlName(gl.getGlName());
        dto.setDescription(gl.getDescription());
        dto.setAccountType(gl.getAccountType().name());
        dto.setLevel(gl.getLevel());
        dto.setIsActive(gl.getIsActive());
        dto.setBalance(gl.getBalance());
        dto.setNormalBalance(gl.getNormalBalance());
        if (gl.getParent() != null) {
            dto.setParentId(gl.getParent().getId());
            dto.setParentGlCode(gl.getParent().getGlCode());
        }
        model.addAttribute("glDTO", dto);
        model.addAttribute("accountTypes", GLAccountType.values());
        return "gl/gl-edit";
    }

    @PostMapping("/{id}/edit")
    public String updateGL(@PathVariable Long id,
                           @Valid @ModelAttribute("glDTO") GeneralLedgerDTO dto,
                           BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("accountTypes", GLAccountType.values());
            return "gl/gl-edit";
        }
        try {
            glService.updateGLAccount(id, dto);
            redirectAttributes.addFlashAttribute("message", "GL Account updated successfully");
            return "redirect:/gl/" + id;
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("accountTypes", GLAccountType.values());
            return "gl/gl-edit";
        }
    }

    @PostMapping("/{id}/toggle")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            glService.toggleGLStatus(id);
            redirectAttributes.addFlashAttribute("message", "GL Account status toggled");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/gl/" + id;
    }
}
