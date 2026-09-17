# Fork manifest policy

`forks.toml` is the human-readable source-of-truth for repositories OmarchyOS
owns. The checkout currently follows Google's official manifest and creates the
same local fork branch in every listed upstream project.

Before public hosting, replace upstream remotes for the modified projects with
Omarchy mirrors and generate a Repo local manifest from this inventory. Do not
publish a manifest that points at a repository name until that repository exists
and its commit is available to a clean checkout.

New Omarchy projects are independent Git repositories so they can be added to a
Repo manifest without folding unrelated Android projects into a monorepo.
