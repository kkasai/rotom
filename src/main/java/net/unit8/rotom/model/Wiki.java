package net.unit8.rotom.model;

import enkan.component.ComponentLifecycle;
import enkan.component.SystemComponent;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;

public class Wiki extends SystemComponent {
    private Path repositoryPath;

    private Repository repository;

    public Page getPage(String name) {
        try {
            Ref head = repository.exactRef("refs/heads/master");

            // a commit points to a tree
            try (RevWalk walk = new RevWalk(repository)) {
                RevCommit commit = walk.parseCommit(head.getObjectId());
                RevTree tree = walk.parseTree(commit.getTree().getId());
                try (TreeWalk treeWalk = new TreeWalk(repository)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(true);
                    treeWalk.setFilter(PathFilter.create(name));
                    if (!treeWalk.next()) {
                        throw new IllegalStateException("Did not find expected file 'README.md'");
                    }
                    ObjectId objectId = treeWalk.getObjectId(0);
                    ObjectLoader loader = repository.open(objectId);
                    BlobEntry blob = new BlobEntry(name, objectId, loader.getCachedBytes());
                    return new Page(repository, blob);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void writePage(String name, String format, byte[] data, String dir, Commit commit) {
        if (dir == null) dir = "";
        Committer committer = new Committer(repository);
        try {
            committer.addToIndex(dir, name, format, data);
            committer.commit(commit);
        } catch (GitAPIException e) {

        } catch (IOException e) {

        }
    }

    public void updatePage(Page page, String name, String format, byte[] data, Commit commit) {
        if (name == null) name = page.getName();
        if (format == null) format = page.getFormat();

        boolean rename = !Objects.equals(name, page.getName());
        Committer committer = new Committer(repository);

        try {
            committer.add(page.getFileName(), data);
            committer.commit(commit);
        } catch (GitAPIException e) {

        } catch (IOException e) {

        }
    }

    @Override
    protected ComponentLifecycle lifecycle() {
        return new ComponentLifecycle<Wiki>() {
            @Override
            public void start(Wiki wiki) {
                try {
                    wiki.repository = FileRepositoryBuilder.create(repositoryPath.resolve(".git").toFile());
                    wiki.repository.create();
                } catch (IOException e) {

                }
            }

            @Override
            public void stop(Wiki wiki) {
                wiki.repository.close();
            }
        };
    }

    public void setRepositoryPath(Path repositoryPath) {
        this.repositoryPath = repositoryPath;
    }
}
