package org.assignment.troubleshooter;

import org.assignment.troubleshooter.model.KnowledgeEntry;
import org.assignment.troubleshooter.repository.KbRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/** Phase 1 check only. Delete this class once Phase 1 passes. */
@Component
public class KbCheckRunner implements CommandLineRunner {

    private final KbRepository repo;

    public KbCheckRunner(KbRepository repo) {
        this.repo = repo;
    }

    @Override
    public void run(String... args) {
        List<KnowledgeEntry> all = repo.findAll();
        System.out.println("=== KB check: " + all.size() + " entries ===");
        for (KnowledgeEntry e : all) {
            System.out.printf("%s | %s | %d keywords | %d steps | team=%s | embedding=%s%n",
                    e.id(), e.title(), e.keywords().size(), e.diagnosticSteps().size(),
                    e.escalationTeam(), e.embedding() == null ? "none" : "present");
        }
    }
}
