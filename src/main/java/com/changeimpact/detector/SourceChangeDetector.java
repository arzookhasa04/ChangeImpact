package com.changeimpact.detector;

import com.changeimpact.graph.DependencyGraph;
import com.changeimpact.model.Node;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Detects changed files using Git diffs or an explicit file/component list,
 * and maps them to graph components.
 */
public class SourceChangeDetector {

    public SourceChangeDetector() {
    }

    /**
     * Build changes from an explicit list of file paths or component IDs.
     */
    public List<FileChange> detectFromExplicitList(Collection<String> explicitList) {
        if (explicitList == null || explicitList.isEmpty()) {
            return Collections.emptyList();
        }
        List<FileChange> changes = new ArrayList<>();
        for (String item : explicitList) {
            if (item != null && !item.trim().isEmpty()) {
                changes.add(new FileChange(item.trim(), FileChangeType.MODIFIED));
            }
        }
        return changes;
    }

    /**
     * Detect changed files from Git repository.
     *
     * @param workingDir repository directory
     * @param diffTarget optional commit/branch reference (e.g., "HEAD~1", "main"). If null, checks uncommitted working tree.
     * @return list of detected file changes
     */
    public List<FileChange> detectFromGit(File workingDir, String diffTarget) {
        List<FileChange> changes = new ArrayList<>();
        try {
            List<String> command = new ArrayList<>();
            command.add("git");

            if (diffTarget != null && !diffTarget.trim().isEmpty()) {
                command.add("diff");
                command.add("--name-status");
                command.add(diffTarget.trim());
            } else {
                command.add("status");
                command.add("--porcelain");
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            if (workingDir != null) {
                pb.directory(workingDir);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    FileChange fc = parseGitLine(line);
                    if (fc != null) {
                        changes.add(fc);
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            // Log or fallback safely; do not throw unchecked crash
            System.err.println("Warning: Git change detection failed (" + e.getMessage() + "), returning empty change list.");
        }
        return changes;
    }

    /**
     * Parse git status / diff line into FileChange.
     */
    public FileChange parseGitLine(String line) {
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        String trimmed = line.trim();
        // Porcelain format: "M path", "?? path", "A  path", "D path", "R  old -> new"
        if (trimmed.startsWith("?? ") || trimmed.startsWith("A ") || trimmed.startsWith("A\t")) {
            String path = trimmed.substring(2).trim();
            return new FileChange(path, FileChangeType.ADDED);
        } else if (trimmed.startsWith("M ") || trimmed.startsWith("M\t") || trimmed.startsWith("MM ")) {
            String path = trimmed.replaceAll("^[M\\s]+", "").trim();
            return new FileChange(path, FileChangeType.MODIFIED);
        } else if (trimmed.startsWith("D ") || trimmed.startsWith("D\t")) {
            String path = trimmed.substring(2).trim();
            return new FileChange(path, FileChangeType.DELETED);
        } else if (trimmed.startsWith("R ") || trimmed.startsWith("R\t") || trimmed.matches("^R\\d+.*")) {
            // Rename: old -> new or tab separated
            if (trimmed.contains("->")) {
                String[] parts = trimmed.substring(2).split("->");
                String oldPath = parts[0].trim();
                String newPath = parts.length > 1 ? parts[1].trim() : oldPath;
                return new FileChange(newPath, FileChangeType.RENAMED, oldPath);
            } else {
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 3) {
                    return new FileChange(parts[2].trim(), FileChangeType.RENAMED, parts[1].trim());
                }
            }
        }
        // Fallback: extract last token as file path
        String[] tokens = trimmed.split("\\s+");
        if (tokens.length >= 2) {
            return new FileChange(tokens[tokens.length - 1], FileChangeType.MODIFIED);
        }
        return new FileChange(trimmed, FileChangeType.MODIFIED);
    }

    /**
     * Map detected file changes to graph component IDs.
     * Checks exact component ID, filePath match, or basename matching.
     */
    public Set<String> mapChangesToComponents(List<FileChange> changes, DependencyGraph graph) {
        Set<String> matchedComponentIds = new LinkedHashSet<>();
        if (changes == null || changes.isEmpty() || graph == null) {
            return matchedComponentIds;
        }

        // Build index of graph nodes by filePath and base name
        Map<String, String> fileToComponent = new HashMap<>();
        Map<String, String> baseNameToComponent = new HashMap<>();

        for (Node node : graph.getNodes()) {
            fileToComponent.put(node.getId().toLowerCase(), node.getId());
            if (node.getFilePath() != null) {
                String normalizedPath = node.getFilePath().replace('\\', '/').toLowerCase();
                fileToComponent.put(normalizedPath, node.getId());

                String baseName = new File(node.getFilePath()).getName().toLowerCase();
                baseNameToComponent.put(baseName, node.getId());
            }
            // Base name from node id
            baseNameToComponent.put(node.getName().toLowerCase(), node.getId());
        }

        for (FileChange change : changes) {
            String path = change.getFilePath().replace('\\', '/');
            String lowerPath = path.toLowerCase();
            String baseName = new File(path).getName().toLowerCase();

            if (graph.hasNode(path)) {
                matchedComponentIds.add(path);
            } else if (fileToComponent.containsKey(lowerPath)) {
                matchedComponentIds.add(fileToComponent.get(lowerPath));
            } else if (baseNameToComponent.containsKey(baseName)) {
                matchedComponentIds.add(baseNameToComponent.get(baseName));
            } else {
                // If not in graph, check if component ID matches without .java extension
                String withoutExt = baseName.endsWith(".java") ? baseName.substring(0, baseName.length() - 5) : baseName;
                if (baseNameToComponent.containsKey(withoutExt)) {
                    matchedComponentIds.add(baseNameToComponent.get(withoutExt));
                } else if (graph.hasNode(change.getFilePath())) {
                    matchedComponentIds.add(change.getFilePath());
                }
            }
        }

        return matchedComponentIds;
    }
}
