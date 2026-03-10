package com.ledgora.calendar.controller;

import com.ledgora.calendar.entity.BankCalendar;
import com.ledgora.calendar.service.BankCalendarService;
import com.ledgora.tenant.context.TenantContextHolder;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for Bank Calendar management. Supports holiday/working day management with
 * maker-checker.
 */
@Controller
@RequestMapping("/calendar")
public class BankCalendarController {

    private final BankCalendarService calendarService;

    public BankCalendarController(BankCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @GetMapping
    public String listCalendar(Model model) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        List<BankCalendar> entries = calendarService.getCalendarForTenant(tenantId);
        List<BankCalendar> pending = calendarService.getPendingCalendarEntries(tenantId);
        List<BankCalendar> holidays = calendarService.getUpcomingHolidays(tenantId);
        model.addAttribute("calendarEntries", entries);
        model.addAttribute("pendingEntries", pending);
        model.addAttribute("upcomingHolidays", holidays);
        return "calendar/calendar-list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        return "calendar/calendar-create";
    }

    @PostMapping("/create")
    public String createCalendarEntry(
            @RequestParam("calendarDate") String dateStr,
            @RequestParam("dayType") String dayType,
            @RequestParam(value = "holidayName", required = false) String holidayName,
            @RequestParam(value = "holidayType", required = false) String holidayType,
            @RequestParam(value = "atmAllowed", defaultValue = "false") boolean atmAllowed,
            @RequestParam(value = "systemTxnAllowed", defaultValue = "false")
                    boolean systemTxnAllowed,
            @RequestParam(value = "remarks", required = false) String remarks,
            RedirectAttributes redirectAttributes) {
        try {
            Long tenantId = TenantContextHolder.getRequiredTenantId();
            LocalDate date = LocalDate.parse(dateStr);
            calendarService.createCalendarEntry(
                    tenantId,
                    date,
                    dayType,
                    holidayName,
                    holidayType,
                    atmAllowed,
                    systemTxnAllowed,
                    remarks);
            redirectAttributes.addFlashAttribute(
                    "success", "Calendar entry submitted for approval.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/calendar";
    }

    @PostMapping("/approve/{id}")
    public String approveEntry(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            calendarService.approveCalendarEntry(id);
            redirectAttributes.addFlashAttribute("success", "Calendar entry approved.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/calendar";
    }

    @PostMapping("/reject/{id}")
    public String rejectEntry(
            @PathVariable Long id,
            @RequestParam(value = "reason", required = false) String reason,
            RedirectAttributes redirectAttributes) {
        try {
            calendarService.rejectCalendarEntry(id, reason);
            redirectAttributes.addFlashAttribute("success", "Calendar entry rejected.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/calendar";
    }
}
